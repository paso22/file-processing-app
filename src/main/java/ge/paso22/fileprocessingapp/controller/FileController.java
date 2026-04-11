package ge.paso22.fileprocessingapp.controller;

import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1/files")
public interface FileController {

  @PostMapping("/upload")
  ResponseEntity<FileProcessingResponseDto> uploadFile(@RequestParam("file") MultipartFile file);

  @GetMapping("/{processingId}/status")
  ResponseEntity<FileProcessingResponseDto> getFileStatus(@PathVariable String processingId);
}
