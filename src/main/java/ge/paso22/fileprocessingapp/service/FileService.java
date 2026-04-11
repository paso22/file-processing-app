package ge.paso22.fileprocessingapp.service;

import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

  FileProcessingResponseDto submitFile(MultipartFile file) throws Exception;

  Optional<FileProcessingResponseDto> getFileStatus(String jobId);
}
