package ge.paso22.fileprocessingapp.service.aws;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceBean implements S3Service {

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  private final S3Client s3Client;

  @Override
  public void uploadFile(String key, byte[] content, String contentType) {
    log.info("uploading file with a given key - {} and contentType - {}", key, contentType);
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build();

    s3Client.putObject(request, RequestBody.fromBytes(content));
    log.info("Successfully uploaded to S3: key={}", key);
  }

  @Override
  public InputStream downloadFile(String key) {
    log.info("downloading file from s3 with a key - {}", key);
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    return s3Client.getObject(getObjectRequest);
  }
}
