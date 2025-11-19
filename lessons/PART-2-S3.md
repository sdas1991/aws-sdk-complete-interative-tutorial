# 📦 PART 2 — S3 (Simple Storage Service)

Welcome to **Part 2 - S3**! Amazon S3 is object storage built to store and retrieve any amount of data. Let's master it!

---

## 🎯 Learning Objectives

By the end of this module, you will:

✅ Upload files to S3 (PutObject)
✅ Download files from S3 (GetObject)
✅ List objects in a bucket
✅ Generate pre-signed URLs for secure sharing
✅ Delete objects
✅ Enable versioning
✅ Set bucket policies
✅ Handle multipart uploads for large files
✅ Build a mini project: **S3 File Uploader Service**

---

## 📚 Lesson 2.1: S3 Basics

### What is S3?

**Amazon S3** (Simple Storage Service) is:
- **Object storage** - Store files as objects (not block storage)
- **Scalable** - Unlimited storage capacity
- **Durable** - 99.999999999% (11 9's) durability
- **Available** - 99.99% availability
- **Secure** - Encryption at rest and in transit

### Key Concepts

| Concept | Description | Example |
|---------|-------------|---------|
| **Bucket** | Container for objects | `my-app-uploads` |
| **Object** | File + metadata | `photos/sunset.jpg` |
| **Key** | Object's unique identifier | `users/123/profile.png` |
| **Version ID** | For versioned objects | `abc123xyz` |
| **Region** | Geographic location | `us-east-1` |

### S3 Storage Classes

```
┌─────────────────────────────────────────────────┐
│ S3 Standard                                     │
│ - Frequent access                               │
│ - Low latency                                   │
│ - $0.023/GB/month                               │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ S3 Intelligent-Tiering                          │
│ - Automatic cost optimization                   │
│ - Moves data between tiers                      │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ S3 Glacier                                      │
│ - Archive storage                               │
│ - Minutes to hours retrieval                    │
│ - $0.004/GB/month                               │
└─────────────────────────────────────────────────┘
```

---

## 🏃 Exercise 1: Upload a File

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/s3/UploadFile.kt`

Let's upload a file to S3!

### Step 1: Create the uploader

```kotlin
package com.awssdk.tutorial.part2.s3

import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.content.ByteStream
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val bucketName = "my-kotlin-tutorial-bucket"
    val objectKey = "test-files/hello.txt"
    val fileContent = "Hello from AWS SDK for Kotlin!"

    println("📤 Uploading file to S3")
    println("=".repeat(50))
    println("Bucket: $bucketName")
    println("Key: $objectKey")

    try {
        AwsClientFactory.createS3Client().use { s3Client ->

            // Upload the file
            s3Client.putObject {
                bucket = bucketName
                key = objectKey
                body = ByteStream.fromString(fileContent)
                contentType = "text/plain"
            }

            println("✅ File uploaded successfully!")

            // Verify upload
            val headResponse = s3Client.headObject {
                bucket = bucketName
                key = objectKey
            }

            println("\n📊 Object Details:")
            println("   Content-Type: ${headResponse.contentType}")
            println("   Content-Length: ${headResponse.contentLength} bytes")
            println("   ETag: ${headResponse.eTag}")
            println("   Last Modified: ${headResponse.lastModified}")
        }

    } catch (e: S3Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

### Step 2: Run it

```bash
docker-compose exec kotlin-app gradle run -PmainClass=com.awssdk.tutorial.part2.s3.UploadFileKt
```

---

## 🏃 Exercise 2: Download a File

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/s3/DownloadFile.kt`

```kotlin
package com.awssdk.tutorial.part2.s3

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val bucketName = "my-kotlin-tutorial-bucket"
    val objectKey = "test-files/hello.txt"
    val downloadPath = "/tmp/downloaded-file.txt"

    println("📥 Downloading file from S3")
    println("=".repeat(50))

    try {
        AwsClientFactory.createS3Client().use { s3Client ->

            // Download the file
            val response = s3Client.getObject {
                bucket = bucketName
                key = objectKey
            }

            // Save to local file
            response.body?.toByteArray()?.let { bytes ->
                File(downloadPath).writeBytes(bytes)
                println("✅ File downloaded successfully!")
                println("   Saved to: $downloadPath")
                println("   Size: ${bytes.size} bytes")
                println("\n📄 Content:")
                println(String(bytes))
            }
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 3: List Objects in Bucket

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/s3/ListObjects.kt`

```kotlin
package com.awssdk.tutorial.part2.s3

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val bucketName = "my-kotlin-tutorial-bucket"

    println("📋 Listing objects in S3 bucket")
    println("=".repeat(50))

    try {
        AwsClientFactory.createS3Client().use { s3Client ->

            val response = s3Client.listObjectsV2 {
                bucket = bucketName
                maxKeys = 10
            }

            val objects = response.contents

            if (objects.isNullOrEmpty()) {
                println("📭 No objects found in bucket: $bucketName")
            } else {
                println("✅ Found ${objects.size} object(s):\n")

                objects.forEach { obj ->
                    println("📄 ${obj.key}")
                    println("   Size: ${obj.size} bytes")
                    println("   Last Modified: ${obj.lastModified}")
                    println("   ETag: ${obj.eTag}")
                    println()
                }
            }

            // Show pagination info
            if (response.isTruncated == true) {
                println("⚠️  More objects available (truncated)")
                println("   Next continuation token: ${response.nextContinuationToken}")
            }
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 4: Generate Pre-Signed URL

Pre-signed URLs allow temporary access to private objects without exposing credentials.

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/s3/GeneratePresignedUrl.kt`

```kotlin
package com.awssdk.tutorial.part2.s3

import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.hours

fun main() = runBlocking {
    val bucketName = "my-kotlin-tutorial-bucket"
    val objectKey = "test-files/hello.txt"

    println("🔗 Generating Pre-Signed URL")
    println("=".repeat(50))

    try {
        AwsClientFactory.createS3Client().use { s3Client ->

            // Create presigned URL valid for 1 hour
            val presignedRequest = s3Client.presignGetObject(
                GetObjectRequest {
                    bucket = bucketName
                    key = objectKey
                },
                1.hours
            )

            println("✅ Pre-signed URL generated!")
            println("\n🔗 URL:")
            println(presignedRequest.url)
            println("\n⏱️  Valid for: 1 hour")
            println("\n💡 Share this URL to allow temporary download access")
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🎓 Mini Project: S3 File Uploader Service

Build a simple file uploader with these features:

✅ Upload files
✅ List uploaded files
✅ Generate shareable links
✅ Delete files
✅ Track upload metadata

**Structure:**

```
src/main/kotlin/com/awssdk/tutorial/part2/s3/project/
├── FileUploaderService.kt    # Main service
├── FileMetadata.kt            # Data class
└── FileUploaderApp.kt         # CLI application
```

Check the `part2/s3/project/` directory for the complete implementation!

---

## 🧪 Quiz: S3 Knowledge Check

### Question 1
What's the maximum size of a single S3 object?
- A) 5 GB
- B) 5 TB
- C) 100 GB
- D) 1 TB

<details>
<summary>Answer</summary>

**B) 5 TB**

Individual objects can be up to 5 TB. For files larger than 5 GB, you should use multipart upload.
</details>

### Question 2
Pre-signed URLs are best used for:
- A) Making all S3 objects public
- B) Temporary access to private objects
- C) Permanent public access
- D) Faster uploads

<details>
<summary>Answer</summary>

**B) Temporary access to private objects**

Pre-signed URLs grant time-limited access to private S3 objects without requiring AWS credentials.
</details>

---

## ✅ Checkpoint

Can you:

- [ ] Upload a file to S3
- [ ] Download a file from S3
- [ ] List objects in a bucket
- [ ] Generate a pre-signed URL
- [ ] Explain the difference between bucket and object
- [ ] Understand when to use pre-signed URLs

---

## 🎯 Next Steps

Choose your path:

1. **[Part 2: DynamoDB →](./PART-2-DYNAMODB.md)** - NoSQL database
2. **[Part 2: SQS/SNS →](./PART-2-MESSAGING.md)** - Messaging services
3. **[Part 3: Advanced S3 →](./PART-3-ADVANCED-S3.md)** - Multipart upload, versioning, lifecycle

**What do you want to learn next?** 🚀
