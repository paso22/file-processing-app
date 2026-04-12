package ge.paso22.fileprocessingapp.service.aws;

import java.io.InputStream;

public interface S3Service {

  void uploadFile(String key, byte[] content, String contentType);

  InputStream downloadFile(String key);
}
