package ge.paso22.fileprocessingapp.model;

import java.time.Instant;
import lombok.Builder;

// Internal state object. Lives in the ConcurrentHashMap and is updated by FileProcessor
// as the processing pipeline advances. Never leaves the service layer — callers receive
// FileProcessingResponseDto instead.
@Builder(toBuilder = true)
public record FileProcessingRecord(
    String processingId,
    FileProcessingStatus status,
    String fileName,
    // Retained internally for logging and future S3 pre-signed URL size checks
    Long fileSizeBytes,
    // Null until processing completes; populated by FileProcessor
    Integer lineCount,
    Integer characterCount,
    String message,
    Instant createdAt,
    // Null until the record transitions to COMPLETED or FAILED
    Instant completedAt
) {}
