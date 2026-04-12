package ge.paso22.fileprocessingapp.processor;

public interface FileProcessorService {

  void process(String processingId, String fileName, String s3Key);
}
