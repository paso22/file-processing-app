package ge.paso22.fileprocessingapp.controller;

import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1/files")
public interface FileController {

  @PostMapping("/upload")
  ResponseEntity<FileProcessingResponseDto> uploadFile(@RequestParam("file") MultipartFile file);

  @GetMapping("/{processingId}/status")
  ResponseEntity<FileProcessingResponseDto> getFileStatus(@PathVariable String processingId);
}
