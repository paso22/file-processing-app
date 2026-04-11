# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=FileProcessingAppApplicationTests

# Run a single test method
./mvnw test -Dtest=FileProcessingAppApplicationTests#contextLoads
```

## Stack

- **Java 25**, Spring Boot 4.0.5
- **Spring Web MVC** (`spring-boot-starter-webmvc`) — servlet-based REST
- **Lombok** — used for boilerplate reduction; annotation processing is configured in `pom.xml`
- Base package: `ge.paso22.fileprocessingapp`

## Architecture

The entry point is `FileProcessingAppApplication` (note: `main` is package-private — update to `public` if Spring Boot fails to launch).

New code should be organized under the base package using standard Spring layering (controller → service → repository). File processing logic belongs in a dedicated service layer, not in controllers.

## About the project
**File-Upload & Processing Service**

Here's the app idea: a Spring Boot REST API that lets users upload files (e.g., CSVs or images), stores them, and sends a notification when processing is done. It's a real pattern used in fintech, HR tools, data pipelines, e-commerce — everywhere. Not a toy, but not overwhelming either.
This naturally involves exactly three AWS services — a sweet spot for a first CloudFormation project:
S3 (Simple Storage Service) for storing uploaded files. SQS (Simple Queue Service) for sending an async message when a file arrives. IAM (Identity & Access Management) for giving your Spring Boot app the right permissions to talk to S3 and SQS. IAM is technically not a "service you use" directly, but you'll define roles and policies in CloudFormation — this is one of the most important CloudFormation skills to build early.
Why this combination is ideal for your situation: S3 and SQS are foundational services that appear in almost every real-world AWS architecture. They're well-supported by the Java SDK. The interaction between them (S3 triggers a message to SQS when a file is uploaded) lets you learn CloudFormation resource dependencies, which is one of the trickiest and most important concepts. And IAM forces you to think about permissions explicitly, which is a critical habit.

The Step-by-Step Plan
Phase 1 — Local Foundation (No AWS yet)
Start by building the Spring Boot app locally with no AWS at all. Create a simple REST endpoint POST /files/upload that accepts a MultipartFile. For now, just save it to a local temp directory and log a "notification sent" message. This way, your app logic is already clear before AWS complexity enters the picture.
This matters because a common beginner mistake is trying to learn CloudFormation and the AWS SDK simultaneously while also writing application logic. Separate these concerns.
Phase 2 — Your First CloudFormation Template (S3 only)
Write a CloudFormation YAML template that creates a single S3 bucket. Deploy it manually using the AWS Console's CloudFormation stack wizard. Verify the bucket appeared in S3. Then delete the stack and watch the bucket disappear. This teaches you the most fundamental CloudFormation concept: infrastructure as a lifecycle, not just a setup script.
At this stage you'll also create an IAM User (just for local development) with programmatic access, and store its credentials in your Spring Boot application.properties using the AWS SDK's credential chain. You'll add the spring-cloud-aws or aws-sdk-java-v2 dependency and wire up an S3Client bean.
Phase 3 — Expand the Template (SQS + IAM Role)
Add an SQS queue to your CloudFormation template. Then add an S3 Event Notification that fires a message to SQS whenever a file is uploaded. This is where CloudFormation gets interesting — you'll learn about the DependsOn attribute and the !Ref / !GetAtt intrinsic functions, which are how CloudFormation resources refer to each other.
You'll also define an IAM Policy in the template that grants s3:PutObject and sqs:SendMessage permissions, and attach it to your dev user. Seeing permissions defined as code — rather than clicked through a console — is genuinely eye-opening.
Phase 4 — Spring Boot Consumes SQS
Add a second component to your Spring Boot app: an SQS listener (using @SqsListener if you use Spring Cloud AWS, or a scheduled poller otherwise) that reads messages from the queue and logs "File X was uploaded and is ready for processing." Now your app has a full async flow: HTTP upload → S3 → SQS → listener.
Phase 5 — CloudFormation Parameters & Outputs
Refactor your template to use Parameters (e.g., BucketName, Environment) and Outputs (e.g., export the queue URL so other stacks could reference it). This is what makes CloudFormation templates reusable across dev/staging/prod environments, and it's a concept every CloudFormation practitioner must know well.
Phase 6 (Stretch) — Deploy the App to EC2 with an Instance Role
Instead of using a hardcoded IAM user for credentials, launch an EC2 instance via CloudFormation and attach an IAM Instance Role to it. Deploy your Spring Boot JAR to it. The app will automatically pick up credentials from the instance metadata — no keys in config files. This teaches you the "right" way AWS apps authenticate in production, and it ties together EC2, IAM, S3, and SQS all in one template.

## Technical rules
- Use two spaces for indentation
- For every controller class, write an interface where you define mappings. After it, write implementation with a suffix "*Impl"
where you write all remaining necessary annotations. For example, UserController → UserControllerImpl
- For every service class, write an interface and then implement its logic in an appropriate class. Implementation class name should be named with a suffix "*Bean".
For example, UserService → UserServiceBean
- Avoid inline comments for self-explanatory code. Write Javadoc on all public methods and classes
- Always write single class imports to avoid redundant class imports
- Leave an empty line between class level fields for better readability
- Inside the method, after each if block leave an empty line for better readability
- Use records for immutable data transfer objects (DTOs) and value objects, but not for JPA entity classes
- Mark classes as final if they are not designed for inheritance. Mark local variables as final if they are not reassigned
- When writing an endpoint path in @RequestMapping annotation, apply the following pattern "/api/v1/{entityNameInPlural}"
- After writing the code, always check unused imports and if you find it, delete
- If unit tests or integration tests are written, after editing or creating classes, always check existing tests status and fix if some of them got broken
- Instead of writing getter methods, use lombok's @Getter annotation on top of the class
- Instead of writing setter methods, use lombok's @Setter annotation on top of the class
- Instead of writing an empty constructor, use lombok's @NoArgsConstructor
- Instead of writing a constructor with class's every field as parameters, use @AllArgsConstructor on top of the class