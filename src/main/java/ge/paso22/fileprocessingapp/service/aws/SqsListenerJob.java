package ge.paso22.fileprocessingapp.service.aws;

import ge.paso22.fileprocessingapp.processor.FileProcessorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsListenerJob {

  @Value("${aws.sqs.queue-url}")
  private String queueUrl;

  private final SqsClient sqsClient;

  private final FileProcessorService fileProcessorService;

  private final ObjectMapper objectMapper;

  @Scheduled(cron = "0 */1 * * * *")
  public void pollSqsQueue() {
    log.info("Started polling sqs queue");
    poll();
    log.info("Finished polling sqs queue");
  }

  private void poll() {
    ReceiveMessageRequest request =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(5)
            .waitTimeSeconds(0)
            .build();

    List<Message> messages = sqsClient.receiveMessage(request).messages();
    if (messages.isEmpty()) {
      log.info("SQS is empty!");
      return;
    }

    for (Message message : messages) {
      processMessage(message);
    }
  }

  private void processMessage(Message message) {
    try {
      String messageBody = message.body();
      JsonNode root = objectMapper.readTree(messageBody);
      JsonNode records = root.path("Records");

      if (records.isMissingNode() || !records.isArray()) {
        log.warn("Unexpected message format, skipping: {}", message.body());
        deleteMessage(message);
        return;
      }

      for (JsonNode record : records) {
        String bucketName = record.path("s3").path("bucket").path("name").asString();
        // S3 URL-encodes the key (spaces become +, special chars become %XX)
        String s3Key =
            java.net.URLDecoder.decode(
                record.path("s3").path("object").path("key").asString(),
                java.nio.charset.StandardCharsets.UTF_8);

        String fileName = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf("/") + 1) : s3Key;

        // Extract processingId from the key structure: "{processingId}/{fileName}"
        String processingId = s3Key.contains("/") ? s3Key.substring(0, s3Key.indexOf("/")) : s3Key;

        log.info(
            "Processing S3 event: bucketName={}, key={}, processingId={}",
            bucketName,
            s3Key,
            processingId);
        fileProcessorService.process(processingId, fileName, s3Key);
      }

      deleteMessage(message);
    } catch (Exception ex) {
      log.error("Failed to parse SQS message with a messageId - {}", message.messageId(), ex);
    }
  }

  private void deleteMessage(Message message) {
    log.info(
        "Deleting SQS message with messageId - {} and receiptId - {}",
        message.messageId(),
        message.receiptHandle());
    DeleteMessageRequest request =
        DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
    sqsClient.deleteMessage(request);
    log.info("Deleted SQS message: {}", message.messageId());
  }
}
