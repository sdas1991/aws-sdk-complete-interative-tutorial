# 📨 PART 2 — SQS & SNS (Messaging Services)

Master AWS messaging with **SQS (Simple Queue Service)** and **SNS (Simple Notification Service)**!

---

## 🎯 Learning Objectives

✅ Send messages to SQS queues
✅ Receive and delete messages
✅ Understand long polling
✅ Use FIFO queues
✅ Handle dead-letter queues (DLQ)
✅ Publish to SNS topics
✅ Subscribe to topics
✅ Build project: **Email Notification System**

---

## 📚 Lesson 2.3.1: SQS Basics

### What is SQS?

**Amazon SQS** is a fully managed message queuing service:
- **Decouples** components
- **Scalable** - Unlimited throughput
- **Reliable** - Message persistence
- **Flexible** - Standard or FIFO queues

### Architecture Pattern

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Producer   │ ───> │  SQS Queue  │ ───> │  Consumer   │
│  Service    │      │             │      │  Service    │
└─────────────┘      └─────────────┘      └─────────────┘

Benefits:
✅ Producer doesn't wait for consumer
✅ Consumer processes at own pace
✅ Automatic retry on failure
✅ Messages persist until deleted
```

### Queue Types

| Type | Ordering | Throughput | Duplicates |
|------|----------|------------|------------|
| **Standard** | Best-effort | Unlimited | Possible |
| **FIFO** | Guaranteed | 300 msg/s | Exactly-once |

---

## 🏃 Exercise 1: Create SQS Queue

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/sqs/CreateQueue.kt`

```kotlin
package com.awssdk.tutorial.part2.sqs

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val queueName = "my-kotlin-queue"

    println("📬 Creating SQS Queue")
    println("=".repeat(50))

    try {
        AwsClientFactory.createSqsClient().use { sqsClient ->

            // Create queue
            val response = sqsClient.createQueue {
                this.queueName = queueName

                // Optional attributes
                attributes = mapOf(
                    "DelaySeconds" to "0",
                    "MessageRetentionPeriod" to "345600",  // 4 days
                    "VisibilityTimeout" to "30"
                )
            }

            println("✅ Queue created successfully!")
            println("   Queue URL: ${response.queueUrl}")
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 2: Send Message

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/sqs/SendMessage.kt`

```kotlin
package com.awssdk.tutorial.part2.sqs

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun main() = runBlocking {
    val queueUrl = "http://localstack:4566/000000000000/my-kotlin-queue"

    println("📤 Sending message to SQS")
    println("=".repeat(50))

    try {
        AwsClientFactory.createSqsClient().use { sqsClient ->

            // Create message body
            val messageBody = buildJsonObject {
                put("eventType", "UserRegistered")
                put("userId", "user-12345")
                put("email", "alice@example.com")
                put("timestamp", System.currentTimeMillis())
            }.toString()

            // Send message
            val response = sqsClient.sendMessage {
                queueUrl = queueUrl
                messageBody = messageBody

                // Optional: message attributes
                messageAttributes = mapOf(
                    "Priority" to {
                        stringValue = "High"
                        dataType = "String"
                    }
                )

                // Optional: delay delivery
                delaySeconds = 0
            }

            println("✅ Message sent successfully!")
            println("   Message ID: ${response.messageId}")
            println("   Body: $messageBody")
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 3: Receive Message (Long Polling)

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/sqs/ReceiveMessage.kt`

```kotlin
package com.awssdk.tutorial.part2.sqs

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val queueUrl = "http://localstack:4566/000000000000/my-kotlin-queue"

    println("📥 Receiving messages from SQS")
    println("=".repeat(50))

    try {
        AwsClientFactory.createSqsClient().use { sqsClient ->

            println("🔍 Polling for messages (waiting up to 20 seconds)...\n")

            val response = sqsClient.receiveMessage {
                queueUrl = queueUrl
                maxNumberOfMessages = 10
                waitTimeSeconds = 20  // Long polling
                messageAttributeNames = listOf("All")
            }

            val messages = response.messages

            if (messages.isNullOrEmpty()) {
                println("📭 No messages received")
            } else {
                println("✅ Received ${messages.size} message(s):\n")

                messages.forEachIndexed { index, message ->
                    println("Message ${index + 1}:")
                    println("   ID: ${message.messageId}")
                    println("   Body: ${message.body}")
                    println("   Receipt Handle: ${message.receiptHandle?.take(20)}...")

                    // Delete message after processing
                    sqsClient.deleteMessage {
                        queueUrl = queueUrl
                        receiptHandle = message.receiptHandle
                    }
                    println("   ✅ Message deleted")
                    println()
                }
            }
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

**Key Concept: Long Polling**
- `waitTimeSeconds = 20` - Wait up to 20 seconds for messages
- Reduces empty responses and costs
- More efficient than short polling

---

## 📚 Lesson 2.3.2: SNS Basics

### What is SNS?

**Amazon SNS** is a pub/sub messaging service:
- **Publishers** send messages to topics
- **Subscribers** receive messages
- **Fan-out** pattern - one message to many subscribers

### Architecture

```
                    ┌──────────────┐
                    │  SNS Topic   │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌─────▼─────┐    ┌─────▼─────┐
    │  Email  │      │    SQS    │    │  Lambda   │
    │Subscriber│      │  Queue    │    │ Function  │
    └─────────┘      └───────────┘    └───────────┘
```

---

## 🏃 Exercise 4: Create SNS Topic & Publish

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/sns/PublishToTopic.kt`

```kotlin
package com.awssdk.tutorial.part2.sns

import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val topicName = "user-events"

    println("📣 Publishing to SNS Topic")
    println("=".repeat(50))

    try {
        AwsClientFactory.createSnsClient().use { snsClient ->

            // Create topic
            val createResponse = snsClient.createTopic {
                name = topicName
            }

            val topicArn = createResponse.topicArn
            println("✅ Topic created: $topicArn\n")

            // Publish message
            val publishResponse = snsClient.publish {
                this.topicArn = topicArn
                message = "Hello from AWS SDK for Kotlin!"
                subject = "Test Notification"

                // Optional: message attributes
                messageAttributes = mapOf(
                    "eventType" to {
                        stringValue = "TestEvent"
                        dataType = "String"
                    }
                )
            }

            println("📤 Message published!")
            println("   Message ID: ${publishResponse.messageId}")
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🎓 Mini Project: Email Notification System

Build a notification system using SNS + SQS:

```
User Action → SNS Topic → Multiple Subscribers
                            ├─ Email Service (SQS)
                            ├─ SMS Service (SQS)
                            └─ Push Notification (SQS)
```

**Features:**
✅ Publish events to SNS
✅ Multiple SQS queues subscribe
✅ Each service processes independently
✅ Dead-letter queue for failures

Check `part2/messaging/project/` for implementation!

---

## 🧪 Quiz

### Question 1
What's the main difference between SQS and SNS?
- A) No difference
- B) SQS is queue (pull), SNS is pub/sub (push)
- C) SNS is more expensive
- D) SQS is faster

<details>
<summary>Answer</summary>

**B) SQS is queue (pull), SNS is pub/sub (push)**

SQS is a message queue where consumers pull messages. SNS is pub/sub where publishers push to topics and subscribers receive messages.
</details>

### Question 2
What is long polling in SQS?
- A) Polling every second
- B) Waiting up to 20 seconds for messages before returning
- C) Polling multiple queues
- D) Slow message processing

<details>
<summary>Answer</summary>

**B) Waiting up to 20 seconds for messages before returning**

Long polling reduces empty responses and API costs by waiting for messages to arrive.
</details>

---

## ✅ Checkpoint

Can you:

- [ ] Create an SQS queue
- [ ] Send messages to SQS
- [ ] Receive and delete messages
- [ ] Understand long polling
- [ ] Create and publish to SNS topics
- [ ] Explain SQS vs SNS

---

## 🎯 Next Steps

1. **[Part 2: Lambda →](./PART-2-LAMBDA.md)** - Serverless functions
2. **[Part 3: Advanced Messaging →](./PART-3-MESSAGING-ADVANCED.md)** - FIFO, DLQ, Fan-out

**What do you want to learn next?** 🚀
