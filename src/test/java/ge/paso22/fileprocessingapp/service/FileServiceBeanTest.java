package ge.paso22.fileprocessingapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import ge.paso22.fileprocessingapp.model.FileProcessingStatus;
import ge.paso22.fileprocessingapp.service.aws.S3Service;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceBeanTest {

  @Mock private S3Service s3Service;

  private final ConcurrentHashMap<String, FileProcessingRecord> processingStore =
      new ConcurrentHashMap<>();

  private FileServiceBean fileServiceBean;

  @BeforeEach
  void setUp() {
    processingStore.clear();
    fileServiceBean = new FileServiceBean(processingStore, s3Service);
  }

  @Test
  @DisplayName("Should return PENDING DTO immediately after file submission")
  void givenValidFile_whenSubmitFile_thenReturnsPendingDto() throws Exception {
    MultipartFile file = mockFile("report.txt", "text/plain");

    FileProcessingResponseDto result = fileServiceBean.submitFile(file);

    verify(s3Service).uploadFile(any(), any(), any());
    assertThat(result.status()).isEqualTo(FileProcessingStatus.PENDING);
    assertThat(result.fileName()).isEqualTo("report.txt");
    assertThat(result.processingId()).isNotNull();
    assertThat(result.lineCount()).isNull();
    assertThat(result.characterCount()).isNull();
  }

  @Test
  @DisplayName("Should store the processing record in the map before returning")
  void givenValidFile_whenSubmitFile_thenStoresRecordInProcessingStore() throws Exception {
    MultipartFile file = mockFile("report.txt", "text/plain");

    FileProcessingResponseDto result = fileServiceBean.submitFile(file);

    assertThat(processingStore).containsKey(result.processingId());
    assertThat(processingStore.get(result.processingId()).status())
        .isEqualTo(FileProcessingStatus.PENDING);
  }

  @Test
  @DisplayName("Should upload file to S3 using {processingId}/{fileName} as the key")
  void givenValidFile_whenSubmitFile_thenUploadsToS3WithCorrectKey() throws Exception {
    MultipartFile file = mockFile("report.csv", "text/csv");

    FileProcessingResponseDto result = fileServiceBean.submitFile(file);

    verify(s3Service)
        .uploadFile(eq(result.processingId() + "/report.csv"), any(byte[].class), eq("text/csv"));
  }

  @Test
  @DisplayName("Should return DTO when the processing ID exists in the store")
  void givenExistingProcessingId_whenGetFileStatus_thenReturnsDto() {
    FileProcessingRecord record =
        FileProcessingRecord.builder()
            .processingId("abc-123")
            .status(FileProcessingStatus.COMPLETED)
            .fileName("report.txt")
            .fileSizeBytes(100L)
            .lineCount(10)
            .characterCount(200)
            .message("Processing completed successfully")
            .build();
    processingStore.put("abc-123", record);

    Optional<FileProcessingResponseDto> result = fileServiceBean.getFileStatus("abc-123");

    assertThat(result).isPresent();
    assertThat(result.get().status()).isEqualTo(FileProcessingStatus.COMPLETED);
    assertThat(result.get().processingId()).isEqualTo("abc-123");
  }

  @Test
  @DisplayName("Should return empty Optional when the processing ID is not found")
  void givenUnknownProcessingId_whenGetFileStatus_thenReturnsEmpty() {
    Optional<FileProcessingResponseDto> result = fileServiceBean.getFileStatus("non-existent");

    assertThat(result).isEmpty();
  }

  private MultipartFile mockFile(String fileName, String contentType) throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(fileName);
    when(file.getSize()).thenReturn(100L);
    when(file.getBytes()).thenReturn("content".getBytes());
    when(file.getContentType()).thenReturn(contentType);
    return file;
  }
}
