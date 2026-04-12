# file-processing-app

A Spring Boot service that accepts file uploads, stores them in S3, and processes them asynchronously. Clients upload a file and receive a `processingId` they can use to poll for results. Processing extracts line and character counts from the file content.

---

## How it works

1. **Upload** — A client posts a file to `POST /api/v1/files/upload`. The service assigns a UUID `processingId`, records the job as `PENDING` in an in-memory store, and uploads the file to S3 under the key `{processingId}/{fileName}`.

2. **S3 → SQS notification** — The S3 bucket is configured to fire an `s3:ObjectCreated:*` event notification to an SQS queue for every upload.

3. **SQS polling** — A scheduled job (`SqsListenerJob`) polls the SQS queue every minute. For each message it extracts the bucket name, S3 key, and `processingId`, then delegates to `FileProcessorService`.

4. **Processing** — `FileProcessorServiceBean` marks the job `PROCESSING`, downloads the file from S3, counts lines and characters, then marks the job `COMPLETED` (or `FAILED` on error). Results are written back to the shared in-memory store.

5. **Status polling** — The client polls `GET /api/v1/files/{processingId}/status` until the status transitions out of `PENDING` / `PROCESSING`.

---

## Technologies

| Technology | Role |
|---|---|
| **Spring Boot 4 (Web MVC)** | HTTP layer and application container |
| **AWS SDK v2 — S3** | Durable file storage; also the event source that triggers the async pipeline |
| **AWS SDK v2 — SQS** | Decouples upload from processing; the service polls the queue on a cron schedule |
| **AWS CloudFormation** (`infrastructure/stack.yaml`) | Provisions the S3 bucket, SQS queue, DLQ, IAM policy, and CloudWatch DLQ-depth alarm |
| **Lombok** | Reduces boilerplate on records and service beans |
| **ConcurrentHashMap** | In-process job store shared between the upload path and the processor via a Spring-managed singleton bean |

---

## REST API

### Upload a file

```
POST /api/v1/files/upload
Content-Type: multipart/form-data

file=<binary>
```

**Response `202 Accepted`**

```json
{
  "processingId": "e3b0c442-...",
  "status": "PENDING",
  "fileName": "report.txt",
  "lineCount": null,
  "characterCount": null,
  "message": "File received and queued for processing",
  "completedAt": null
}
```

---

### Get processing status

```
GET /api/v1/files/{processingId}/status
```

**Response `200 OK`** (while in progress)

```json
{
  "processingId": "e3b0c442-...",
  "status": "PROCESSING",
  "fileName": "report.txt",
  "lineCount": null,
  "characterCount": null,
  "message": "File is being processed",
  "completedAt": null
}
```

**Response `200 OK`** (on completion)

```json
{
  "processingId": "e3b0c442-...",
  "status": "COMPLETED",
  "fileName": "report.txt",
  "lineCount": 142,
  "characterCount": 8301,
  "message": "Processing completed successfully",
  "completedAt": "2026-04-11T10:23:45.123Z"
}
```

Possible `status` values: `PENDING` | `PROCESSING` | `COMPLETED` | `FAILED`

---

## Infrastructure

The CloudFormation stack (`infrastructure/stack.yaml`) creates all required AWS resources and wires them together:

- **S3 bucket** — versioning enabled, SSE-S3 encryption, public access fully blocked, 30-day lifecycle expiry, fires `ObjectCreated` events to the SQS queue.
- **SQS queue** — 5-minute visibility timeout, 7-day retention, redrive policy after 5 failures.
- **Dead Letter Queue** — 14-day retention; a CloudWatch alarm fires as soon as any message lands in the DLQ.
- **IAM managed policy** — least-privilege `s3:PutObject`, `s3:GetObject`, `s3:ListBucket`, and `sqs:ReceiveMessage` / `sqs:DeleteMessage` permissions for the application identity.
