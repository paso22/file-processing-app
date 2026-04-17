package ge.paso22.fileprocessingapp.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import ge.paso22.fileprocessingapp.model.FileProcessingStatus;
import ge.paso22.fileprocessingapp.service.aws.S3Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileProcessorServiceBeanTest {

  @Mock
  private S3Service s3Service;

  private final ConcurrentHashMap<String, FileProcessingRecord> processingStore =
      new ConcurrentHashMap<>();

  private FileProcessorServiceBean fileProcessorServiceBean;

  @BeforeEach
  void setUp() {
    processingStore.clear();
    fileProcessorServiceBean = new FileProcessorServiceBean(processingStore, s3Service);
  }

  @Test
  @DisplayName("Should transition to COMPLETED and return correct line and character counts")
  void givenValidS3Content_whenProcess_thenTransitionsToCompletedWithCorrectCounts() {
    String processingId = seedStore("test.txt");
    String content = "line one\nline two\nline three";
    stubS3Download(content);

    fileProcessorServiceBean.process(processingId, "test.txt", "key/test.txt");

    FileProcessingRecord result = processingStore.get(processingId);
    assertThat(result.status()).isEqualTo(FileProcessingStatus.COMPLETED);
    assertThat(result.lineCount()).isEqualTo(3);
    assertThat(result.characterCount()).isEqualTo(content.length());
    assertThat(result.completedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should count lines correctly when content uses Windows (CRLF) line endings")
  void givenContentWithWindowsLineEndings_whenProcess_thenCountsLinesCorrectly() {
    String processingId = seedStore("test.txt");
    stubS3Download("line1\r\nline2\r\nline3");

    fileProcessorServiceBean.process(processingId, "test.txt", "key/test.txt");

    assertThat(processingStore.get(processingId).lineCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should count lines correctly when content uses mixed line endings (CRLF, LF, CR)")
  void givenContentWithMixedLineEndings_whenProcess_thenCountsLinesCorrectly() {
    String processingId = seedStore("test.txt");
    // Windows (\r\n), Unix (\n), old Mac (\r)
    stubS3Download("line1\r\nline2\nline3\rline4");

    fileProcessorServiceBean.process(processingId, "test.txt", "key/test.txt");

    assertThat(processingStore.get(processingId).lineCount()).isEqualTo(4);
  }

  @Test
  @DisplayName("Should complete with empty content when S3 download fails silently")
  void givenS3DownloadFailure_whenProcess_thenCompletesWithEmptyContent() {
    // S3 download failure is caught internally and swallowed — content defaults
    // to empty string, resulting in COMPLETED with 1 line and 0 characters.
    // This test documents the current behavior.
    String processingId = seedStore("test.txt");
    when(s3Service.downloadFile(any())).thenThrow(new RuntimeException("S3 unavailable"));

    fileProcessorServiceBean.process(processingId, "test.txt", "key/test.txt");

    FileProcessingRecord result = processingStore.get(processingId);
    assertThat(result.status()).isEqualTo(FileProcessingStatus.COMPLETED);
    assertThat(result.characterCount()).isEqualTo(0);
  }

  private String seedStore(String fileName) {
    String processingId = "test-id-" + Instant.now().toEpochMilli();
    processingStore.put(processingId, FileProcessingRecord.builder()
        .processingId(processingId)
        .status(FileProcessingStatus.PENDING)
        .fileName(fileName)
        .fileSizeBytes(100L)
        .message("File received and queued for processing")
        .build());
    return processingId;
  }

  private void stubS3Download(String content) {
    InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    when(s3Service.downloadFile(any())).thenReturn(stream);
  }
}
