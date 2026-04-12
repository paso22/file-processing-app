package ge.paso22.fileprocessingapp.config;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
public class SchedulingConfig {

  @Bean
  public ConcurrentHashMap<String, FileProcessingRecord> processingStoreAsBean() {
    return new ConcurrentHashMap<>();
  }
}
