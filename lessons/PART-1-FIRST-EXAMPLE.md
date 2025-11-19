# 🚀 Your First AWS SDK Example: List S3 Buckets

Welcome to your first hands-on AWS SDK Kotlin example! Let's write code that actually talks to AWS (LocalStack).

---

## 🎯 What You'll Learn

✅ Create an S3 client using AWS SDK for Kotlin
✅ Configure endpoint for LocalStack
✅ Handle coroutines properly
✅ List S3 buckets
✅ Handle errors gracefully
✅ Use proper resource management

---

## 📝 The Code

Let's break down the example step by step.

### Step 1: Create AWS Client Factory (Reusable Utility)

**File:** `src/main/kotlin/com/awssdk/tutorial/common/AwsClientFactory.kt`

```kotlin
package com.awssdk.tutorial.common

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url

object AwsClientFactory {
    /**
     * Creates an S3 client configured for LocalStack or AWS
     *
     * @param useLocalStack If true, uses LocalStack endpoint
     * @return Configured S3Client
     */
    fun createS3Client(useLocalStack: Boolean = true): S3Client {
        return S3Client {
            region = System.getenv("AWS_REGION") ?: "us-east-1"

            if (useLocalStack) {
                // LocalStack configuration
                endpointUrl = Url.parse(
                    System.getenv("AWS_ENDPOINT_URL") ?: "http://localstack:4566"
                )

                // LocalStack uses static test credentials
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }

                // Disable S3 path-style for LocalStack
                forcePathStyle = true
            }
            // If not LocalStack, SDK will use default credential chain
        }
    }

    /**
     * Gets the endpoint URL (useful for debugging)
     */
    fun getEndpointUrl(): String {
        return System.getenv("AWS_ENDPOINT_URL") ?: "http://localhost:4566"
    }
}
```

**Key Concepts:**

1. **Object Declaration** - Singleton pattern for factory
2. **StaticCredentialsProvider** - For LocalStack test credentials
3. **endpointUrl** - Points to LocalStack instead of AWS
4. **forcePathStyle** - LocalStack uses path-style URLs (bucket-name.s3.amazonaws.com vs s3.amazonaws.com/bucket-name)
5. **Default Credential Chain** - When not using LocalStack, SDK finds credentials automatically

---

### Step 2: List S3 Buckets

**File:** `src/main/kotlin/com/awssdk/tutorial/part1/ListS3Buckets.kt`

```kotlin
package com.awssdk.tutorial.part1

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.S3Exception
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

/**
 * Example 1: List all S3 buckets
 *
 * This demonstrates:
 * - Creating an S3 client
 * - Making an async API call
 * - Proper resource management with use {}
 * - Error handling
 */
fun main() = runBlocking {
    println("🪣 AWS SDK Kotlin - List S3 Buckets Example")
    println("=" .repeat(50))

    try {
        // Create S3 client (will auto-close after use block)
        AwsClientFactory.createS3Client().use { s3Client ->

            println("\n📡 Connecting to: ${AwsClientFactory.getEndpointUrl()}")
            println("🔍 Listing S3 buckets...\n")

            // Call AWS API - listBuckets is a suspend function
            val response = s3Client.listBuckets()

            // Check if we have buckets
            val buckets = response.buckets

            if (buckets.isNullOrEmpty()) {
                println("📭 No buckets found")
                println("\n💡 TIP: Create a bucket first!")
                println("   Run: CreateS3Bucket.kt")
            } else {
                println("✅ Found ${buckets.size} bucket(s):\n")

                buckets.forEachIndexed { index, bucket ->
                    println("${index + 1}. ${bucket.name}")
                    println("   Created: ${bucket.creationDate}")
                    println()
                }
            }
        }

        println("✅ Success!")

    } catch (e: S3Exception) {
        // AWS S3-specific errors
        println("❌ S3 Error: ${e.message}")
        println("   Error Code: ${e.sdkErrorMetadata.errorCode}")
        println("   Status Code: ${e.sdkErrorMetadata.errorCode}")
    } catch (e: Exception) {
        // General errors (network, etc.)
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
```

**Key Concepts:**

1. **runBlocking** - Starts a coroutine scope for suspend functions
2. **.use { }** - Kotlin's try-with-resources (auto-closes client)
3. **suspend function** - `listBuckets()` is async, doesn't block thread
4. **S3Exception** - Specific AWS S3 errors
5. **Error handling** - Proper exception hierarchy

---

## 🏃 Running the Example

### Method 1: Inside Docker Container

```bash
# Start the environment
docker-compose up -d

# Wait for LocalStack to be ready (about 10 seconds)
sleep 10

# Enter Kotlin container
docker-compose exec kotlin-app bash

# Run the example
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# Exit container
exit
```

### Method 2: Using Gradle Wrapper

```bash
# From your host machine (if Gradle is installed)
./gradlew run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

---

## 📊 Expected Output

### If no buckets exist:

```
🪣 AWS SDK Kotlin - List S3 Buckets Example
==================================================

📡 Connecting to: http://localstack:4566
🔍 Listing S3 buckets...

📭 No buckets found

💡 TIP: Create a bucket first!
   Run: CreateS3Bucket.kt

✅ Success!
```

### If buckets exist:

```
🪣 AWS SDK Kotlin - List S3 Buckets Example
==================================================

📡 Connecting to: http://localstack:4566
🔍 Listing S3 buckets...

✅ Found 3 bucket(s):

1. my-first-bucket
   Created: 2025-11-19T10:30:00Z

2. dev-uploads
   Created: 2025-11-19T10:31:15Z

3. test-data
   Created: 2025-11-19T10:32:00Z

✅ Success!
```

---

## 🔧 Debugging Tips

### Issue: "Connection refused"

**Problem:** LocalStack isn't running

**Solution:**
```bash
# Check if LocalStack is running
docker-compose ps

# Start it if not running
docker-compose up -d localstack

# Check logs
docker-compose logs localstack
```

### Issue: "No credentials found"

**Problem:** Environment variables not set

**Solution:**
Check the docker-compose.yml - credentials are set automatically via environment variables.

### Issue: "Unknown host: localstack"

**Problem:** Running outside Docker network

**Solution:**
- Either run inside `kotlin-app` container
- Or change endpoint to `http://localhost:4566`

---

## 🧪 Experiment Time!

Try these modifications to learn more:

### Experiment 1: Create a Bucket First

```bash
# Using AWS CLI in docker
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://my-test-bucket

# Now run ListS3Buckets again
```

### Experiment 2: Use Real AWS

Modify `AwsClientFactory.kt`:

```kotlin
fun createS3Client(useLocalStack: Boolean = false): S3Client { // Change to false
    // ...
}
```

**⚠️ Warning:** This will use real AWS and your real credentials!

### Experiment 3: Add More Details

Modify the listing code to show more information:

```kotlin
buckets.forEach { bucket ->
    println("Bucket: ${bucket.name}")

    // Get bucket location
    val location = s3Client.getBucketLocation {
        this.bucket = bucket.name
    }
    println("  Region: ${location.locationConstraint}")
}
```

---

## 🧠 Deep Dive: How Does This Work?

### 1. Client Creation

```kotlin
S3Client {
    region = "us-east-1"
    endpointUrl = Url.parse("http://localstack:4566")
    credentialsProvider = StaticCredentialsProvider { ... }
}
```

This creates a configured HTTP client that:
- Knows which AWS region to use
- Knows where to send requests (LocalStack)
- Knows how to authenticate (credentials)

### 2. API Call

```kotlin
val response = s3Client.listBuckets()
```

Behind the scenes:
1. SDK creates HTTP request: `GET /`
2. Signs request with SigV4 using credentials
3. Sends to `http://localstack:4566`
4. Parses XML/JSON response
5. Returns type-safe Kotlin object

### 3. Resource Management

```kotlin
s3Client.use { ... }
```

Ensures:
- HTTP connections are closed
- Memory is freed
- No connection leaks

---

## 📚 Related Concepts

### Coroutines vs Callbacks

**Old Style (Java SDK v1):**
```java
// Blocking call - freezes thread
ListBucketsResponse response = s3.listBuckets();
```

**Kotlin SDK:**
```kotlin
// Suspend function - releases thread while waiting
val response = s3Client.listBuckets()
```

### Credentials Provider Chain

When you don't specify credentials, AWS SDK checks:

1. ✅ Environment variables (`AWS_ACCESS_KEY_ID`, etc.)
2. ✅ System properties
3. ✅ Credentials file (`~/.aws/credentials`)
4. ✅ ECS container credentials
5. ✅ EC2 instance profile credentials

---

## ✅ Checkpoint

Before moving forward, make sure you can:

- [ ] Start the Docker environment
- [ ] Run the ListS3Buckets example
- [ ] Understand what .use { } does
- [ ] Create a bucket using AWS CLI
- [ ] Explain the difference between suspend functions and blocking calls
- [ ] Know where credentials come from

---

## 🎯 Next Steps

Ready for more? Choose your path:

1. **[Create S3 Bucket Example →](./PART-1-CREATE-BUCKET.md)** - Learn to create buckets
2. **[Part 2: S3 Deep Dive →](./PART-2-S3.md)** - Upload, download, signed URLs
3. **[Part 2: DynamoDB →](./PART-2-DYNAMODB.md)** - NoSQL database operations

---

## 🧪 Mini Quiz

### Question 1
What does the `.use { }` block do?
- A) Catches exceptions
- B) Automatically closes resources
- C) Makes code run faster
- D) Enables logging

<details>
<summary>Answer</summary>

**B) Automatically closes resources**

`.use { }` is Kotlin's equivalent to try-with-resources in Java. It ensures the client is properly closed even if an exception occurs.
</details>

### Question 2
Why do we use `suspend` functions in AWS SDK for Kotlin?
- A) To make code more secure
- B) To enable async/non-blocking operations
- C) To reduce memory usage
- D) To add authentication

<details>
<summary>Answer</summary>

**B) To enable async/non-blocking operations**

Suspend functions allow the thread to be released while waiting for I/O operations, making your application more efficient.
</details>

---

**What would you like to do next?** 🚀

Tell me and I'll guide you there!
