# 📘 PART 1 — Foundational Setup

Welcome to **Part 1** of your AWS SDK Kotlin journey! This module covers everything you need to know to start working with AWS SDK for Kotlin.

---

## 🎯 Learning Objectives

By the end of this lesson, you will:

✅ Understand what AWS SDK is and why we use it
✅ Learn AWS authentication mechanisms
✅ Configure AWS CLI and SDK credentials
✅ Set up a local development environment with Docker + LocalStack
✅ Understand AWS request signing (SigV4)
✅ Make your first API call: **List S3 buckets**

---

## 📚 Lesson 1.1: What is AWS SDK?

### What is AWS SDK?

**AWS SDK (Software Development Kit)** is a collection of libraries that allow you to interact with AWS services programmatically from your code.

Instead of using the AWS Console UI or AWS CLI, you can:
- Create S3 buckets
- Store/retrieve data from DynamoDB
- Send messages via SQS/SNS
- Invoke Lambda functions
- ...and much more!

### Why Kotlin?

The **AWS SDK for Kotlin** provides:
- **Type-safe** APIs
- **Coroutine-based** async operations (no callbacks!)
- **Idiomatic Kotlin** syntax
- **Null safety**
- **Multiplatform** support (JVM, Native)

### Architecture Overview

```
┌─────────────────────────────────────┐
│   Your Kotlin Application           │
│                                      │
│   ┌──────────────────────────────┐  │
│   │  AWS SDK for Kotlin          │  │
│   │  - S3Client                  │  │
│   │  - DynamoDbClient            │  │
│   │  - SqsClient                 │  │
│   │  - etc.                      │  │
│   └──────────────────────────────┘  │
│              ↓                       │
│   ┌──────────────────────────────┐  │
│   │  HTTP Client + SigV4 Signing │  │
│   └──────────────────────────────┘  │
└─────────────────────────────────────┘
              ↓
    ┌──────────────────┐
    │   AWS Services   │
    │   - S3           │
    │   - DynamoDB     │
    │   - SQS/SNS      │
    │   - Lambda       │
    └──────────────────┘
```

---

## 📚 Lesson 1.2: AWS Authentication

### How AWS Authentication Works

Every request to AWS must be **signed** using your credentials. AWS uses **Signature Version 4 (SigV4)** signing process.

### Credential Types

| Type | Use Case | Security |
|------|----------|----------|
| **IAM User Access Keys** | Local development, CI/CD | ⚠️ Long-lived, must rotate |
| **IAM Roles** | EC2, Lambda, ECS | ✅ Temporary, auto-rotated |
| **AWS SSO** | Corporate environments | ✅ Centralized, MFA-enabled |
| **STS Temporary Credentials** | Cross-account access | ✅ Short-lived |

### Credential Chain (Priority Order)

AWS SDK looks for credentials in this order:

1. **Environment variables**
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SESSION_TOKEN` (for temporary credentials)

2. **AWS credentials file** (`~/.aws/credentials`)

3. **AWS config file** (`~/.aws/config`)

4. **Container credentials** (ECS task role)

5. **Instance profile credentials** (EC2 IAM role)

---

## 📚 Lesson 1.3: Setting Up Your Environment

### Your Docker Environment

We've set up a complete environment for you:

```yaml
Services:
  ✅ LocalStack     - Local AWS cloud (S3, DynamoDB, SQS, SNS, Lambda)
  ✅ Kotlin App     - Your development container
  ✅ AWS CLI        - Command-line helper
```

### Starting the Environment

```bash
# Start all services
docker-compose up -d

# Check if services are running
docker-compose ps

# View logs
docker-compose logs -f localstack
```

### Stopping the Environment

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

---

## 📚 Lesson 1.4: AWS Configuration Files

### Example: `~/.aws/credentials`

```ini
[default]
aws_access_key_id = YOUR_ACCESS_KEY_ID
aws_secret_access_key = YOUR_SECRET_ACCESS_KEY

[development]
aws_access_key_id = DEV_ACCESS_KEY_ID
aws_secret_access_key = DEV_SECRET_ACCESS_KEY

[production]
aws_access_key_id = PROD_ACCESS_KEY_ID
aws_secret_access_key = PROD_SECRET_ACCESS_KEY
```

### Example: `~/.aws/config`

```ini
[default]
region = us-east-1
output = json

[profile development]
region = us-west-2
output = json

[profile production]
region = eu-west-1
output = json

# SSO Configuration
[profile sso-dev]
sso_start_url = https://my-company.awsapps.com/start
sso_region = us-east-1
sso_account_id = 123456789012
sso_role_name = DeveloperAccess
region = us-east-1
```

### Using AWS SSO

```bash
# Configure SSO
aws configure sso

# Login to SSO
aws sso login --profile sso-dev

# Use SSO profile
export AWS_PROFILE=sso-dev

# Verify credentials
aws sts get-caller-identity
```

---

## 📚 Lesson 1.5: Understanding SigV4 Request Signing

### What is SigV4?

**Signature Version 4 (SigV4)** is AWS's authentication protocol. It ensures:
- **Authentication** - Proves you are who you say you are
- **Integrity** - Request hasn't been tampered with
- **Non-repudiation** - Request came from you

### How It Works

```
┌─────────────────────────────────────────────────┐
│ 1. Canonical Request                            │
│    - HTTP method, URI, query params             │
│    - Headers (sorted)                           │
│    - Payload hash                               │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│ 2. String to Sign                               │
│    - Algorithm (AWS4-HMAC-SHA256)               │
│    - Timestamp                                  │
│    - Credential scope                           │
│    - Canonical request hash                     │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│ 3. Signing Key                                  │
│    - Derive from secret key                     │
│    - Date, region, service                      │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│ 4. Signature                                    │
│    - HMAC-SHA256 of string-to-sign              │
│    - Added to Authorization header              │
└─────────────────────────────────────────────────┘
```

**Good News:** AWS SDK handles all this for you automatically! 🎉

---

## 📚 Lesson 1.6: Project Structure Best Practices

### Recommended Structure

```
aws-sdk-kotlin-tutorial/
├── src/
│   ├── main/kotlin/com/awssdk/tutorial/
│   │   ├── Main.kt
│   │   ├── part1/                 # Foundation examples
│   │   │   └── ListS3Buckets.kt
│   │   ├── part2/                 # Core services
│   │   │   ├── s3/
│   │   │   ├── dynamodb/
│   │   │   ├── sqs/
│   │   │   └── sns/
│   │   ├── part3/                 # Advanced concepts
│   │   ├── part4/                 # Projects
│   │   └── common/                # Shared utilities
│   │       ├── AwsClientFactory.kt
│   │       └── Config.kt
│   └── test/kotlin/
├── lessons/                       # Tutorial markdown files
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts
```

---

## 🏃 Hands-On Exercise 1: Verify Your Setup

### Step 1: Start Docker Environment

```bash
docker-compose up -d
```

### Step 2: Check LocalStack Health

```bash
# Enter the AWS CLI container
docker-compose exec aws-cli sh

# Check LocalStack health
aws --endpoint-url=http://localstack:4566 s3 ls

# Exit container
exit
```

### Step 3: Run the Kotlin Application

```bash
# Enter Kotlin container
docker-compose exec kotlin-app bash

# Run the application
gradle run

# Exit container
exit
```

---

## 🏃 Hands-On Exercise 2: Your First AWS SDK Call

Let's make your first API call! We'll create a simple program to **list S3 buckets**.

**File:** `src/main/kotlin/com/awssdk/tutorial/part1/ListS3Buckets.kt`

You'll find the complete code in the next section!

---

## 🧪 Quiz: Test Your Knowledge

### Question 1
Which credential type is MOST secure for production workloads?
- A) IAM User Access Keys
- B) Root account credentials
- C) IAM Roles with temporary credentials
- D) Hardcoded credentials in code

<details>
<summary>Click for answer</summary>

**Answer: C) IAM Roles with temporary credentials**

Explanation: IAM Roles provide temporary, auto-rotating credentials that don't require manual management or rotation.
</details>

### Question 2
What is the AWS SDK credential lookup order (first to last)?
- A) Config file → Environment variables → IAM role
- B) Environment variables → Credentials file → IAM role
- C) IAM role → Environment variables → Credentials file
- D) Credentials file → IAM role → Environment variables

<details>
<summary>Click for answer</summary>

**Answer: B) Environment variables → Credentials file → IAM role**

Explanation: AWS SDK checks environment variables first, then the credentials/config files, and finally instance/container credentials.
</details>

### Question 3
What does SigV4 stand for?
- A) Signature Verification 4
- B) Signature Version 4
- C) Secure Signature V4
- D) Simple Signature Version 4

<details>
<summary>Click for answer</summary>

**Answer: B) Signature Version 4**

Explanation: SigV4 (Signature Version 4) is AWS's authentication protocol for signing API requests.
</details>

---

## ✅ Checkpoint: Can You...?

Before moving to Part 2, make sure you can:

- [ ] Explain what AWS SDK is and why we use it
- [ ] List at least 3 types of AWS credentials
- [ ] Start and stop the Docker environment
- [ ] Understand the credential chain priority
- [ ] Explain what SigV4 signing is (conceptually)
- [ ] Navigate the project structure

---

## 🎯 Next Steps

Ready to write some code? Let's move to your **first working example**!

Continue to: **[First Example: List S3 Buckets](./PART-1-FIRST-EXAMPLE.md)**

Or jump to: **[Part 2: Core AWS Services →](./PART-2-CORE-SERVICES.md)**

---

## 📖 Additional Resources

- [AWS SDK for Kotlin Documentation](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [AWS SigV4 Signing Process](https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html)
- [AWS Credential Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

---

**What would you like to learn next?**

1. Continue to first code example (List S3 Buckets)
2. Deep dive into SigV4 signing
3. Learn more about IAM roles
4. Jump to Part 2 (Core Services)

Let me know! 🚀
