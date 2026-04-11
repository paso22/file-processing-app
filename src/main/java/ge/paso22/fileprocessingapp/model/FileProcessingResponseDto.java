package ge.paso22.fileprocessingapp.model;

import java.time.Instant;

// External API contract. Only exposes fields that are meaningful to callers.
// Internal fields (fileSizeBytes, createdAt, processingId) are intentionally omitted —
// they are implementation details that must not leak into the public API surface.
public record FileProcessingResponseDto(
    String processingId,
    FileProcessingStatus status,
    String fileName,
    Integer lineCount,
    Integer characterCount,
    String message,
    Instant completedAt
) {

  public static FileProcessingResponseDto from(FileProcessingRecord record) {
    return new FileProcessingResponseDto(
        record.processingId(),
        record.status(),
        record.fileName(),
        record.lineCount(),
        record.characterCount(),
        record.message(),
        record.completedAt()
    );
  }
}
