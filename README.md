# 🎓 AWS SDK Complete Interactive Tutorial - Kotlin Edition

> **Your comprehensive, hands-on journey from AWS SDK beginner to expert!**

Welcome to the **complete AWS SDK tutorial** designed to teach you everything about using AWS SDK for Kotlin through interactive, project-based learning. This tutorial takes you from foundational concepts to building production-ready applications.

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose installed
- Basic Kotlin knowledge
- Terminal/Command line access

### Get Started in 3 Steps

```bash
# 1. Clone and enter the repository
cd aws-sdk-complete-interative-tutorial

# 2. Start the environment (LocalStack + Kotlin dev container)
docker-compose up -d

# 3. Run your first example
docker-compose exec kotlin-app gradle run
```

That's it! Your AWS SDK learning environment is ready! 🎉

---

## 📚 What's Inside?

This tutorial is organized into **6 comprehensive parts**:

### 📘 [Part 1 - Foundational Setup](./lessons/PART-1-FOUNDATION.md)

Learn the essentials:
- ✅ What is AWS SDK and why use it?
- ✅ AWS authentication (IAM roles, SSO, access keys)
- ✅ Configure AWS CLI and SDK credentials
- ✅ Understanding SigV4 request signing
- ✅ Project structure best practices
- ✅ **First API call:** List S3 buckets

**Start here:** [Part 1 Tutorial →](./lessons/PART-1-FOUNDATION.md)

---

### 📦 [Part 2 - Core AWS Services](./lessons/)

Master essential AWS services:

#### [S3 - Simple Storage Service](./lessons/PART-2-S3.md)
- Upload/download files
- Generate pre-signed URLs
- List and manage objects
- **Project:** S3 File Uploader Service

#### [DynamoDB - NoSQL Database](./lessons/PART-2-DYNAMODB.md)
- Create tables
- CRUD operations (PutItem, GetItem, UpdateItem, DeleteItem)
- Query vs Scan
- **Project:** User Profile CRUD Microservice

#### [SQS/SNS - Messaging](./lessons/PART-2-MESSAGING.md)
- Send/receive SQS messages
- Publish to SNS topics
- Long polling and FIFO queues
- **Project:** Email Notification System

#### Lambda - Serverless (Coming soon!)
- Invoke Lambda functions
- Handle async execution
- Error handling and retries

---

### 🛡️ Part 3 - Advanced Concepts (Coming soon!)

Deep dive into:
- Advanced authentication (assume roles, STS)
- Performance optimization
- Error handling strategies
- Testing with LocalStack
- CI/CD integration

---

### 🧩 Part 4 - Real-World Projects (Coming soon!)

Build complete applications:
- **Serverless File Sharing App** (S3 + Lambda + API Gateway)
- **AI Document Processing Pipeline** (S3 + SQS + Lambda + DynamoDB)
- **Multi-Tenant Admin Dashboard**
- **Event-Driven Inventory System**

---

### 🎯 Part 5 - Expert Best Practices (Coming soon!)

Production-ready patterns:
- Resilience and fault tolerance
- Cost optimization
- Security best practices
- Performance tuning
- Observability with X-Ray

---

### 🎓 Part 6 - Gamified Learning (Coming soon!)

Test your skills:
- Interactive quizzes
- Coding challenges
- Debugging exercises
- Final certification project

---

## 🏗️ Project Structure

```
aws-sdk-kotlin-tutorial/
├── src/
│   ├── main/kotlin/com/awssdk/tutorial/
│   │   ├── Main.kt                    # Entry point
│   │   ├── common/
│   │   │   └── AwsClientFactory.kt    # Reusable AWS clients
│   │   ├── part1/                     # Foundation examples
│   │   │   ├── ListS3Buckets.kt
│   │   │   └── CreateS3Bucket.kt
│   │   ├── part2/                     # Core services
│   │   │   ├── s3/
│   │   │   ├── dynamodb/
│   │   │   ├── sqs/
│   │   │   └── sns/
│   │   └── part3/                     # Advanced topics
│   └── test/kotlin/                   # Tests
│
├── lessons/                           # Tutorial content
│   ├── PART-1-FOUNDATION.md
│   ├── PART-1-FIRST-EXAMPLE.md
│   ├── PART-2-S3.md
│   ├── PART-2-DYNAMODB.md
│   └── PART-2-MESSAGING.md
│
├── aws-config-examples/               # AWS config examples
│   ├── credentials.example
│   └── config.example
│
├── docker-compose.yml                 # Docker orchestration
├── Dockerfile                         # Kotlin dev container
├── build.gradle.kts                   # Gradle build
└── README.md                          # This file
```

---

## 🐳 Docker Environment

### Services

| Service | Purpose | Port |
|---------|---------|------|
| **localstack** | Local AWS cloud | 4566 |
| **kotlin-app** | Development container | - |
| **aws-cli** | CLI helper | - |

### Useful Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f localstack

# Enter Kotlin dev container
docker-compose exec kotlin-app bash

# Run a specific example
docker-compose exec kotlin-app gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# Stop all services
docker-compose down

# Clean slate (remove volumes)
docker-compose down -v
```

### Test LocalStack is Running

```bash
# Using AWS CLI container
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 ls

# Create a test bucket
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://test-bucket
```

---

## 🎯 Learning Paths

### Path 1: Beginner (Recommended)

1. Read [Part 1 - Foundation](./lessons/PART-1-FOUNDATION.md)
2. Run [First Example: List S3 Buckets](./lessons/PART-1-FIRST-EXAMPLE.md)
3. Complete [Part 2 - S3](./lessons/PART-2-S3.md)
4. Complete [Part 2 - DynamoDB](./lessons/PART-2-DYNAMODB.md)
5. Complete [Part 2 - Messaging](./lessons/PART-2-MESSAGING.md)

### Path 2: Experienced Developer (Fast Track)

1. Skim [Part 1 - Foundation](./lessons/PART-1-FOUNDATION.md)
2. Jump to specific services in Part 2
3. Focus on projects in Part 4

### Path 3: Specific Service Focus

Choose the service you need:
- **Storage?** → [S3 Tutorial](./lessons/PART-2-S3.md)
- **Database?** → [DynamoDB Tutorial](./lessons/PART-2-DYNAMODB.md)
- **Messaging?** → [SQS/SNS Tutorial](./lessons/PART-2-MESSAGING.md)

---

## 🧪 Running Examples

### Method 1: Inside Docker Container

```bash
# Enter container
docker-compose exec kotlin-app bash

# Run any example
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# Or shorter:
gradle run -PmainClass=com.awssdk.tutorial.part2.s3.UploadFileKt
```

### Method 2: Gradle Wrapper (Host Machine)

```bash
# If you have Gradle installed locally
./gradlew run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

---

## 🔧 Troubleshooting

### Issue: "Connection refused"

**Problem:** LocalStack isn't running

**Solution:**
```bash
docker-compose up -d localstack
docker-compose logs localstack  # Check logs
```

### Issue: "No credentials found"

**Problem:** AWS credentials not configured

**Solution:**
The Docker Compose environment sets test credentials automatically. If running outside Docker, check:
```bash
echo $AWS_ACCESS_KEY_ID
echo $AWS_SECRET_ACCESS_KEY
```

### Issue: "Table/Bucket doesn't exist"

**Problem:** Resource not created yet

**Solution:**
Run the creation examples first:
```bash
# Create S3 bucket
docker-compose exec kotlin-app gradle run -PmainClass=com.awssdk.tutorial.part1.CreateS3BucketKt

# Create DynamoDB table
docker-compose exec kotlin-app gradle run -PmainClass=com.awssdk.tutorial.part2.dynamodb.CreateTableKt
```

### Issue: Gradle build fails

**Problem:** Dependencies not downloaded or build cache corrupted

**Solution:**
```bash
# Rebuild containers
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## 💡 Tips for Success

### 1. **Hands-On Practice**
Don't just read - run every example and experiment with modifications.

### 2. **Use the REPL**
Enter the Kotlin container and experiment:
```bash
docker-compose exec kotlin-app bash
kotlinc
```

### 3. **Read Error Messages**
AWS SDK provides detailed error messages. They're your friends!

### 4. **Check the Docs**
- [AWS SDK for Kotlin Documentation](https://docs.aws.amazon.com/sdk-for-kotlin/)
- [LocalStack Documentation](https://docs.localstack.cloud/)

### 5. **Ask Questions**
Stuck? Check the quiz answers or experiment with the code.

---

## 🎓 Interactive Features

Every lesson includes:

✅ **Clear explanations** - Concepts explained like a senior instructor
✅ **Working code examples** - Copy, run, and learn
✅ **Hands-on exercises** - Practice what you learned
✅ **Quizzes** - Test your understanding
✅ **Checkpoints** - Ensure you're ready to move forward
✅ **Mini projects** - Apply concepts to real scenarios
✅ **Deep dives** - Optional advanced topics
✅ **Visual diagrams** - Understand architecture patterns

---

## 📊 Progress Tracking

Use this checklist to track your progress:

### Part 1: Foundation
- [ ] Understand AWS SDK basics
- [ ] Configure authentication
- [ ] Run first API call (List S3 Buckets)

### Part 2: Core Services
- [ ] S3 operations (upload, download, list)
- [ ] DynamoDB CRUD operations
- [ ] SQS message queue
- [ ] SNS pub/sub

### Part 3: Advanced (Coming Soon)
- [ ] Authentication strategies
- [ ] Performance optimization
- [ ] Testing patterns

### Part 4: Projects (Coming Soon)
- [ ] File Sharing App
- [ ] Document Processing Pipeline
- [ ] Multi-Tenant Dashboard

---

## 🤝 Contributing

Found an issue or have a suggestion? Feel free to:
- Open an issue
- Submit a pull request
- Suggest new examples or projects

---

## 📜 License

This tutorial is provided as-is for educational purposes.

---

## 🌟 What's Next?

Ready to begin your AWS SDK journey?

### **👉 [Start with Part 1 - Foundation →](./lessons/PART-1-FOUNDATION.md)**

Or jump to a specific topic:
- [S3 Tutorial →](./lessons/PART-2-S3.md)
- [DynamoDB Tutorial →](./lessons/PART-2-DYNAMODB.md)
- [Messaging Tutorial →](./lessons/PART-2-MESSAGING.md)

---

## 💬 Support

Need help? Here are your resources:

- **Tutorial Docs:** Check the `lessons/` directory
- **Code Examples:** Browse `src/main/kotlin/`
- **AWS SDK Docs:** [Official Documentation](https://docs.aws.amazon.com/sdk-for-kotlin/)
- **LocalStack:** [LocalStack Docs](https://docs.localstack.cloud/)

---

<div align="center">

### **Happy Learning! 🚀**

Built with ❤️ using Kotlin and AWS SDK

[Get Started →](./lessons/PART-1-FOUNDATION.md)

</div>
