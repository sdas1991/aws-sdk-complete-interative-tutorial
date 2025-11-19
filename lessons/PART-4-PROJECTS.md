# 🧩 PART 4 — Real-World Projects

Welcome to **Part 4**! Now you'll build **3 production-ready applications** that demonstrate real-world AWS SDK usage.

---

## 🎯 What You'll Build

### Project 1: S3 File Manager 📦
**Complexity:** ⭐⭐☆☆☆ (Intermediate)
**Time:** 3-4 hours
**Skills:** S3, file operations, public/private permissions

A complete file management system with:
- Upload/download files
- List bucket contents with pagination
- Public/private file sharing
- File metadata tracking
- Pre-signed URL generation

---

### Project 2: Event-Driven Notification System 📨
**Complexity:** ⭐⭐⭐☆☆ (Advanced)
**Time:** 4-6 hours
**Skills:** Lambda, SNS, SQS, event-driven architecture

A scalable notification system:
- Lambda triggers from S3 events
- SNS topic for fan-out messaging
- SQS queues for different notification types
- Consumer applications (email, SMS, webhooks)
- Dead-letter queue handling

---

### Project 3: Serverless AI API with Bedrock 🤖
**Complexity:** ⭐⭐⭐⭐☆ (Expert)
**Time:** 6-8 hours
**Skills:** S3, Lambda, Bedrock, DynamoDB, API design

An AI-powered document processor:
- Upload documents to S3
- Trigger Lambda function
- Process with AWS Bedrock (Claude)
- Store results in DynamoDB
- REST API for interaction

---

## 📦 PROJECT 1: S3 File Manager

### Architecture

```
┌─────────────┐
│   Client    │
│  (Web/CLI)  │
└──────┬──────┘
       │
       ├─── Upload File ────────┐
       ├─── Download File ──────┤
       ├─── List Files ─────────┤
       ├─── Share File ─────────┤
       └─── Delete File ────────┤
                                │
                                ▼
                        ┌───────────────┐
                        │ File Manager  │
                        │   Service     │
                        └───────┬───────┘
                                │
                                ▼
                        ┌───────────────┐
                        │  Amazon S3    │
                        │               │
                        │  • my-bucket  │
                        │    /public/   │
                        │    /private/  │
                        └───────────────┘
```

### Project Structure

```
project1-s3-file-manager/
├── src/main/kotlin/com/awssdk/projects/filemanager/
│   ├── FileManagerService.kt       # Main service
│   ├── models/
│   │   ├── FileMetadata.kt         # File metadata model
│   │   ├── UploadResult.kt         # Upload response
│   │   └── FilePermission.kt       # PUBLIC/PRIVATE enum
│   ├── cli/
│   │   └── FileManagerCLI.kt       # Command-line interface
│   └── api/
│       └── FileManagerAPI.kt       # REST API (optional)
├── src/test/kotlin/
│   └── FileManagerServiceTest.kt
├── docker-compose.yml               # LocalStack setup
└── README.md
```

### Step 1: Define Data Models

**File:** `src/main/kotlin/com/awssdk/projects/filemanager/models/FileMetadata.kt`

```kotlin
package com.awssdk.projects.filemanager.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class FileMetadata(
    val fileName: String,
    val s3Key: String,
    val size: Long,
    val contentType: String,
    val permission: FilePermission,
    val uploadedAt: String = Instant.now().toString(),
    val etag: String? = null,
    val publicUrl: String? = null,
    val presignedUrl: String? = null
)

enum class FilePermission {
    PUBLIC,   // Anyone can access
    PRIVATE   // Requires authentication
}

@Serializable
data class UploadResult(
    val success: Boolean,
    val fileMetadata: FileMetadata? = null,
    val error: String? = null
)

@Serializable
data class FileList(
    val files: List<FileMetadata>,
    val totalCount: Int,
    val hasMore: Boolean,
    val nextToken: String? = null
)
```

### Step 2: Implement Core Service

**File:** `src/main/kotlin/com/awssdk/projects/filemanager/FileManagerService.kt`

```kotlin
package com.awssdk.projects.filemanager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.content.ByteStream
import com.awssdk.projects.filemanager.models.*
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.hours

class FileManagerService(
    private val s3Client: S3Client,
    private val bucketName: String,
    private val publicBaseUrl: String? = null
) {

    /**
     * Upload a file to S3
     */
    suspend fun uploadFile(
        file: File,
        permission: FilePermission = FilePermission.PRIVATE,
        customKey: String? = null
    ): UploadResult {
        return try {
            val key = customKey ?: buildKey(file.name, permission)
            val contentType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

            // Upload file
            val response = s3Client.putObject {
                bucket = bucketName
                this.key = key
                body = ByteStream.fromFile(file)
                this.contentType = contentType

                // Set ACL for public files
                if (permission == FilePermission.PUBLIC) {
                    acl = ObjectCannedAcl.PublicRead
                }

                // Add metadata
                metadata = mapOf(
                    "original-filename" to file.name,
                    "upload-timestamp" to System.currentTimeMillis().toString()
                )
            }

            val publicUrl = if (permission == FilePermission.PUBLIC && publicBaseUrl != null) {
                "$publicBaseUrl/$key"
            } else null

            val metadata = FileMetadata(
                fileName = file.name,
                s3Key = key,
                size = file.length(),
                contentType = contentType,
                permission = permission,
                etag = response.eTag,
                publicUrl = publicUrl
            )

            UploadResult(success = true, fileMetadata = metadata)

        } catch (e: Exception) {
            UploadResult(success = false, error = e.message)
        }
    }

    /**
     * Download a file from S3
     */
    suspend fun downloadFile(
        s3Key: String,
        destinationPath: String
    ): Boolean {
        return try {
            val response = s3Client.getObject {
                bucket = bucketName
                key = s3Key
            }

            response.body?.toFile(File(destinationPath))

            true
        } catch (e: Exception) {
            println("❌ Download failed: ${e.message}")
            false
        }
    }

    /**
     * List all files with pagination
     */
    suspend fun listFiles(
        prefix: String? = null,
        maxKeys: Int = 100,
        continuationToken: String? = null
    ): FileList {
        val response = s3Client.listObjectsV2 {
            bucket = bucketName
            this.prefix = prefix
            this.maxKeys = maxKeys
            this.continuationToken = continuationToken
        }

        val files = response.contents?.map { obj ->
            FileMetadata(
                fileName = obj.key?.substringAfterLast("/") ?: obj.key ?: "",
                s3Key = obj.key ?: "",
                size = obj.size ?: 0,
                contentType = "unknown",
                permission = detectPermission(obj.key ?: ""),
                uploadedAt = obj.lastModified.toString(),
                etag = obj.eTag
            )
        } ?: emptyList()

        return FileList(
            files = files,
            totalCount = files.size,
            hasMore = response.isTruncated ?: false,
            nextToken = response.nextContinuationToken
        )
    }

    /**
     * Generate a presigned URL for temporary access
     */
    suspend fun generatePresignedDownloadUrl(
        s3Key: String,
        expirationHours: Int = 1
    ): String {
        val presignedRequest = s3Client.presignGetObject(
            GetObjectRequest {
                bucket = bucketName
                key = s3Key
            },
            expirationHours.hours
        )

        return presignedRequest.url.toString()
    }

    /**
     * Generate a presigned URL for upload
     */
    suspend fun generatePresignedUploadUrl(
        fileName: String,
        contentType: String,
        expirationHours: Int = 1
    ): String {
        val key = buildKey(fileName, FilePermission.PRIVATE)

        val presignedRequest = s3Client.presignPutObject(
            PutObjectRequest {
                bucket = bucketName
                this.key = key
                this.contentType = contentType
            },
            expirationHours.hours
        )

        return presignedRequest.url.toString()
    }

    /**
     * Delete a file
     */
    suspend fun deleteFile(s3Key: String): Boolean {
        return try {
            s3Client.deleteObject {
                bucket = bucketName
                key = s3Key
            }
            true
        } catch (e: Exception) {
            println("❌ Delete failed: ${e.message}")
            false
        }
    }

    /**
     * Get file metadata
     */
    suspend fun getFileMetadata(s3Key: String): FileMetadata? {
        return try {
            val response = s3Client.headObject {
                bucket = bucketName
                key = s3Key
            }

            FileMetadata(
                fileName = s3Key.substringAfterLast("/"),
                s3Key = s3Key,
                size = response.contentLength ?: 0,
                contentType = response.contentType ?: "unknown",
                permission = detectPermission(s3Key),
                uploadedAt = response.lastModified.toString(),
                etag = response.eTag
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Make a file public
     */
    suspend fun makePublic(s3Key: String): Boolean {
        return try {
            s3Client.putObjectAcl {
                bucket = bucketName
                key = s3Key
                acl = ObjectCannedAcl.PublicRead
            }
            true
        } catch (e: Exception) {
            println("❌ Failed to make public: ${e.message}")
            false
        }
    }

    /**
     * Make a file private
     */
    suspend fun makePrivate(s3Key: String): Boolean {
        return try {
            s3Client.putObjectAcl {
                bucket = bucketName
                key = s3Key
                acl = ObjectCannedAcl.Private
            }
            true
        } catch (e: Exception) {
            println("❌ Failed to make private: ${e.message}")
            false
        }
    }

    // Helper functions
    private fun buildKey(fileName: String, permission: FilePermission): String {
        val prefix = if (permission == FilePermission.PUBLIC) "public" else "private"
        val timestamp = System.currentTimeMillis()
        return "$prefix/$timestamp-$fileName"
    }

    private fun detectPermission(key: String): FilePermission {
        return if (key.startsWith("public/")) {
            FilePermission.PUBLIC
        } else {
            FilePermission.PRIVATE
        }
    }
}
```

### Step 3: Build Command-Line Interface

**File:** `src/main/kotlin/com/awssdk/projects/filemanager/cli/FileManagerCLI.kt`

```kotlin
package com.awssdk.projects.filemanager.cli

import com.awssdk.common.AwsClientFactory
import com.awssdk.projects.filemanager.FileManagerService
import com.awssdk.projects.filemanager.models.FilePermission
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) = runBlocking {
    val bucketName = System.getenv("BUCKET_NAME") ?: "file-manager-bucket"

    val service = FileManagerService(
        s3Client = AwsClientFactory.createS3Client(),
        bucketName = bucketName,
        publicBaseUrl = "https://$bucketName.s3.amazonaws.com"
    )

    println("📦 S3 File Manager CLI")
    println("=".repeat(50))

    if (args.isEmpty()) {
        printUsage()
        return@runBlocking
    }

    when (args[0]) {
        "upload" -> {
            if (args.size < 2) {
                println("❌ Usage: upload <file-path> [public|private]")
                return@runBlocking
            }

            val filePath = args[1]
            val permission = if (args.size > 2 && args[2] == "public") {
                FilePermission.PUBLIC
            } else {
                FilePermission.PRIVATE
            }

            val file = File(filePath)
            if (!file.exists()) {
                println("❌ File not found: $filePath")
                return@runBlocking
            }

            println("📤 Uploading ${file.name}...")
            val result = service.uploadFile(file, permission)

            if (result.success) {
                println("✅ Upload successful!")
                println("   File: ${result.fileMetadata?.fileName}")
                println("   S3 Key: ${result.fileMetadata?.s3Key}")
                println("   Size: ${result.fileMetadata?.size} bytes")
                println("   Permission: ${result.fileMetadata?.permission}")
                result.fileMetadata?.publicUrl?.let {
                    println("   Public URL: $it")
                }
            } else {
                println("❌ Upload failed: ${result.error}")
            }
        }

        "download" -> {
            if (args.size < 3) {
                println("❌ Usage: download <s3-key> <destination-path>")
                return@runBlocking
            }

            val s3Key = args[1]
            val destPath = args[2]

            println("📥 Downloading $s3Key...")
            val success = service.downloadFile(s3Key, destPath)

            if (success) {
                println("✅ Downloaded to: $destPath")
            }
        }

        "list" -> {
            val prefix = if (args.size > 1) args[1] else null

            println("📋 Listing files${prefix?.let { " with prefix: $it" } ?: ""}...")
            val result = service.listFiles(prefix = prefix)

            if (result.files.isEmpty()) {
                println("📭 No files found")
            } else {
                println("✅ Found ${result.totalCount} file(s):\n")

                result.files.forEachIndexed { index, file ->
                    println("${index + 1}. ${file.fileName}")
                    println("   Key: ${file.s3Key}")
                    println("   Size: ${file.size} bytes")
                    println("   Permission: ${file.permission}")
                    println("   Uploaded: ${file.uploadedAt}")
                    println()
                }

                if (result.hasMore) {
                    println("⚠️  More files available (use pagination)")
                }
            }
        }

        "share" -> {
            if (args.size < 2) {
                println("❌ Usage: share <s3-key> [hours]")
                return@runBlocking
            }

            val s3Key = args[1]
            val hours = if (args.size > 2) args[2].toIntOrNull() ?: 1 else 1

            println("🔗 Generating share link for $s3Key...")
            val url = service.generatePresignedDownloadUrl(s3Key, hours)

            println("✅ Share link (valid for $hours hour(s)):")
            println("   $url")
        }

        "delete" -> {
            if (args.size < 2) {
                println("❌ Usage: delete <s3-key>")
                return@runBlocking
            }

            val s3Key = args[1]

            println("🗑️  Deleting $s3Key...")
            val success = service.deleteFile(s3Key)

            if (success) {
                println("✅ File deleted")
            }
        }

        "info" -> {
            if (args.size < 2) {
                println("❌ Usage: info <s3-key>")
                return@runBlocking
            }

            val s3Key = args[1]

            println("ℹ️  Getting metadata for $s3Key...")
            val metadata = service.getFileMetadata(s3Key)

            if (metadata != null) {
                println("✅ File metadata:")
                println("   Name: ${metadata.fileName}")
                println("   Size: ${metadata.size} bytes")
                println("   Type: ${metadata.contentType}")
                println("   Permission: ${metadata.permission}")
                println("   Uploaded: ${metadata.uploadedAt}")
                println("   ETag: ${metadata.etag}")
            } else {
                println("❌ File not found")
            }
        }

        else -> printUsage()
    }
}

fun printUsage() {
    println("""
        Usage: filemanager <command> [options]

        Commands:
          upload <file-path> [public|private]  - Upload a file
          download <s3-key> <dest-path>        - Download a file
          list [prefix]                        - List all files
          share <s3-key> [hours]               - Generate share link
          delete <s3-key>                      - Delete a file
          info <s3-key>                        - Get file metadata

        Examples:
          filemanager upload myfile.pdf public
          filemanager download public/123-myfile.pdf ./downloads/myfile.pdf
          filemanager list public/
          filemanager share private/456-doc.pdf 24
          filemanager delete public/123-myfile.pdf
          filemanager info public/123-myfile.pdf
    """.trimIndent())
}
```

### Step 4: Docker Setup

**File:** `project1-s3-file-manager/docker-compose.yml`

```yaml
version: '3.8'

services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3
      - DEBUG=1
    volumes:
      - "./localstack-data:/tmp/localstack"

  file-manager:
    build: .
    environment:
      - AWS_ENDPOINT_URL=http://localstack:4566
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - BUCKET_NAME=file-manager-bucket
      - USE_LOCALSTACK=true
    depends_on:
      - localstack
    volumes:
      - .:/app
    command: tail -f /dev/null
```

### Step 5: Testing

**Run the CLI:**

```bash
# Start environment
docker-compose up -d

# Create bucket
docker-compose exec file-manager aws --endpoint-url=http://localstack:4566 s3 mb s3://file-manager-bucket

# Upload a file
docker-compose exec file-manager gradle run --args="upload /path/to/file.pdf public"

# List files
docker-compose exec file-manager gradle run --args="list"

# Generate share link
docker-compose exec file-manager gradle run --args="share public/123-file.pdf 24"
```

---

## 💡 Do you want to try a hands-on lab?

### Lab 4.1: Extend the File Manager

**Your Tasks:**
1. Add file search functionality (search by name)
2. Implement file tagging
3. Add file size validation (max 100MB)
4. Create a batch upload feature
5. Add progress tracking for uploads

**✨ Senior-Level Challenge:**
- Implement file versioning
- Add virus scanning integration
- Create file thumbnails for images
- Implement server-side encryption

**🔒 Security Best Practice:**
- Validate file types (block dangerous extensions)
- Sanitize file names
- Implement rate limiting
- Add audit logging

---

## 📨 PROJECT 2: Event-Driven Notification System

*(Continue in next response - this is getting long!)*

---

**What do you want to do next?**
1. Continue with Project 2 (Event-Driven Notification System)
2. Continue with Project 3 (Serverless AI API with Bedrock)
3. See the complete Part 5 (Expert Best Practices)
4. Jump to Part 6 (Mastery Quiz & Capstone)

Let me know! 🚀
