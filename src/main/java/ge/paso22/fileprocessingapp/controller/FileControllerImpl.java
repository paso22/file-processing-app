package ge.paso22.fileprocessingapp.controller;

import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import ge.paso22.fileprocessingapp.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileControllerImpl implements FileController {

  private final FileService fileService;

  @Override
  public ResponseEntity<FileProcessingResponseDto> uploadFile(MultipartFile file) {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    try {
      FileProcessingResponseDto response = fileService.submitFile(file);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    } catch (Exception e) {
      log.error("Unexpected error during file upload: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Override
  public ResponseEntity<FileProcessingResponseDto> getFileStatus(String processingId) {
    return fileService
        .getFileStatus(processingId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
