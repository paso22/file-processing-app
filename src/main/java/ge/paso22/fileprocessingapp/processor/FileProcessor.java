package ge.paso22.fileprocessingapp.processor;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import ge.paso22.fileprocessingapp.model.FileProcessingStatus;
import ge.paso22.fileprocessingapp.service.aws.S3Service;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessor {

  private final ConcurrentHashMap<String, FileProcessingRecord> processingStore;

  private final S3Service s3Service;

  @Async
  public void process(String jobId, String fileName, String s3Key) {
    try {
      processingStore.put(
          jobId,
          processingStore.get(jobId).toBuilder()
              .status(FileProcessingStatus.PROCESSING)
              .message("File is being processed")
              .build());

      String content = "";
      try (InputStream inputStream = s3Service.downloadFile(s3Key)) {
        content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception ex) {
        log.error("Unexpected error occurred while reading inputStream", ex);
      }

      // Split on \r\n, \r, or \n to handle Windows, old Mac, and Unix line endings uniformly
      String[] lines = content.split("\\r\\n|\\r|\\n", -1);
      int lineCount = lines.length;
      int characterCount = content.length();

      processingStore.put(
          jobId,
          processingStore.get(jobId).toBuilder()
              .status(FileProcessingStatus.COMPLETED)
              .lineCount(lineCount)
              .characterCount(characterCount)
              .message("Processing completed successfully")
              .completedAt(Instant.now())
              .build());

      log.info(
          "Processing {} completed: fileName='{}', lines={}, chars={}",
          jobId,
          fileName,
          lineCount,
          characterCount);
    } catch (Exception e) {
      log.error("Processing {} failed for file '{}': {}", jobId, fileName, e.getMessage(), e);
      processingStore.put(
          jobId,
          processingStore.get(jobId).toBuilder()
              .status(FileProcessingStatus.FAILED)
              .message("Processing failed: " + e.getMessage())
              .completedAt(Instant.now())
              .build());
    }
  }
}
