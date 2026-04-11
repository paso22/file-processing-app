package ge.paso22.fileprocessingapp.config;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync is required for @Async in FileProcessor to take effect.
// Without it, Spring ignores the annotation and processing blocks the HTTP thread.
@EnableAsync
@Configuration
public class Async {

  // Declared here so Spring manages it as a singleton, ensuring FileService and FileProcessor
  // share the exact same map instance. Declaring it with 'new' inside each class would create
  // two independent maps — each component would only see its own writes.
  @Bean
  public ConcurrentHashMap<String, FileProcessingRecord> processingStoreAsBean() {
    return new ConcurrentHashMap<>();
  }
}
