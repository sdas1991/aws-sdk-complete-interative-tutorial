# 🖥️ Local Development Guide - Complete Navigation

This guide provides **complete instructions** for navigating and using the AWS SDK Kotlin tutorial in your local development environment with LocalStack.

---

## 📋 Table of Contents

1. [Environment Setup](#environment-setup)
2. [Understanding Your Local Environment](#understanding-your-local-environment)
3. [Navigation Guide](#navigation-guide)
4. [Running Examples](#running-examples)
5. [Development Workflow](#development-workflow)
6. [Testing and Debugging](#testing-and-debugging)
7. [Troubleshooting](#troubleshooting)

---

## 🚀 Environment Setup

### Prerequisites

```bash
# Check if you have required tools
docker --version          # Should be 20.10+ or higher
docker-compose --version  # Should be 1.29+ or higher
```

If you don't have Docker installed:
- **macOS**: Install [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
- **Windows**: Install [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
- **Linux**: Install [Docker Engine](https://docs.docker.com/engine/install/)

### Initial Setup

```bash
# 1. Navigate to the project directory
cd aws-sdk-complete-interative-tutorial

# 2. Start all services (LocalStack + Kotlin + AWS CLI)
docker-compose up -d

# 3. Verify all services are running
docker-compose ps
```

**Expected output:**
```
NAME                  COMMAND                  STATUS
aws-cli-helper        "/bin/sh -c 'tail -f…"   Up
aws-localstack        "docker-entrypoint.sh"   Up (healthy)
kotlin-aws-sdk-app    "tail -f /dev/null"      Up
```

### First-Time Initialization

```bash
# Wait for LocalStack to fully start (takes ~10 seconds)
sleep 10

# Optional: Initialize LocalStack with sample resources
docker-compose exec aws-cli sh /app/scripts/init-localstack.sh
```

---

## 🏗️ Understanding Your Local Environment

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ Your Host Machine                                       │
│                                                         │
│  ┌───────────────────────────────────────────────┐    │
│  │ Docker Compose Network: aws-tutorial-network  │    │
│  │                                                │    │
│  │  ┌──────────────┐  ┌──────────────────────┐  │    │
│  │  │ LocalStack   │  │ Kotlin Dev Container │  │    │
│  │  │              │  │                      │  │    │
│  │  │ Port: 4566   │◄─┤ • Gradle            │  │    │
│  │  │              │  │ • AWS SDK for Kotlin │  │    │
│  │  │ AWS Services:│  │ • Source code        │  │    │
│  │  │ • S3         │  │                      │  │    │
│  │  │ • DynamoDB   │  │ Environment:         │  │    │
│  │  │ • SQS        │  │ AWS_ENDPOINT_URL     │  │    │
│  │  │ • SNS        │  │ AWS_REGION           │  │    │
│  │  │ • Lambda     │  │ AWS_ACCESS_KEY_ID    │  │    │
│  │  │ • Secrets Mgr│  │ AWS_SECRET_ACCESS_KEY│  │    │
│  │  └──────────────┘  └──────────────────────┘  │    │
│  │                                                │    │
│  │  ┌──────────────┐                             │    │
│  │  │ AWS CLI      │                             │    │
│  │  │ Container    │                             │    │
│  │  │              │                             │    │
│  │  │ For testing  │                             │    │
│  │  └──────────────┘                             │    │
│  └───────────────────────────────────────────────┘    │
│                                                         │
│  Volumes:                                              │
│  • ./  → /app (source code sync)                      │
│  • gradle-cache → /root/.gradle                       │
│  • ./localstack-data → /tmp/localstack                │
└─────────────────────────────────────────────────────────┘
```

### Service Details

#### 1. **LocalStack** (aws-localstack)
- **Purpose**: Simulates AWS cloud services locally
- **Endpoint**: `http://localhost:4566` (from host) or `http://localstack:4566` (from containers)
- **Services Available**: S3, DynamoDB, SQS, SNS, Lambda, Secrets Manager, STS
- **Data Persistence**: Stored in `./localstack-data/` directory
- **Access Logs**: `docker-compose logs -f localstack`

#### 2. **Kotlin Dev Container** (kotlin-aws-sdk-app)
- **Purpose**: Your main development environment
- **Contents**: Gradle, JDK 17, Kotlin compiler, AWS SDK dependencies
- **Source Code**: Synced from host machine's `./` directory to container's `/app`
- **Build Cache**: Persisted in `gradle-cache` volume for faster builds
- **Shell Access**: `docker-compose exec kotlin-app bash`

#### 3. **AWS CLI Container** (aws-cli-helper)
- **Purpose**: Quick AWS CLI commands for testing
- **Pre-configured**: Points to LocalStack endpoint
- **Usage**: `docker-compose exec aws-cli aws s3 ls --endpoint-url=http://localstack:4566`

### Environment Variables

These are automatically set in `docker-compose.yml`:

```yaml
AWS_ENDPOINT_URL=http://localstack:4566  # Points to LocalStack
AWS_REGION=us-east-1                     # Default region
AWS_ACCESS_KEY_ID=test                   # LocalStack test credentials
AWS_SECRET_ACCESS_KEY=test               # LocalStack test credentials
```

---

## 🗺️ Navigation Guide

### Project Structure Explained

```
aws-sdk-complete-interative-tutorial/
│
├── 📚 DOCUMENTATION
│   ├── README.md                    # Main overview and getting started
│   ├── QUICKSTART.md                # 5-minute quick start guide
│   ├── docs/
│   │   ├── LOCAL-DEVELOPMENT-GUIDE.md     # ← You are here!
│   │   ├── AWS-DEPLOYMENT-GUIDE.md        # Using real AWS
│   │   └── TRANSITION-GUIDE.md            # LocalStack → AWS
│
├── 📖 TUTORIALS (Start learning here!)
│   └── lessons/
│       ├── PART-1-FOUNDATION.md           # Start: AWS SDK basics
│       ├── PART-1-FIRST-EXAMPLE.md        # Your first API call
│       ├── PART-2-S3.md                   # S3 operations
│       ├── PART-2-DYNAMODB.md             # DynamoDB NoSQL
│       └── PART-2-MESSAGING.md            # SQS/SNS messaging
│
├── 💻 SOURCE CODE
│   └── src/main/kotlin/com/awssdk/tutorial/
│       ├── Main.kt                        # Entry point
│       ├── common/
│       │   └── AwsClientFactory.kt        # Reusable AWS clients
│       ├── part1/                         # Foundation examples
│       │   ├── ListS3Buckets.kt          # List S3 buckets
│       │   └── CreateS3Bucket.kt         # Create S3 bucket
│       ├── part2/                         # Service examples
│       │   ├── s3/                        # S3 examples
│       │   ├── dynamodb/                  # DynamoDB examples
│       │   ├── sqs/                       # SQS examples
│       │   └── sns/                       # SNS examples
│       └── part3/                         # Advanced (coming)
│
├── 🔧 CONFIGURATION
│   ├── build.gradle.kts                   # Gradle build config
│   ├── settings.gradle.kts                # Gradle settings
│   ├── gradle.properties                  # Gradle properties
│   ├── docker-compose.yml                 # Docker orchestration
│   ├── Dockerfile                         # Kotlin container image
│   └── aws-config-examples/               # AWS config examples
│       ├── credentials.example            # AWS credentials format
│       └── config.example                 # AWS config format
│
└── 🛠️ UTILITIES
    └── scripts/
        ├── init-localstack.sh             # Initialize sample resources
        └── run-example.sh                 # Helper to run examples
```

### Where to Start

#### **Complete Beginner?**
1. **Read**: `QUICKSTART.md` (5 minutes)
2. **Read**: `lessons/PART-1-FOUNDATION.md` (30 minutes)
3. **Code**: `lessons/PART-1-FIRST-EXAMPLE.md` (15 minutes)
4. **Practice**: Run examples in `src/main/kotlin/.../part1/`

#### **Experienced with AWS?**
1. **Skim**: `QUICKSTART.md`
2. **Jump to**: Specific service in `lessons/PART-2-*.md`
3. **Code**: Corresponding examples in `src/main/kotlin/.../part2/`

#### **Need Specific Service?**
- **S3 Storage**: `lessons/PART-2-S3.md` + `src/.../part2/s3/`
- **DynamoDB**: `lessons/PART-2-DYNAMODB.md` + `src/.../part2/dynamodb/`
- **Messaging**: `lessons/PART-2-MESSAGING.md` + `src/.../part2/sqs/` + `src/.../part2/sns/`

---

## ▶️ Running Examples

### Method 1: Using Docker Compose (Recommended)

This is the **recommended method** for following the tutorial.

```bash
# Step 1: Enter the Kotlin development container
docker-compose exec kotlin-app bash

# You're now inside the container. You should see:
# root@<container-id>:/app#

# Step 2: Run any example using Gradle
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# Step 3: Exit when done
exit
```

### Method 2: Using Helper Script

```bash
# From your host machine (outside containers)
./scripts/run-example.sh part1.ListS3Buckets

# More examples:
./scripts/run-example.sh part1.CreateS3Bucket
./scripts/run-example.sh part2.s3.UploadFile
./scripts/run-example.sh part2.dynamodb.CreateTable
```

### Method 3: Direct Docker Exec (One-liner)

```bash
# Run from host machine without entering container
docker-compose exec kotlin-app gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

### Running the Main Application

```bash
# Method 1: From inside container
docker-compose exec kotlin-app bash
gradle run

# Method 2: From host
docker-compose exec kotlin-app gradle run
```

---

## 🔄 Development Workflow

### Typical Learning Session

```bash
# 1. Start environment (do this once per day/session)
docker-compose up -d

# 2. Verify services are healthy
docker-compose ps

# 3. Open tutorial in your editor/browser
# Read: lessons/PART-1-FOUNDATION.md

# 4. Enter development container
docker-compose exec kotlin-app bash

# 5. Run examples as you learn
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# 6. Experiment with code (edit files in your editor)
# The container will see changes immediately via volume mount

# 7. Re-run to see your changes
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# 8. When done for the day
exit                      # Exit container
docker-compose down       # Stop services (keeps data)
# OR
docker-compose down -v    # Stop and delete all data (fresh start next time)
```

### Editing Code

You can edit files on your **host machine** using your favorite IDE:

```bash
# Your favorite editor/IDE on host machine
code .                    # VS Code
idea .                    # IntelliJ IDEA
vim src/main/kotlin/...   # Vim
```

**Changes are immediate!** The container sees your edits via Docker volume mounts.

### Building and Testing

```bash
# Inside the Kotlin container

# Build the project
gradle build

# Run tests (when you write them)
gradle test

# Clean build
gradle clean build

# Check dependencies
gradle dependencies
```

---

## 🧪 Testing and Debugging

### Using AWS CLI to Verify

```bash
# List S3 buckets
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 ls

# Create a test bucket
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://test-bucket

# Upload a file
echo "Hello World" > /tmp/test.txt
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 cp /tmp/test.txt s3://test-bucket/

# List objects in bucket
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 ls s3://test-bucket/

# DynamoDB: List tables
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 dynamodb list-tables

# SQS: List queues
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 sqs list-queues

# SNS: List topics
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 sns list-topics
```

### Viewing Logs

```bash
# LocalStack logs (see AWS API calls)
docker-compose logs -f localstack

# Kotlin app logs
docker-compose logs -f kotlin-app

# All logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100 localstack
```

### Debugging Tips

#### Enable Debug Logging

Edit `src/main/resources/logback.xml` (create if doesn't exist):

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="aws.sdk" level="DEBUG"/>
    <logger name="aws.smithy" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### Interactive Kotlin REPL

```bash
# Enter container
docker-compose exec kotlin-app bash

# Start Kotlin REPL
kotlinc

# Try AWS SDK commands interactively
:load /app/src/main/kotlin/com/awssdk/tutorial/common/AwsClientFactory.kt
```

---

## 🔧 Troubleshooting

### Issue: "Connection refused" or "Cannot connect to LocalStack"

**Symptoms:**
```
Exception: Connection refused: localstack/172.20.0.2:4566
```

**Solutions:**

```bash
# 1. Check if LocalStack is running
docker-compose ps

# If not running, start it
docker-compose up -d localstack

# 2. Check LocalStack health
docker-compose logs localstack | grep "Ready"

# Should see: "Ready."

# 3. Wait a bit longer (LocalStack takes ~10 seconds to start)
sleep 10

# 4. Test connectivity from Kotlin container
docker-compose exec kotlin-app curl http://localstack:4566/_localstack/health

# Should return JSON with service statuses
```

### Issue: "No buckets found" but I created them

**Symptoms:**
- AWS CLI shows buckets, but Kotlin code doesn't
- Buckets disappear after restart

**Solutions:**

```bash
# Check if data is persisted
ls -la ./localstack-data/

# If empty, LocalStack data isn't persisting
# Make sure docker-compose.yml has:
# volumes:
#   - "./localstack-data:/tmp/localstack"

# Recreate bucket
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://my-bucket
```

### Issue: Gradle build fails

**Symptoms:**
```
Could not resolve dependencies
Could not download kotlin-stdlib.jar
```

**Solutions:**

```bash
# 1. Clean Gradle cache
docker-compose exec kotlin-app gradle clean

# 2. Delete Gradle cache and rebuild
docker-compose down
docker volume rm aws-sdk-complete-interative-tutorial_gradle-cache
docker-compose up -d

# 3. Rebuild Docker images
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Issue: "Port 4566 already in use"

**Symptoms:**
```
Error starting LocalStack: bind: address already in use
```

**Solutions:**

```bash
# 1. Find what's using the port
lsof -i :4566

# 2. Kill the process
kill -9 <PID>

# 3. Or change the port in docker-compose.yml
# Edit: "4567:4566" instead of "4566:4566"
# Update code to use localhost:4567
```

### Issue: Changes to code not reflected

**Symptoms:**
- You edited code, but running example shows old behavior

**Solutions:**

```bash
# 1. Rebuild inside container
docker-compose exec kotlin-app gradle clean build

# 2. Restart container
docker-compose restart kotlin-app

# 3. Check if file is really updated
docker-compose exec kotlin-app cat /app/src/main/kotlin/.../YourFile.kt
```

### Issue: Out of memory errors

**Symptoms:**
```
OutOfMemoryError: Java heap space
```

**Solutions:**

```bash
# Edit gradle.properties and increase memory
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties

# Restart container
docker-compose restart kotlin-app
```

### Issue: LocalStack services not available

**Symptoms:**
- Some AWS services return errors
- "Service not enabled" messages

**Solutions:**

```bash
# Check LocalStack health
docker-compose exec aws-cli curl http://localstack:4566/_localstack/health

# Verify SERVICES in docker-compose.yml includes what you need
# Should have: s3,dynamodb,sqs,sns,lambda,secretsmanager,sts

# Restart LocalStack
docker-compose restart localstack
sleep 10
```

---

## 📊 Monitoring and Observability

### Check LocalStack Health

```bash
# Health endpoint
curl http://localhost:4566/_localstack/health

# Example response:
{
  "services": {
    "s3": "running",
    "dynamodb": "running",
    "sqs": "running",
    "sns": "running"
  }
}
```

### View Resource Usage

```bash
# Docker stats
docker stats

# Specific container
docker stats kotlin-aws-sdk-app

# Disk usage
docker system df
```

### Cleanup

```bash
# Remove stopped containers
docker-compose rm

# Remove all data (fresh start)
docker-compose down -v

# Remove unused Docker resources
docker system prune -a
```

---

## 🎯 Best Practices for Local Development

### 1. **Data Persistence**

```bash
# Keep data between restarts
docker-compose down        # Good - keeps volumes

# Fresh start (deletes all data)
docker-compose down -v     # Use when you want clean slate
```

### 2. **Resource Management**

```bash
# Stop when not using (frees resources)
docker-compose stop

# Start again later
docker-compose start

# Check what's using resources
docker stats
```

### 3. **Code Organization**

- Keep experiments in `src/main/kotlin/com/awssdk/tutorial/experiments/`
- Follow package structure for examples
- Use descriptive names: `ListS3BucketsWithPagination.kt`

### 4. **Version Control**

```bash
# Don't commit LocalStack data
# Already in .gitignore:
# localstack-data/
# build/
# .gradle/

# Do commit your code changes
git add src/main/kotlin/...
git commit -m "Add: Custom S3 upload example"
```

---

## 🚀 Next Steps

Now that you understand local development:

1. **Start Learning**: Open `lessons/PART-1-FOUNDATION.md`
2. **Run Examples**: Try `src/main/kotlin/.../part1/ListS3Buckets.kt`
3. **Transition to AWS**: When ready, see `docs/AWS-DEPLOYMENT-GUIDE.md`

---

## 📚 Quick Reference

### Essential Commands

| Task | Command |
|------|---------|
| Start environment | `docker-compose up -d` |
| Stop environment | `docker-compose down` |
| Enter Kotlin container | `docker-compose exec kotlin-app bash` |
| Run example | `gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt` |
| View logs | `docker-compose logs -f localstack` |
| AWS CLI test | `docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 ls` |
| Check health | `curl http://localhost:4566/_localstack/health` |
| Clean slate | `docker-compose down -v && docker-compose up -d` |

### File Paths

| Purpose | Path |
|---------|------|
| Tutorials | `lessons/` |
| Code examples | `src/main/kotlin/com/awssdk/tutorial/` |
| AWS client factory | `src/main/kotlin/com/awssdk/tutorial/common/AwsClientFactory.kt` |
| Configuration | `build.gradle.kts`, `docker-compose.yml` |
| Helper scripts | `scripts/` |

---

**Ready to code?** 🚀

Continue to: **[AWS Deployment Guide](./AWS-DEPLOYMENT-GUIDE.md)** or **[Start Part 1 Tutorial](../lessons/PART-1-FOUNDATION.md)**
