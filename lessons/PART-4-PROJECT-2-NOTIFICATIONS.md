# 📨 PROJECT 2: Event-Driven Notification System

**Complexity:** ⭐⭐⭐☆☆ (Advanced)
**Time:** 4-6 hours

Build a production-ready event-driven notification system using Lambda, SNS, SQS, and consumer applications.

---

## 🎯 Architecture

```
┌─────────────┐
│  S3 Bucket  │  (File uploaded)
└──────┬──────┘
       │ S3 Event
       ↓
┌──────────────┐
│   Lambda     │  (Event Handler)
│   Function   │
└──────┬───────┘
       │ Publish
       ↓
┌──────────────┐
│  SNS Topic   │  (Fan-out)
│ "user-events"│
└───────┬──────┘
        │
        ├──────────────┬──────────────┬──────────────┐
        ↓              ↓              ↓              ↓
    ┌───────┐      ┌───────┐      ┌───────┐    ┌──────────┐
    │  SQS  │      │  SQS  │      │  SQS  │    │  Email   │
    │ Email │      │  SMS  │      │ Slack │    │ (Direct) │
    └───┬───┘      └───┬───┘      └───┬───┘    └──────────┘
        │              │              │
        ↓              ↓              ↓
    ┌───────┐      ┌───────┐      ┌───────┐
    │ Email │      │  SMS  │      │ Slack │
    │Worker │      │Worker │      │Worker │
    └───────┘      └───────┘      └───────┘
```

---

## 📂 Project Structure

```
project2-event-driven-notifications/
├── lambda/
│   └── event-handler/
│       └── src/main/kotlin/
│           └── EventHandler.kt          # Lambda function
├── consumers/
│   ├── email-worker/
│   │   └── EmailConsumer.kt             # Email worker
│   ├── sms-worker/
│   │   └── SmsConsumer.kt               # SMS worker
│   └── slack-worker/
│       └── SlackConsumer.kt             # Slack worker
├── common/
│   ├── models/
│   │   └── NotificationEvent.kt         # Event model
│   └── NotificationService.kt           # Shared service
├── infrastructure/
│   ├── setup.sh                         # Setup script
│   └── docker-compose.yml
└── README.md
```

---

## 📝 Step 1: Define Event Models

**File:** `common/models/NotificationEvent.kt`

```kotlin
package com.awssdk.projects.notifications.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationEvent(
    val eventId: String,
    val eventType: String,        // FILE_UPLOADED, USER_REGISTERED, etc.
    val timestamp: Long,
    val source: String,            // s3, api, scheduler
    val data: Map<String, String>, // Event-specific data
    val userId: String? = null,
    val priority: Priority = Priority.NORMAL
)

enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

@Serializable
data class S3EventData(
    val bucketName: String,
    val objectKey: String,
    val size: Long,
    val contentType: String?
)

@Serializable
data class EmailNotification(
    val to: String,
    val subject: String,
    val body: String,
    val priority: Priority = Priority.NORMAL
)

@Serializable
data class SmsNotification(
    val phoneNumber: String,
    val message: String
)

@Serializable
data class SlackNotification(
    val channel: String,
    val message: String,
    val username: String = "NotificationBot"
)
```

---

## 📝 Step 2: Lambda Event Handler

**File:** `lambda/event-handler/EventHandler.kt`

```kotlin
package com.awssdk.projects.notifications.lambda

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.MessageAttributeValue
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.awssdk.projects.notifications.models.NotificationEvent
import com.awssdk.projects.notifications.models.Priority
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class EventHandler : RequestHandler<S3Event, String> {

    private val snsTopicArn = System.getenv("SNS_TOPIC_ARN")
    private val json = Json { prettyPrint = false }

    override fun handleRequest(s3Event: S3Event, context: Context): String = runBlocking {
        val logger = context.logger

        logger.log("📥 Processing S3 event with ${s3Event.records.size} record(s)")

        SnsClient { region = "us-east-1" }.use { snsClient ->

            s3Event.records.forEach { record ->
                val s3 = record.s3

                logger.log("Processing: ${s3.bucket.name}/${s3.`object`.key}")

                // Create notification event
                val event = NotificationEvent(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "FILE_UPLOADED",
                    timestamp = System.currentTimeMillis(),
                    source = "s3",
                    data = mapOf(
                        "bucketName" to s3.bucket.name,
                        "objectKey" to s3.`object`.key,
                        "size" to s3.`object`.size.toString(),
                        "eventName" to record.eventName
                    ),
                    priority = determinePriority(s3.`object`.key)
                )

                // Publish to SNS
                val messageJson = json.encodeToString(event)

                snsClient.publish {
                    topicArn = snsTopicArn
                    message = messageJson
                    subject = "File Upload Notification"

                    // Add message attributes for filtering
                    messageAttributes = mapOf(
                        "eventType" to MessageAttributeValue {
                            dataType = "String"
                            stringValue = event.eventType
                        },
                        "priority" to MessageAttributeValue {
                            dataType = "String"
                            stringValue = event.priority.name
                        }
                    )
                }

                logger.log("✅ Published event ${event.eventId} to SNS")
            }
        }

        "Processed ${s3Event.records.size} event(s)"
    }

    private fun determinePriority(objectKey: String): Priority {
        return when {
            objectKey.contains("/urgent/") -> Priority.URGENT
            objectKey.contains("/high/") -> Priority.HIGH
            objectKey.contains("/low/") -> Priority.LOW
            else -> Priority.NORMAL
        }
    }
}
```

---

## 📝 Step 3: Email Consumer Worker

**File:** `consumers/email-worker/EmailConsumer.kt`

```kotlin
package com.awssdk.projects.notifications.consumers

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.awssdk.projects.notifications.models.NotificationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class EmailConsumer(
    private val sqsClient: SqsClient,
    private val queueUrl: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun start() {
        println("📧 Email Consumer started")
        println("   Queue: $queueUrl")
        println("   Polling for messages...\n")

        while (true) {
            try {
                pollAndProcess()
            } catch (e: Exception) {
                println("❌ Error: ${e.message}")
                delay(5000) // Wait before retry
            }
        }
    }

    private suspend fun pollAndProcess() {
        val response = sqsClient.receiveMessage {
            queueUrl = this@EmailConsumer.queueUrl
            maxNumberOfMessages = 10
            waitTimeSeconds = 20 // Long polling
            messageAttributeNames = listOf("All")
        }

        val messages = response.messages
        if (messages.isNullOrEmpty()) {
            return
        }

        println("📬 Received ${messages.size} message(s)")

        messages.forEach { message ->
            try {
                // Parse SNS message
                val snsMessage = json.decodeFromString<SnsMessage>(message.body ?: "")
                val event = json.decodeFromString<NotificationEvent>(snsMessage.Message)

                println("\n📧 Processing Email Notification")
                println("   Event ID: ${event.eventId}")
                println("   Event Type: ${event.eventType}")
                println("   Priority: ${event.priority}")

                // Send email (simulated)
                sendEmail(event)

                // Delete message from queue
                sqsClient.deleteMessage {
                    queueUrl = this@EmailConsumer.queueUrl
                    receiptHandle = message.receiptHandle
                }

                println("   ✅ Email sent and message deleted")

            } catch (e: Exception) {
                println("   ❌ Failed to process message: ${e.message}")
                // Message will return to queue for retry
            }
        }
    }

    private fun sendEmail(event: NotificationEvent) {
        // In real implementation, use AWS SES or SMTP
        println("   📤 Sending email...")
        println("   To: user@example.com")
        println("   Subject: ${event.eventType}")

        val body = buildEmailBody(event)
        println("   Body:\n$body")

        // Simulate email sending delay
        Thread.sleep(100)
    }

    private fun buildEmailBody(event: NotificationEvent): String {
        return when (event.eventType) {
            "FILE_UPLOADED" -> """
                Hello,

                A new file has been uploaded to your bucket:

                Bucket: ${event.data["bucketName"]}
                File: ${event.data["objectKey"]}
                Size: ${event.data["size"]} bytes

                Timestamp: ${event.timestamp}

                Best regards,
                File Manager Team
            """.trimIndent()

            else -> """
                Event Type: ${event.eventType}
                Event ID: ${event.eventId}
                Data: ${event.data}
            """.trimIndent()
        }
    }
}

@kotlinx.serialization.Serializable
data class SnsMessage(
    val Type: String,
    val MessageId: String,
    val TopicArn: String,
    val Message: String,
    val Timestamp: String
)

fun main() = runBlocking {
    val queueUrl = System.getenv("EMAIL_QUEUE_URL")
        ?: "http://localstack:4566/000000000000/email-notifications"

    SqsClient {
        region = "us-east-1"
        endpointUrl = aws.smithy.kotlin.runtime.net.url.Url.parse(
            System.getenv("AWS_ENDPOINT_URL") ?: "http://localstack:4566"
        )
    }.use { sqsClient ->
        val consumer = EmailConsumer(sqsClient, queueUrl)
        consumer.start()
    }
}
```

---

## 📝 Step 4: SMS & Slack Consumers (Similar Pattern)

**File:** `consumers/sms-worker/SmsConsumer.kt`

```kotlin
package com.awssdk.projects.notifications.consumers

import aws.sdk.kotlin.services.sqs.SqsClient
import com.awssdk.projects.notifications.models.NotificationEvent
import kotlinx.coroutines.runBlocking

class SmsConsumer(
    private val sqsClient: SqsClient,
    private val queueUrl: String
) {

    suspend fun start() {
        println("📱 SMS Consumer started")
        println("   Queue: $queueUrl")

        // Similar implementation to EmailConsumer
        // but sends SMS via AWS SNS SMS or Twilio
    }

    private fun sendSms(event: NotificationEvent) {
        println("   📤 Sending SMS...")
        println("   To: +1234567890")
        println("   Message: File ${event.data["objectKey"]} uploaded")
        // Use AWS SNS for SMS or Twilio API
    }
}

fun main() = runBlocking {
    val queueUrl = System.getenv("SMS_QUEUE_URL")
        ?: "http://localstack:4566/000000000000/sms-notifications"

    SqsClient {
        region = "us-east-1"
    }.use { sqsClient ->
        val consumer = SmsConsumer(sqsClient, queueUrl)
        consumer.start()
    }
}
```

**File:** `consumers/slack-worker/SlackConsumer.kt`

```kotlin
package com.awssdk.projects.notifications.consumers

import aws.sdk.kotlin.services.sqs.SqsClient
import com.awssdk.projects.notifications.models.NotificationEvent
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SlackConsumer(
    private val sqsClient: SqsClient,
    private val queueUrl: String,
    private val slackWebhookUrl: String
) {

    private val httpClient = HttpClient.newHttpClient()

    suspend fun start() {
        println("💬 Slack Consumer started")
        println("   Queue: $queueUrl")

        // Similar implementation to EmailConsumer
    }

    private fun sendSlackMessage(event: NotificationEvent) {
        val payload = """
            {
                "text": "📁 New File Uploaded",
                "blocks": [
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "*File Upload Notification*\n\nBucket: `${event.data["bucketName"]}`\nFile: `${event.data["objectKey"]}`\nSize: ${event.data["size"]} bytes"
                        }
                    }
                ]
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(slackWebhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) {
            println("   ✅ Slack message sent")
        } else {
            println("   ❌ Slack message failed: ${response.statusCode()}")
        }
    }
}
```

---

## 📝 Step 5: Infrastructure Setup

**File:** `infrastructure/setup.sh`

```bash
#!/bin/bash

set -e

echo "🚀 Setting up Event-Driven Notification System"
echo "=" | head -c 60 | tr '\0' '='
echo ""

ENDPOINT="http://localstack:4566"

# Create SNS Topic
echo "📣 Creating SNS topic..."
TOPIC_ARN=$(aws --endpoint-url=$ENDPOINT sns create-topic \
    --name user-events \
    --output text --query 'TopicArn')
echo "✅ Topic ARN: $TOPIC_ARN"

# Create SQS Queues
echo ""
echo "📬 Creating SQS queues..."

# Email queue
EMAIL_QUEUE_URL=$(aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name email-notifications \
    --output text --query 'QueueUrl')
echo "✅ Email Queue: $EMAIL_QUEUE_URL"

# SMS queue
SMS_QUEUE_URL=$(aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name sms-notifications \
    --output text --query 'QueueUrl')
echo "✅ SMS Queue: $SMS_QUEUE_URL"

# Slack queue
SLACK_QUEUE_URL=$(aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name slack-notifications \
    --output text --query 'QueueUrl')
echo "✅ Slack Queue: $SLACK_QUEUE_URL"

# Dead Letter Queue
DLQ_URL=$(aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name notifications-dlq \
    --output text --query 'QueueUrl')
echo "✅ Dead Letter Queue: $DLQ_URL"

# Subscribe queues to SNS topic
echo ""
echo "🔗 Subscribing queues to SNS topic..."

aws --endpoint-url=$ENDPOINT sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $EMAIL_QUEUE_URL

aws --endpoint-url=$ENDPOINT sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $SMS_QUEUE_URL

aws --endpoint-url=$ENDPOINT sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol sqs \
    --notification-endpoint $SLACK_QUEUE_URL

echo "✅ Subscriptions created"

# Create S3 bucket for testing
echo ""
echo "📦 Creating S3 bucket..."
aws --endpoint-url=$ENDPOINT s3 mb s3://notification-test-bucket
echo "✅ Bucket created"

echo ""
echo "=" | head -c 60 | tr '\0' '='
echo ""
echo "✅ Setup complete!"
echo ""
echo "Environment variables to use:"
echo "  SNS_TOPIC_ARN=$TOPIC_ARN"
echo "  EMAIL_QUEUE_URL=$EMAIL_QUEUE_URL"
echo "  SMS_QUEUE_URL=$SMS_QUEUE_URL"
echo "  SLACK_QUEUE_URL=$SLACK_QUEUE_URL"
echo "  DLQ_URL=$DLQ_URL"
```

---

## 📝 Step 6: Docker Compose

**File:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3,sns,sqs,lambda
      - DEBUG=1
    volumes:
      - "./localstack-data:/tmp/localstack"

  email-worker:
    build: ./consumers/email-worker
    environment:
      - AWS_ENDPOINT_URL=http://localstack:4566
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - EMAIL_QUEUE_URL=http://localstack:4566/000000000000/email-notifications
    depends_on:
      - localstack
    restart: unless-stopped

  sms-worker:
    build: ./consumers/sms-worker
    environment:
      - AWS_ENDPOINT_URL=http://localstack:4566
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - SMS_QUEUE_URL=http://localstack:4566/000000000000/sms-notifications
    depends_on:
      - localstack
    restart: unless-stopped

  slack-worker:
    build: ./consumers/slack-worker
    environment:
      - AWS_ENDPOINT_URL=http://localstack:4566
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - SLACK_QUEUE_URL=http://localstack:4566/000000000000/slack-notifications
      - SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
    depends_on:
      - localstack
    restart: unless-stopped
```

---

## 🧪 Testing the System

### Step 1: Start Services

```bash
docker-compose up -d
sleep 10
```

### Step 2: Run Setup Script

```bash
docker-compose exec localstack sh /app/infrastructure/setup.sh
```

### Step 3: Test Event Flow

```bash
# Upload file to S3 (triggers Lambda → SNS → SQS)
aws --endpoint-url=http://localhost:4566 s3 cp test.txt s3://notification-test-bucket/urgent/test.txt

# Watch consumer logs
docker-compose logs -f email-worker
docker-compose logs -f sms-worker
docker-compose logs -f slack-worker
```

**Expected Output:**

```
email-worker    | 📬 Received 1 message(s)
email-worker    |
email-worker    | 📧 Processing Email Notification
email-worker    |    Event ID: abc-123-def
email-worker    |    Event Type: FILE_UPLOADED
email-worker    |    Priority: URGENT
email-worker    |    📤 Sending email...
email-worker    |    To: user@example.com
email-worker    |    Subject: FILE_UPLOADED
email-worker    |    ✅ Email sent and message deleted
```

---

## 💡 Do you want to try a hands-on lab?

### Lab 4.2: Extend the Notification System

**Your Tasks:**
1. Add filtering: Only send SMS for URGENT events
2. Implement retry logic with exponential backoff
3. Add metrics tracking (count of notifications sent)
4. Create a notification history in DynamoDB
5. Implement notification preferences per user

**✨ Senior-Level Challenge:**
- Add circuit breaker pattern for external services
- Implement batching for cost optimization
- Add A/B testing for notification templates
- Create a dashboard showing notification metrics

**🔒 Security Best Practice:**
- Encrypt sensitive data in messages
- Implement message validation
- Add rate limiting per user
- Audit all notification sends

---

**Next:** [Project 3: Serverless AI API with Bedrock →](./PART-4-PROJECT-3-AI-API.md)
