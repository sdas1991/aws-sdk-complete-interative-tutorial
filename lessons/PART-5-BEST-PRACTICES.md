# 🎯 PART 5 — Expert Best Practices

Master production-ready patterns and advanced techniques for building enterprise-grade AWS SDK applications.

---

## 🎯 Learning Objectives

✅ SDK client lifecycle management (singleton vs recreate)
✅ VPC endpoints for secure private connectivity
✅ Multi-account management with AWS Profiles
✅ Disaster recovery and credential failure handling
✅ Logging & tracing with AWS X-Ray
✅ Building SDK wrappers for large systems

---

## 📚 Lesson 5.1: SDK Client Lifecycle Management

### The Problem: Creating Too Many Clients

```kotlin
// ❌ ANTI-PATTERN - Creates new HTTP connection pool every time!
fun uploadFile(file: File) {
    S3Client { region = "us-east-1" }.use { s3 ->
        s3.putObject { /* ... */ }
    }  // Connection pool destroyed!
}

// Called 1000 times = 1000 connection pools created/destroyed!
```

### The Solution: Singleton Pattern

```kotlin
// ✅ BEST PRACTICE - Reuse client across requests
object AwsClients {
    val s3Client: S3Client by lazy {
        S3Client {
            region = "us-east-1"
            retryStrategy = StandardRetryStrategy { maxAttempts = 5 }
            httpClient = CrtHttpEngineConfig {
                maxConnections = 50
                connectionIdleTimeout = 60.seconds
            }
        }
    }

    val dynamoDbClient: DynamoDbClient by lazy {
        DynamoDbClient { region = "us-east-1" }
    }

    fun shutdown() {
        s3Client.close()
        dynamoDbClient.close()
    }
}

// Usage
suspend fun uploadFile(file: File) {
    AwsClients.s3Client.putObject { /* ... */ }
}
```

### When to Recreate Clients

**Recreate when:**
- Cross-region operations (different regions)
- Credential rotation (new IAM credentials)
- Configuration changes (different retry strategy)

**Don't recreate for:**
- Every request (huge performance hit!)
- Different operations on same service
- Thread-per-request models

---

## 📚 Lesson 5.2: VPC Endpoints

### Why VPC Endpoints?

**Without VPC Endpoint:**
```
EC2 Instance → Internet Gateway → AWS Service
  (Public internet - insecure, costs $$)
```

**With VPC Endpoint:**
```
EC2 Instance → VPC Endpoint → AWS Service
  (Private AWS network - secure, free!)
```

### Setup VPC Endpoint (AWS Console)

```
1. Go to VPC → Endpoints → Create Endpoint
2. Service: com.amazonaws.us-east-1.s3 (Gateway endpoint - FREE!)
3. VPC: Select your VPC
4. Route Tables: Select route tables
5. Policy: Full Access or custom
6. Create Endpoint
```

### Using VPC Endpoints in Code

**Good news:** No code changes needed! SDK automatically uses VPC endpoints when available.

```kotlin
// This automatically uses VPC endpoint if configured in VPC
val s3Client = S3Client { region = "us-east-1" }
```

### Verification

```bash
# Check if using VPC endpoint (from EC2)
aws s3 ls --debug 2>&1 | grep endpoint

# Should show private endpoint, not public
```

---

## 📚 Lesson 5.3: Multi-Account Management

### AWS Profiles Setup

**~/.aws/credentials:**
```ini
[dev-account]
aws_access_key_id = AKIADEV...
aws_secret_access_key = SECRET_DEV...

[prod-account]
aws_access_key_id = AKIAPROD...
aws_secret_access_key = SECRET_PROD...

[staging-account]
role_arn = arn:aws:iam::222222222222:role/StagingRole
source_profile = dev-account
```

### Using Profiles in Code

```kotlin
class MultiAccountAwsClientFactory(private val profile: String) {

    fun createS3Client(): S3Client {
        return S3Client {
            region = "us-east-1"

            credentialsProvider = ProfileCredentialsProvider {
                profileName = profile
            }
        }
    }
}

// Usage
val devS3 = MultiAccountAwsClientFactory("dev-account").createS3Client()
val prodS3 = MultiAccountAwsClientFactory("prod-account").createS3Client()
```

### Cross-Account Access with AssumeRole

```kotlin
suspend fun assumeRoleAndAccess(roleArn: String, sessionName: String) {
    val stsClient = StsClient { region = "us-east-1" }

    val assumeRoleResponse = stsClient.assumeRole {
        this.roleArn = roleArn
        roleSessionName = sessionName
        durationSeconds = 3600 // 1 hour
    }

    val credentials = assumeRoleResponse.credentials!!

    // Use temporary credentials
    val s3Client = S3Client {
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = credentials.accessKeyId
            secretAccessKey = credentials.secretAccessKey
            sessionToken = credentials.sessionToken
        }
    }

    s3Client.use {
        // Access cross-account resources
        it.listBuckets()
    }
}
```

---

## 📚 Lesson 5.4: Disaster Recovery & Credential Failures

### Handling Credential Failures

```kotlin
class ResilientAwsClient {
    private var s3Client: S3Client? = null
    private var lastCredentialRefresh = 0L

    suspend fun getS3Client(): S3Client {
        val now = System.currentTimeMillis()

        // Refresh credentials every 50 minutes (AWS temp creds last 1 hour)
        if (s3Client == null || (now - lastCredentialRefresh) > 50 * 60 * 1000) {
            s3Client?.close()

            s3Client = try {
                createS3ClientWithRetry()
            } catch (e: Exception) {
                // Fallback to instance profile
                S3Client { region = "us-east-1" }
            }

            lastCredentialRefresh = now
        }

        return s3Client!!
    }

    private suspend fun createS3ClientWithRetry(): S3Client {
        repeat(3) { attempt ->
            try {
                return S3Client {
                    region = "us-east-1"
                    credentialsProvider = DefaultChainCredentialsProvider()
                }
            } catch (e: Exception) {
                if (attempt == 2) throw e
                delay(2000L * (attempt + 1))
            }
        }
        throw IllegalStateException("Failed to create S3Client")
    }
}
```

### Circuit Breaker Pattern

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Long = 60000
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED

    enum class State { CLOSED, OPEN, HALF_OPEN }

    suspend fun <T> execute(block: suspend () -> T): T {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                    state = State.HALF_OPEN
                } else {
                    throw Exception("Circuit breaker is OPEN")
                }
            }
            State.HALF_OPEN -> {
                // Try one request
            }
            State.CLOSED -> {
                // Normal operation
            }
        }

        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }

    private fun onSuccess() {
        failureCount = 0
        state = State.CLOSED
    }

    private fun onFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()

        if (failureCount >= failureThreshold) {
            state = State.OPEN
            println("⚠️  Circuit breaker opened!")
        }
    }
}

// Usage
val circuitBreaker = CircuitBreaker()

suspend fun uploadWithCircuitBreaker(file: File) {
    circuitBreaker.execute {
        s3Client.putObject {
            bucket = "my-bucket"
            key = file.name
            body = ByteStream.fromFile(file)
        }
    }
}
```

---

## 📚 Lesson 5.5: Logging & Tracing with AWS X-Ray

### Enable X-Ray Tracing

**build.gradle.kts:**
```kotlin
dependencies {
    implementation("aws.sdk.kotlin:xray:1.0.57")
    implementation("com.amazonaws:aws-xray-recorder-sdk-core:2.14.0")
    implementation("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2:2.14.0")
}
```

### X-Ray Configuration

```kotlin
import com.amazonaws.xray.AWSXRay
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy

// Configure X-Ray
fun configureXRay() {
    // Sample 10% of requests
    val samplingRules = """
        {
          "version": 2,
          "rules": [],
          "default": {
            "fixed_target": 1,
            "rate": 0.1
          }
        }
    """.trimIndent()

    AWSXRay.setGlobalRecorder(
        AWSXRayRecorderBuilder.standard()
            .withSamplingStrategy(LocalizedSamplingStrategy(URL(samplingRules)))
            .build()
    )
}

// Create traced segments
suspend fun tracedS3Upload(file: File) {
    AWSXRay.beginSegment("S3Upload")

    try {
        AWSXRay.beginSubsegment("FileValidation")
        validateFile(file)
        AWSXRay.endSubsegment()

        AWSXRay.beginSubsegment("S3PutObject")
        s3Client.putObject {
            bucket = "my-bucket"
            key = file.name
            body = ByteStream.fromFile(file)
        }
        AWSXRay.endSubsegment()

        AWSXRay.getCurrentSegment().putAnnotation("fileSize", file.length())
        AWSXRay.getCurrentSegment().putMetadata("fileName", file.name)

    } finally {
        AWSXRay.endSegment()
    }
}
```

### Structured Logging

```kotlin
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class StructuredLogger(private val component: String) {
    private val logger = LoggerFactory.getLogger(component)

    suspend fun logS3Operation(
        operation: String,
        bucket: String,
        key: String,
        block: suspend () -> Unit
    ) {
        val requestId = UUID.randomUUID().toString()

        MDC.put("requestId", requestId)
        MDC.put("operation", operation)
        MDC.put("bucket", bucket)
        MDC.put("key", key)

        val startTime = System.currentTimeMillis()

        try {
            logger.info("Starting S3 operation")
            block()
            val duration = System.currentTimeMillis() - startTime
            MDC.put("duration", duration.toString())
            logger.info("S3 operation completed successfully")
        } catch (e: Exception) {
            logger.error("S3 operation failed", e)
            throw e
        } finally {
            MDC.clear()
        }
    }
}
```

---

## 📚 Lesson 5.6: Building SDK Wrappers for Large Systems

### Wrapper Pattern

```kotlin
interface StorageService {
    suspend fun upload(file: File, path: String): UploadResult
    suspend fun download(path: String, destination: File): DownloadResult
    suspend fun delete(path: String): Boolean
    suspend fun list(prefix: String): List<StorageObject>
}

class S3StorageService(
    private val s3Client: S3Client,
    private val bucketName: String,
    private val metrics: MetricsService,
    private val logger: StructuredLogger
) : StorageService {

    override suspend fun upload(file: File, path: String): UploadResult {
        return metrics.recordMetric("s3.upload") {
            logger.logS3Operation("upload", bucketName, path) {
                val response = s3Client.putObject {
                    bucket = bucketName
                    key = path
                    body = ByteStream.fromFile(file)
                }

                UploadResult(
                    success = true,
                    path = path,
                    etag = response.eTag
                )
            }
        }
    }

    // Implement other methods...
}

// Usage - easy to test and swap implementations
class FileProcessor(private val storage: StorageService) {
    suspend fun processFile(file: File) {
        val result = storage.upload(file, "uploads/${file.name}")
        // Business logic
    }
}
```

---

## 💡 Do you want to try a hands-on lab?

### Lab 5.1: Build a Production-Ready Wrapper

**Objective:** Create a production-ready S3 wrapper with all best practices.

**Requirements:**
1. Singleton client management
2. Circuit breaker for failures
3. Retry with exponential backoff
4. Structured logging
5. X-Ray tracing
6. Metrics collection
7. Multi-account support

**✨ Senior-Level Challenge:**
- Add request deduplication
- Implement caching layer
- Add request throttling
- Create health check endpoint
- Implement graceful shutdown

---

## ✅ Checkpoint

- [ ] Can implement singleton client pattern
- [ ] Understand VPC endpoints benefits
- [ ] Can configure multi-account access
- [ ] Implemented circuit breaker
- [ ] Set up X-Ray tracing
- [ ] Built production-ready wrapper

---

**Next:** [Part 6: Mastery Quiz & Capstone →](./PART-6-MASTERY.md)

Test your knowledge and build your final project!
