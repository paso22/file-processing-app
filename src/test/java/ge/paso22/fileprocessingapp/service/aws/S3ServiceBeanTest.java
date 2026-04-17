package ge.paso22.fileprocessingapp.service.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceBeanTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private ResponseInputStream<GetObjectResponse> mockResponseStream;

  private S3ServiceBean s3ServiceBean;

  @BeforeEach
  void setUp() {
    s3ServiceBean = new S3ServiceBean(s3Client);
    ReflectionTestUtils.setField(s3ServiceBean, "bucketName", "test-bucket");
  }

  @Test
  @DisplayName("Should build PutObjectRequest with correct bucket, key, and content type")
  void givenFileContent_whenUploadFile_thenCallsPutObjectWithCorrectParameters() {
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

    s3ServiceBean.uploadFile("uploads/report.csv", "data".getBytes(), "text/csv");

    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    PutObjectRequest request = captor.getValue();
    assertThat(request.bucket()).isEqualTo("test-bucket");
    assertThat(request.key()).isEqualTo("uploads/report.csv");
    assertThat(request.contentType()).isEqualTo("text/csv");
  }

  @Test
  @DisplayName("Should build GetObjectRequest with correct bucket and key")
  void givenS3Key_whenDownloadFile_thenCallsGetObjectWithCorrectParameters() {
    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockResponseStream);
    ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);

    s3ServiceBean.downloadFile("uploads/report.csv");

    verify(s3Client).getObject(captor.capture());
    GetObjectRequest request = captor.getValue();
    assertThat(request.bucket()).isEqualTo("test-bucket");
    assertThat(request.key()).isEqualTo("uploads/report.csv");
  }
}
