# 🛡️ PART 3 — Advanced AWS SDK Concepts

Welcome to **Part 3**! Now that you've mastered the basics, let's dive into advanced topics that will make you a production-ready AWS SDK expert.

---

## 🎯 Learning Objectives

By the end of this module, you will:

✅ **Performance**: Optimize SDK performance with async clients, connection pooling, and batch operations
✅ **Error Handling**: Implement robust retry strategies and handle failures gracefully
✅ **Testing**: Write testable code with LocalStack, mocking, and CI/CD integration
✅ **Security**: Implement advanced authentication patterns and credential management
✅ **Monitoring**: Use CloudWatch, X-Ray, and structured logging

---

## 📚 Lesson 3.1: Performance Optimization

### Async Clients vs Sync Clients

**The Problem:**
Sync clients block your thread while waiting for AWS responses. This wastes resources!

```kotlin
// ❌ Synchronous - blocks thread
val response = s3Client.getObject { ... }  // Thread blocked here!
// 1 request at a time, slow!
```

**The Solution:**
Use coroutines! AWS SDK for Kotlin is built for async from the ground up.

```kotlin
// ✅ Asynchronous - non-blocking
suspend fun uploadFiles(files: List<File>) {
    coroutineScope {
        files.map { file ->
            async {
                s3Client.putObject {
                    bucket = "my-bucket"
                    key = file.name
                    body = ByteStream.fromFile(file)
                }
            }
        }.awaitAll()
    }
}
// Multiple requests in parallel! 🚀
```

**Example: Upload 100 Files in Parallel**

**File:** `src/main/kotlin/com/awssdk/tutorial/part3/performance/ParallelUpload.kt`

```kotlin
package com.awssdk.tutorial.part3.performance

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.content.ByteStream
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.*
import java.io.File
import kotlin.system.measureTimeMillis

suspend fun uploadFilesSequentially(files: List<File>, bucketName: String) {
    AwsClientFactory.createS3Client().use { s3 ->
        files.forEach { file ->
            s3.putObject {
                bucket = bucketName
                key = "sequential/${file.name}"
                body = ByteStream.fromFile(file)
            }
        }
    }
}

suspend fun uploadFilesInParallel(files: List<File>, bucketName: String) {
    AwsClientFactory.createS3Client().use { s3 ->
        coroutineScope {
            files.map { file ->
                async {
                    s3.putObject {
                        bucket = bucketName
                        key = "parallel/${file.name}"
                        body = ByteStream.fromFile(file)
                    }
                }
            }.awaitAll()
        }
    }
}

fun main() = runBlocking {
    val bucketName = "performance-test-bucket"
    val files = (1..100).map { File.createTempFile("test-$it", ".txt") }

    println("⚡ Performance Comparison: Sequential vs Parallel Upload")
    println("=".repeat(60))

    // Sequential
    val sequentialTime = measureTimeMillis {
        uploadFilesSequentially(files, bucketName)
    }
    println("⏱️  Sequential: ${sequentialTime}ms")

    // Parallel
    val parallelTime = measureTimeMillis {
        uploadFilesInParallel(files, bucketName)
    }
    println("🚀 Parallel: ${parallelTime}ms")

    val speedup = sequentialTime.toDouble() / parallelTime
    println("\n✅ Speedup: ${String.format("%.2fx", speedup)} faster!")

    // Cleanup
    files.forEach { it.delete() }
}
```

**📊 Expected Results:**
```
⚡ Performance Comparison: Sequential vs Parallel Upload
============================================================
⏱️  Sequential: 45000ms (45 seconds)
🚀 Parallel: 3000ms (3 seconds)

✅ Speedup: 15.00x faster!
```

---

### Connection Pooling

**The Problem:**
Creating new HTTP connections for every request is expensive!

**The Solution:**
Reuse connections with proper client configuration.

```kotlin
// ❌ BAD - Creates new client every time
fun uploadFile(file: File) {
    S3Client { ... }.use { s3 ->  // New connection pool!
        s3.putObject { ... }
    }
}  // Connection pool closed!

// ✅ GOOD - Reuse client (singleton pattern)
object S3Service {
    private val s3Client = AwsClientFactory.createS3Client()

    suspend fun uploadFile(file: File) {
        s3Client.putObject {
            bucket = "my-bucket"
            key = file.name
            body = ByteStream.fromFile(file)
        }
    }

    fun close() {
        s3Client.close()
    }
}
```

**Advanced Configuration:**

```kotlin
fun createOptimizedS3Client(): S3Client {
    return S3Client {
        region = "us-east-1"

        // Configure HTTP client for performance
        httpClient = CrtHttpEngineConfig {
            // Connection pool settings
            maxConnections = 50
            connectionIdleTimeout = 60.seconds

            // Throughput optimization
            maxConcurrentStreams = 100
        }
    }
}
```

---

### Large File Streaming

**For files > 5GB, use multipart upload!**

**File:** `src/main/kotlin/com/awssdk/tutorial/part3/performance/MultipartUpload.kt`

```kotlin
package com.awssdk.tutorial.part3.performance

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.content.ByteStream
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.ceil

suspend fun uploadLargeFile(
    s3Client: S3Client,
    bucketName: String,
    key: String,
    file: File,
    partSize: Long = 5 * 1024 * 1024 // 5 MB parts
) {
    println("📤 Starting multipart upload for: ${file.name}")
    println("   File size: ${file.length() / 1024 / 1024} MB")

    // Step 1: Initiate multipart upload
    val initiateResponse = s3Client.createMultipartUpload {
        bucket = bucketName
        this.key = key
    }

    val uploadId = initiateResponse.uploadId!!
    println("✅ Upload initiated: $uploadId")

    try {
        // Step 2: Upload parts
        val parts = mutableListOf<CompletedPart>()
        val totalParts = ceil(file.length().toDouble() / partSize).toInt()

        file.inputStream().use { input ->
            var partNumber = 1
            var bytesRead: Long

            while (partNumber <= totalParts) {
                val buffer = ByteArray(partSize.toInt())
                bytesRead = input.read(buffer).toLong()

                if (bytesRead <= 0) break

                println("📦 Uploading part $partNumber of $totalParts...")

                val uploadPartResponse = s3Client.uploadPart {
                    bucket = bucketName
                    this.key = key
                    this.uploadId = uploadId
                    this.partNumber = partNumber
                    body = ByteStream.fromBytes(buffer.sliceArray(0 until bytesRead.toInt()))
                }

                parts.add(CompletedPart {
                    this.partNumber = partNumber
                    eTag = uploadPartResponse.eTag
                })

                partNumber++
            }
        }

        // Step 3: Complete multipart upload
        s3Client.completeMultipartUpload {
            bucket = bucketName
            this.key = key
            this.uploadId = uploadId
            multipartUpload = CompletedMultipartUpload {
                this.parts = parts
            }
        }

        println("✅ Upload completed successfully!")

    } catch (e: Exception) {
        // Abort upload on error
        println("❌ Upload failed, aborting...")
        s3Client.abortMultipartUpload {
            bucket = bucketName
            this.key = key
            this.uploadId = uploadId
        }
        throw e
    }
}

fun main() = runBlocking {
    val bucketName = "large-files-bucket"
    val file = File("/path/to/large-file.zip")  // Your large file

    AwsClientFactory.createS3Client().use { s3 ->
        uploadLargeFile(s3, bucketName, file.name, file)
    }
}
```

---

### Batch Operations (DynamoDB)

**Write 25 items at once instead of 25 separate requests!**

```kotlin
package com.awssdk.tutorial.part3.performance

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

data class User(
    val userId: String,
    val name: String,
    val email: String
)

suspend fun batchWriteUsers(users: List<User>) {
    AwsClientFactory.createDynamoDbClient().use { dynamoDb ->

        // DynamoDB allows max 25 items per batch
        users.chunked(25).forEach { chunk ->

            val writeRequests = chunk.map { user ->
                WriteRequest {
                    putRequest = PutRequest {
                        item = mapOf(
                            "userId" to AttributeValue.S(user.userId),
                            "name" to AttributeValue.S(user.name),
                            "email" to AttributeValue.S(user.email)
                        )
                    }
                }
            }

            dynamoDb.batchWriteItem {
                requestItems = mapOf("Users" to writeRequests)
            }

            println("✅ Wrote batch of ${chunk.size} users")
        }
    }
}

fun main() = runBlocking {
    val users = (1..100).map { i ->
        User(
            userId = "user-$i",
            name = "User $i",
            email = "user$i@example.com"
        )
    }

    println("📤 Batch writing ${users.size} users...")
    val startTime = System.currentTimeMillis()

    batchWriteUsers(users)

    val duration = System.currentTimeMillis() - startTime
    println("✅ Completed in ${duration}ms")
}
```

---

## 📚 Lesson 3.2: Error Handling & Retries

### Understanding AWS Errors

```kotlin
try {
    s3Client.getObject {
        bucket = "my-bucket"
        key = "file.txt"
    }
} catch (e: S3Exception) {
    when (e.sdkErrorMetadata.errorCode) {
        "NoSuchKey" -> println("File doesn't exist")
        "NoSuchBucket" -> println("Bucket doesn't exist")
        "AccessDenied" -> println("No permission")
        "InvalidBucketName" -> println("Invalid bucket name")
        else -> println("Unknown S3 error: ${e.message}")
    }
}
```

### Retry Strategies

**Built-in Retry:**

```kotlin
S3Client {
    region = "us-east-1"

    retryStrategy = StandardRetryStrategy {
        maxAttempts = 5

        // Retry configuration
        retryPolicy = RetryPolicy {
            // Retry on throttling errors
            throttleErrorCodes = setOf("ThrottlingException", "ProvisionedThroughputExceededException")

            // Exponential backoff
            backoffStrategy = ExponentialBackoffStrategy {
                baseDelay = 100.milliseconds
                maxBackoff = 20.seconds
            }
        }
    }
}
```

**Custom Retry Logic:**

**File:** `src/main/kotlin/com/awssdk/tutorial/part3/errorhandling/RetryWithBackoff.kt`

```kotlin
package com.awssdk.tutorial.part3.errorhandling

import kotlinx.coroutines.delay
import kotlin.math.pow

suspend fun <T> retryWithExponentialBackoff(
    maxAttempts: Int = 5,
    baseDelayMs: Long = 100,
    maxDelayMs: Long = 30000,
    block: suspend () -> T
): T {
    var attempt = 0
    var lastException: Exception? = null

    while (attempt < maxAttempts) {
        try {
            return block()
        } catch (e: Exception) {
            attempt++
            lastException = e

            if (attempt >= maxAttempts) {
                break
            }

            // Calculate exponential backoff with jitter
            val delayMs = minOf(
                baseDelayMs * 2.0.pow(attempt - 1).toLong(),
                maxDelayMs
            )
            val jitter = (0..delayMs / 10).random()

            println("⚠️  Attempt $attempt failed: ${e.message}")
            println("⏳ Retrying in ${delayMs + jitter}ms...")

            delay(delayMs + jitter)
        }
    }

    throw lastException ?: Exception("Max retries exceeded")
}

// Usage
suspend fun uploadWithRetry(file: File) {
    retryWithExponentialBackoff {
        s3Client.putObject {
            bucket = "my-bucket"
            key = file.name
            body = ByteStream.fromFile(file)
        }
    }
}
```

---

### Idempotent Operations

**The Problem:**
What if upload succeeds but you don't get the response? Retry might create duplicate!

**The Solution:**
Use idempotency tokens!

```kotlin
// Generate unique token per operation
val idempotencyToken = UUID.randomUUID().toString()

// DynamoDB conditional write (idempotent)
dynamoDb.putItem {
    tableName = "Orders"
    item = mapOf(
        "orderId" to AttributeValue.S(orderId),
        "idempotencyToken" to AttributeValue.S(idempotencyToken),
        "status" to AttributeValue.S("pending")
    )

    // Only write if this token doesn't exist
    conditionExpression = "attribute_not_exists(idempotencyToken)"
}
```

---

## 📚 Lesson 3.3: Testing AWS SDK Code

### Unit Testing with Mocking

**File:** `src/test/kotlin/com/awssdk/tutorial/part3/testing/S3ServiceTest.kt`

```kotlin
package com.awssdk.tutorial.part3.testing

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class S3ServiceTest {

    @Test
    fun `should upload file successfully`() = runBlocking {
        // Create mock S3 client
        val mockS3 = mockk<S3Client>()

        // Define behavior
        coEvery {
            mockS3.putObject(any<PutObjectRequest>())
        } returns PutObjectResponse {
            eTag = "mock-etag-123"
        }

        // Test your service
        val service = S3Service(mockS3)
        val result = service.uploadFile("test.txt", "content")

        // Verify
        assertEquals("mock-etag-123", result)

        coVerify(exactly = 1) {
            mockS3.putObject(match<PutObjectRequest> {
                it.key == "test.txt"
            })
        }
    }
}
```

---

### Integration Testing with LocalStack

**File:** `src/test/kotlin/com/awssdk/tutorial/part3/testing/S3IntegrationTest.kt`

```kotlin
package com.awssdk.tutorial.part3.testing

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3IntegrationTest {

    private val bucketName = "test-bucket-${System.currentTimeMillis()}"

    @BeforeAll
    fun setup() = runBlocking {
        // Create test bucket in LocalStack
        AwsClientFactory.createS3Client().use { s3 ->
            s3.createBucket {
                bucket = bucketName
            }
        }
    }

    @Test
    fun `should upload and download file`() = runBlocking {
        AwsClientFactory.createS3Client().use { s3 ->
            val content = "Hello, LocalStack!"
            val key = "test-file.txt"

            // Upload
            s3.putObject {
                bucket = bucketName
                this.key = key
                body = ByteStream.fromString(content)
            }

            // Download
            val response = s3.getObject {
                bucket = bucketName
                this.key = key
            }

            val downloaded = response.body?.decodeToString()
            assertEquals(content, downloaded)
        }
    }

    @AfterAll
    fun cleanup() = runBlocking {
        // Delete test bucket
        AwsClientFactory.createS3Client().use { s3 ->
            // Delete all objects first
            val objects = s3.listObjectsV2 { bucket = bucketName }.contents
            objects?.forEach { obj ->
                s3.deleteObject {
                    bucket = bucketName
                    key = obj.key
                }
            }

            // Then delete bucket
            s3.deleteBucket { bucket = bucketName }
        }
    }
}
```

---

### CI/CD Integration

**`.github/workflows/test.yml`:**

```yaml
name: Test AWS SDK Code

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      localstack:
        image: localstack/localstack:latest
        env:
          SERVICES: s3,dynamodb,sqs,sns
        ports:
          - 4566:4566

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'corretto'

      - name: Run tests
        env:
          AWS_ENDPOINT_URL: http://localhost:4566
          AWS_ACCESS_KEY_ID: test
          AWS_SECRET_ACCESS_KEY: test
          USE_LOCALSTACK: true
        run: ./gradlew test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/test-results/
```

---

## 🧪 Hands-On Lab

**💡 Do you want to try a hands-on lab?**

### Lab 3.1: Performance Optimization Challenge

**Objective:** Upload 1000 small files as fast as possible!

**Your Task:**
1. Create 1000 temporary files (1KB each)
2. Implement 3 upload strategies:
   - Sequential (one at a time)
   - Parallel with 10 workers
   - Parallel with 50 workers
3. Measure and compare performance
4. Find the optimal number of parallel workers

**Starter Code:** `src/main/kotlin/com/awssdk/tutorial/part3/labs/Lab01PerformanceChallenge.kt`

```kotlin
// TODO: Implement this!
fun main() = runBlocking {
    val files = createTestFiles(1000)

    println("Testing sequential upload...")
    val seqTime = measureUpload(files, parallelism = 1)

    println("Testing parallel upload (10 workers)...")
    val par10Time = measureUpload(files, parallelism = 10)

    println("Testing parallel upload (50 workers)...")
    val par50Time = measureUpload(files, parallelism = 50)

    // Print results
}
```

**✨ Senior-Level Challenge:**
- Add rate limiting to avoid overwhelming LocalStack
- Implement a progress bar
- Add retry logic for failed uploads
- Calculate and display throughput (MB/s)

**🔒 Security Best Practice:**
- Never log file contents
- Validate file sizes before upload
- Check for malicious file types

---

## ✅ Checkpoint

Before moving to Part 4, ensure you can:

- [ ] Explain async vs sync clients
- [ ] Implement parallel uploads
- [ ] Use multipart upload for large files
- [ ] Implement retry with exponential backoff
- [ ] Write unit tests with mocking
- [ ] Run integration tests with LocalStack
- [ ] Set up CI/CD pipeline

---

## 🎯 Next Steps

Ready for real-world projects?

**Continue to:** [Part 4: Real-World Projects →](./PART-4-PROJECTS.md)

Build 3 production-ready applications from scratch!

---

**What do you want to learn next?** 🚀
