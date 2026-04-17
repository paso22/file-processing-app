package ge.paso22.fileprocessingapp.service.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ge.paso22.fileprocessingapp.processor.FileProcessorService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SqsListenerJobTest {

  @Mock
  private SqsClient sqsClient;

  @Mock
  private FileProcessorService fileProcessorService;

  private SqsListenerJob sqsListenerJob;

  @BeforeEach
  void setUp() {
    sqsListenerJob = new SqsListenerJob(sqsClient, fileProcessorService, new ObjectMapper());
    ReflectionTestUtils.setField(sqsListenerJob, "queueUrl", "https://sqs.eu-central-1.amazonaws.com/123/test-queue");
  }

  @Test
  @DisplayName("Should do nothing when the SQS queue has no messages")
  void givenEmptyQueue_whenPollSqsQueue_thenNoProcessingOrDeletion() {
    stubSqsResponse(List.of());

    sqsListenerJob.pollSqsQueue();

    verify(fileProcessorService, never()).process(any(), any(), any());
    verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
  }

  @Test
  @DisplayName("Should process the S3 event and delete the message from the queue")
  void givenValidS3Event_whenPollSqsQueue_thenProcessesAndDeletesMessage() {
    String body = s3EventBody("my-bucket", "abc-123/report.csv");
    Message message = buildMessage("msg-1", "receipt-1", body);
    stubSqsResponse(List.of(message));

    sqsListenerJob.pollSqsQueue();

    verify(fileProcessorService).process("abc-123", "report.csv", "abc-123/report.csv");
    verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
  }

  @Test
  @DisplayName("Should URL-decode the S3 key before passing it to the file processor")
  void givenUrlEncodedS3Key_whenPollSqsQueue_thenDecodesKeyBeforeProcessing() {
    // S3 encodes spaces as '+' and special chars as %XX in event notifications
    String body = s3EventBody("my-bucket", "abc-123/my+report+file.csv");
    Message message = buildMessage("msg-2", "receipt-2", body);
    stubSqsResponse(List.of(message));

    sqsListenerJob.pollSqsQueue();

    verify(fileProcessorService).process("abc-123", "my report file.csv", "abc-123/my report file.csv");
  }

  @Test
  @DisplayName("Should skip processing and delete the message when its format is unexpected")
  void givenMalformedMessage_whenPollSqsQueue_thenSkipsProcessingAndDeletesMessage() {
    Message message = buildMessage("msg-3", "receipt-3", "{\"unexpected\": \"format\"}");
    stubSqsResponse(List.of(message));

    sqsListenerJob.pollSqsQueue();

    verify(fileProcessorService, never()).process(any(), any(), any());
    verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
  }

  private void stubSqsResponse(List<Message> messages) {
    ReceiveMessageResponse response = ReceiveMessageResponse.builder()
        .messages(messages)
        .build();
    when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
  }

  private Message buildMessage(String messageId, String receiptHandle, String body) {
    return Message.builder()
        .messageId(messageId)
        .receiptHandle(receiptHandle)
        .body(body)
        .build();
  }

  private String s3EventBody(String bucket, String key) {
    return """
        {
          "Records": [
            {
              "s3": {
                "bucket": { "name": "%s" },
                "object": { "key": "%s" }
              }
            }
          ]
        }
        """.formatted(bucket, key);
  }
}
