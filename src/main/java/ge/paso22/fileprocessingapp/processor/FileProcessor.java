package ge.paso22.fileprocessingapp.processor;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import ge.paso22.fileprocessingapp.model.FileProcessingStatus;
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

  // ConcurrentHashMap is required here because the HTTP thread writes the initial PENDING entry
  // while this async thread writes PROCESSING and COMPLETED — both accessing the map concurrently.
  private final ConcurrentHashMap<String, FileProcessingRecord> processingStore;

  // Local simulation of what will become an @SqsListener in Phase 4.
  // The method signature intentionally mirrors what would be extracted from an SQS message payload:
  // job ID, file name, and raw file content as bytes — so migrating to SQS requires only changing
  // how this method is invoked, not its internal logic.
  @Async
  public void process(String jobId, String fileName, byte[] fileContent) {
    try {
      processingStore.put(jobId, processingStore.get(jobId).toBuilder()
          .status(FileProcessingStatus.PROCESSING)
          .message("File is being processed")
          .build());

      // Simulate the latency of a real processing step (e.g., S3 upload + SQS round-trip)
      Thread.sleep(10_000);

      String content = new String(fileContent, StandardCharsets.UTF_8);
      // Split on \r\n, \r, or \n to handle Windows, old Mac, and Unix line endings uniformly
      String[] lines = content.split("\\r\\n|\\r|\\n", -1);
      int lineCount = lines.length;
      int characterCount = content.length();

      processingStore.put(jobId, processingStore.get(jobId).toBuilder()
          .status(FileProcessingStatus.COMPLETED)
          .lineCount(lineCount)
          .characterCount(characterCount)
          .message("Processing completed successfully")
          .completedAt(Instant.now())
          .build());

      log.info("Processing {} completed: fileName='{}', lines={}, chars={}",
          jobId, fileName, lineCount, characterCount);
    } catch (Exception e) {
      log.error("Processing {} failed for file '{}': {}", jobId, fileName, e.getMessage(), e);
      processingStore.put(jobId, processingStore.get(jobId).toBuilder()
          .status(FileProcessingStatus.FAILED)
          .message("Processing failed: " + e.getMessage())
          .completedAt(Instant.now())
          .build());
    }
  }
}
