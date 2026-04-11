package ge.paso22.fileprocessingapp.service;

import ge.paso22.fileprocessingapp.model.FileProcessingRecord;
import ge.paso22.fileprocessingapp.model.FileProcessingResponseDto;
import ge.paso22.fileprocessingapp.model.FileProcessingStatus;
import ge.paso22.fileprocessingapp.processor.FileProcessor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceBean implements FileService {

  private final FileProcessor fileProcessor;

  private final ConcurrentHashMap<String, FileProcessingRecord> processingStore;

  @Override
  public FileProcessingResponseDto submitFile(MultipartFile file) throws Exception {
    String processingId = UUID.randomUUID().toString();
    String fileName = file.getOriginalFilename();
    long fileSizeBytes = file.getSize();

    log.info(
        "Received file '{}' ({} bytes), assigned processingId={}",
        fileName,
        fileSizeBytes,
        processingId);

    FileProcessingRecord record =
        FileProcessingRecord.builder()
            .processingId(processingId)
            .status(FileProcessingStatus.PENDING)
            .fileName(fileName)
            .fileSizeBytes(fileSizeBytes)
            .message("File received and queued for processing")
            .createdAt(Instant.now())
            .build();

    processingStore.put(processingId, record);
    fileProcessor.process(processingId, fileName, file.getBytes());
    return FileProcessingResponseDto.from(record);
  }

  @Override
  public Optional<FileProcessingResponseDto> getFileStatus(String processingId) {
    return Optional.ofNullable(processingStore.get(processingId))
        .map(FileProcessingResponseDto::from);
  }
}
