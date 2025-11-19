# ☁️ AWS Deployment Guide - Using Real AWS Services

This guide shows you how to transition from LocalStack to **real AWS services** and deploy your applications in production environments.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [AWS Account Setup](#aws-account-setup)
3. [Credentials Configuration](#credentials-configuration)
4. [Modifying Code for AWS](#modifying-code-for-aws)
5. [Deployment Scenarios](#deployment-scenarios)
6. [Security Best Practices](#security-best-practices)
7. [Cost Management](#cost-management)
8. [Monitoring and Logging](#monitoring-and-logging)

---

## 🎯 Overview

### LocalStack vs Real AWS

| Aspect | LocalStack (Development) | Real AWS (Production) |
|--------|-------------------------|----------------------|
| **Endpoint** | `http://localhost:4566` | AWS region endpoints |
| **Credentials** | `test` / `test` | Real IAM credentials |
| **Cost** | Free | Pay for usage |
| **Data** | Temporary | Persistent |
| **Performance** | Limited | Full AWS scale |
| **Services** | Subset | All AWS services |

### When to Use Real AWS

✅ **Production applications**
✅ **Integration testing** with real AWS services
✅ **Performance testing** at scale
✅ **Learning advanced features** not available in LocalStack
✅ **Cross-service integration** (e.g., AWS Lambda triggers)

⚠️ **Start with LocalStack for:**
- Initial learning
- Unit testing
- Development without costs
- Rapid prototyping

---

## 🔐 AWS Account Setup

### Step 1: Create AWS Account

1. Go to [https://aws.amazon.com](https://aws.amazon.com)
2. Click "Create an AWS Account"
3. Follow the registration process
4. **Note**: Requires credit card, but AWS Free Tier available

### Step 2: Enable MFA (Highly Recommended)

```bash
# Secure your root account with Multi-Factor Authentication
# AWS Console → IAM → Dashboard → Activate MFA on your root account
```

### Step 3: Create IAM User for Development

**⚠️ NEVER use root account credentials for development!**

```bash
# In AWS Console:
# 1. Go to IAM → Users → Add user
# 2. User name: "dev-user" (or your name)
# 3. Access type: ✅ Programmatic access
# 4. Permissions: Attach policies:
#    - AmazonS3FullAccess (for S3 examples)
#    - AmazonDynamoDBFullAccess (for DynamoDB examples)
#    - AmazonSQSFullAccess (for SQS examples)
#    - AmazonSNSFullAccess (for SNS examples)
#
# 5. Download credentials CSV file - SAVE IT SECURELY!
```

**Example IAM Policy (Least Privilege):**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::my-tutorial-bucket",
        "arn:aws:s3:::my-tutorial-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:*:table/Users"
    }
  ]
}
```

---

## 🔑 Credentials Configuration

### Method 1: AWS CLI Configuration (Recommended for Local Dev)

```bash
# Install AWS CLI v2
# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Windows
# Download from: https://awscli.amazonaws.com/AWSCLIV2.msi

# Configure credentials
aws configure

# Enter your IAM user credentials:
# AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
# AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
# Default region name [None]: us-east-1
# Default output format [None]: json
```

This creates two files:

**`~/.aws/credentials`:**
```ini
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

**`~/.aws/config`:**
```ini
[default]
region = us-east-1
output = json
```

### Method 2: Environment Variables

```bash
# Set environment variables (Linux/macOS)
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export AWS_REGION=us-east-1

# Windows PowerShell
$env:AWS_ACCESS_KEY_ID="AKIAIOSFODNN7EXAMPLE"
$env:AWS_SECRET_ACCESS_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
$env:AWS_REGION="us-east-1"

# Windows CMD
set AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
set AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
set AWS_REGION=us-east-1
```

### Method 3: Multiple Profiles

```bash
# Configure additional profiles
aws configure --profile production
aws configure --profile development

# ~/.aws/credentials
[development]
aws_access_key_id = DEV_KEY_ID
aws_secret_access_key = DEV_SECRET_KEY

[production]
aws_access_key_id = PROD_KEY_ID
aws_secret_access_key = PROD_SECRET_KEY

# Use specific profile
export AWS_PROFILE=development
```

### Method 4: AWS SSO (Recommended for Organizations)

```bash
# Configure SSO
aws configure sso

# Follow prompts:
# SSO start URL: https://my-company.awsapps.com/start
# SSO Region: us-east-1
# Account: Select your account
# Role: Select your role
# CLI default client Region: us-east-1

# Login
aws sso login --profile my-sso-profile

# Use SSO profile
export AWS_PROFILE=my-sso-profile
```

---

## 🔧 Modifying Code for AWS

### Option 1: Environment Variable Toggle (Recommended)

**No code changes needed!** Just set environment variable:

```bash
# Use LocalStack
export USE_LOCALSTACK=true
gradle run

# Use Real AWS
export USE_LOCALSTACK=false
gradle run
```

The `AwsClientFactory.kt` already supports this:

```kotlin
// In AwsClientFactory.kt
private val useLocalStack = System.getenv("USE_LOCALSTACK")?.toBoolean() ?: true
```

### Option 2: Direct Code Modification

**Original (LocalStack):**
```kotlin
val s3Client = AwsClientFactory.createS3Client()  // Uses LocalStack by default
```

**For Real AWS:**
```kotlin
val s3Client = AwsClientFactory.createS3Client(forceLocalStack = false)
```

### Option 3: Create Separate Configuration

**`src/main/kotlin/com/awssdk/tutorial/common/Config.kt`:**

```kotlin
package com.awssdk.tutorial.common

object Config {
    enum class Environment {
        LOCAL,
        DEVELOPMENT,
        PRODUCTION
    }

    val currentEnvironment: Environment by lazy {
        when (System.getenv("APP_ENV")?.uppercase()) {
            "PRODUCTION" -> Environment.PRODUCTION
            "DEVELOPMENT" -> Environment.DEVELOPMENT
            else -> Environment.LOCAL
        }
    }

    val useLocalStack: Boolean
        get() = currentEnvironment == Environment.LOCAL

    val awsRegion: String
        get() = System.getenv("AWS_REGION") ?: "us-east-1"
}
```

**Usage:**
```kotlin
val s3Client = AwsClientFactory.createS3Client(
    forceLocalStack = Config.useLocalStack
)
```

### Example: Modified ListS3Buckets for Real AWS

**`src/main/kotlin/com/awssdk/tutorial/part1/ListS3BucketsAWS.kt`:**

```kotlin
package com.awssdk.tutorial.part1

import aws.sdk.kotlin.services.s3.model.S3Exception
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🪣 AWS SDK Kotlin - List S3 Buckets (Real AWS)")
    println("=".repeat(50))

    try {
        // Force real AWS (not LocalStack)
        AwsClientFactory.createS3Client(forceLocalStack = false).use { s3Client ->

            println("\n📡 Connecting to: Real AWS")
            println("🌍 Region: ${System.getenv("AWS_REGION") ?: "us-east-1"}")
            println("🔍 Listing S3 buckets...\n")

            val response = s3Client.listBuckets()
            val buckets = response.buckets

            if (buckets.isNullOrEmpty()) {
                println("📭 No buckets found")
                println("\n💡 TIP: Create a bucket in AWS Console or use CreateS3BucketAWS.kt")
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
        println("❌ S3 Error: ${e.message}")
        println("   Error Code: ${e.sdkErrorMetadata.errorCode}")

        if (e.message?.contains("credentials") == true) {
            println("\n💡 Check your AWS credentials:")
            println("   - Run: aws configure")
            println("   - Or set: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY")
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
```

---

## 🚀 Deployment Scenarios

### Scenario 1: Local Development with Real AWS

**Best for:** Testing against real AWS while developing locally

```bash
# 1. Configure AWS credentials
aws configure

# 2. Set environment to use real AWS
export USE_LOCALSTACK=false
export AWS_REGION=us-east-1

# 3. Run your code locally
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

**Pros:**
- ✅ Test with real AWS services
- ✅ Quick iteration
- ✅ Full debugging capabilities

**Cons:**
- ⚠️ Incurs AWS costs
- ⚠️ Requires internet connection
- ⚠️ Slower than LocalStack

---

### Scenario 2: Deploying to EC2

**Best for:** Traditional server-based applications

#### Step 1: Create EC2 Instance

```bash
# Launch EC2 instance via AWS Console or CLI
aws ec2 run-instances \
    --image-id ami-0c55b159cbfafe1f0 \
    --instance-type t2.micro \
    --key-name my-key-pair \
    --security-group-ids sg-0123456789abcdef0 \
    --iam-instance-profile Name=EC2-S3-DynamoDB-Role
```

#### Step 2: Create IAM Role for EC2

```bash
# Create role with permissions
# AWS Console → IAM → Roles → Create Role
# Trusted entity: EC2
# Attach policies:
# - AmazonS3FullAccess
# - AmazonDynamoDBFullAccess
```

#### Step 3: Deploy Application

```bash
# SSH into EC2
ssh -i my-key-pair.pem ec2-user@<ec2-public-ip>

# Install Java
sudo yum install java-17-amazon-corretto -y

# Copy your application
scp -i my-key-pair.pem -r ./build ec2-user@<ec2-public-ip>:~/

# Run application
java -jar build/libs/aws-sdk-kotlin-tutorial-1.0.0.jar
```

#### Code Changes: Use Instance Profile

```kotlin
// AwsClientFactory.kt - Auto-detects EC2 instance profile
fun createS3Client(forceLocalStack: Boolean? = null): S3Client {
    return S3Client {
        region = System.getenv("AWS_REGION") ?: "us-east-1"

        if (forceLocalStack == true) {
            endpointUrl = Url.parse("http://localstack:4566")
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "test"
                secretAccessKey = "test"
            }
        }
        // If forceLocalStack is false/null, SDK uses default credential chain
        // which automatically picks up EC2 instance profile
    }
}
```

**No code changes needed!** The SDK automatically uses the EC2 instance profile.

---

### Scenario 3: Deploying to ECS/Fargate

**Best for:** Containerized applications

#### Docker Image for AWS

**`Dockerfile.aws`:**
```dockerfile
FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Run application
CMD ["java", "-jar", "app.jar"]
```

#### Build and Push to ECR

```bash
# Create ECR repository
aws ecr create-repository --repository-name aws-sdk-kotlin-tutorial

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
    docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build image
docker build -f Dockerfile.aws -t aws-sdk-kotlin-tutorial .

# Tag image
docker tag aws-sdk-kotlin-tutorial:latest \
    <account-id>.dkr.ecr.us-east-1.amazonaws.com/aws-sdk-kotlin-tutorial:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/aws-sdk-kotlin-tutorial:latest
```

#### Create ECS Task Definition

**`ecs-task-definition.json`:**
```json
{
  "family": "aws-sdk-kotlin-tutorial",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "taskRoleArn": "arn:aws:iam::<account-id>:role/ECS-Task-Role",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "kotlin-app",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/aws-sdk-kotlin-tutorial:latest",
      "essential": true,
      "environment": [
        {
          "name": "AWS_REGION",
          "value": "us-east-1"
        },
        {
          "name": "USE_LOCALSTACK",
          "value": "false"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/aws-sdk-kotlin-tutorial",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### Deploy to ECS

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json

# Create or update service
aws ecs create-service \
    --cluster my-cluster \
    --service-name kotlin-app \
    --task-definition aws-sdk-kotlin-tutorial \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-12345],securityGroups=[sg-12345]}"
```

---

### Scenario 4: AWS Lambda

**Best for:** Event-driven, serverless applications

#### Create Lambda Handler

**`src/main/kotlin/com/awssdk/tutorial/lambda/S3EventHandler.kt`:**

```kotlin
package com.awssdk.tutorial.lambda

import aws.lambda.powertools.logging.Logging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

class S3EventHandler : RequestHandler<S3Event, String> {

    @Logging
    override fun handleRequest(event: S3Event, context: Context): String = runBlocking {
        val logger = context.logger

        logger.log("Processing S3 event: ${event.records.size} records")

        AwsClientFactory.createS3Client(forceLocalStack = false).use { s3Client ->
            event.records.forEach { record ->
                val bucket = record.s3.bucket.name
                val key = record.s3.`object`.key

                logger.log("Processing: s3://$bucket/$key")

                // Your logic here
                val response = s3Client.getObject {
                    this.bucket = bucket
                    this.key = key
                }

                logger.log("Object size: ${response.contentLength} bytes")
            }
        }

        "Success"
    }
}
```

#### Build Lambda Deployment Package

**`build.gradle.kts` (add):**
```kotlin
tasks.register<Zip>("buildLambdaZip") {
    from(tasks.compileKotlin)
    from(configurations.runtimeClasspath) {
        into("lib")
    }
    archiveFileName.set("lambda-function.zip")
}
```

```bash
# Build deployment package
gradle buildLambdaZip

# Deploy to Lambda
aws lambda create-function \
    --function-name s3-event-processor \
    --runtime java17 \
    --role arn:aws:iam::<account-id>:role/lambda-execution-role \
    --handler com.awssdk.tutorial.lambda.S3EventHandler::handleRequest \
    --zip-file fileb://build/distributions/lambda-function.zip \
    --timeout 30 \
    --memory-size 512
```

---

## 🔒 Security Best Practices

### 1. Never Hardcode Credentials

```kotlin
// ❌ BAD - Never do this!
val credentials = StaticCredentialsProvider {
    accessKeyId = "AKIAIOSFODNN7EXAMPLE"
    secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLE"
}

// ✅ GOOD - Use default credential chain
S3Client {
    region = "us-east-1"
    // SDK automatically finds credentials from:
    // 1. Environment variables
    // 2. Credentials file
    // 3. IAM role (EC2/ECS/Lambda)
}
```

### 2. Use IAM Roles Instead of Access Keys

**Order of preference:**

1. **IAM Roles** (EC2, ECS, Lambda) - ✅ Best
2. **AWS SSO** - ✅ Good for corporate
3. **IAM User credentials** in `~/.aws/credentials` - ⚠️ OK for local dev
4. **Environment variables** - ⚠️ OK for CI/CD
5. **Hardcoded credentials** - ❌ Never!

### 3. Use Least Privilege

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-specific-bucket/*"
    }
  ]
}
```

### 4. Rotate Credentials Regularly

```bash
# Create new access key
aws iam create-access-key --user-name dev-user

# Update credentials
aws configure

# Delete old access key
aws iam delete-access-key --access-key-id OLD_KEY_ID --user-name dev-user
```

### 5. Enable MFA for Sensitive Operations

```kotlin
// Require MFA for delete operations
{
  "Effect": "Allow",
  "Action": "s3:DeleteBucket",
  "Resource": "*",
  "Condition": {
    "Bool": {
      "aws:MultiFactorAuthPresent": "true"
    }
  }
}
```

### 6. Use Secrets Manager for Application Secrets

```kotlin
import aws.sdk.kotlin.services.secretsmanager.SecretsManagerClient

suspend fun getSecret(secretName: String): String {
    SecretsManagerClient { region = "us-east-1" }.use { client ->
        val response = client.getSecretValue {
            secretId = secretName
        }
        return response.secretString ?: ""
    }
}
```

---

## 💰 Cost Management

### Understanding AWS Costs

| Service | Free Tier | After Free Tier |
|---------|-----------|-----------------|
| **S3** | 5 GB storage, 20,000 GET, 2,000 PUT | $0.023/GB/month |
| **DynamoDB** | 25 GB storage, 25 RCU, 25 WCU | $0.25/GB/month + request costs |
| **SQS** | 1M requests | $0.40/million requests |
| **SNS** | 1M publishes | $0.50/million requests |
| **Lambda** | 1M requests, 400,000 GB-seconds | $0.20/million requests |

### Cost Optimization Tips

#### 1. **Use Free Tier Wisely**

```bash
# Monitor Free Tier usage
# AWS Console → Billing → Free Tier
```

#### 2. **Delete Unused Resources**

```bash
# Delete S3 bucket
aws s3 rb s3://my-test-bucket --force

# Delete DynamoDB table
aws dynamodb delete-table --table-name Users

# Delete SQS queue
aws sqs delete-queue --queue-url <queue-url>
```

#### 3. **Set Billing Alarms**

```bash
# Create billing alarm
aws cloudwatch put-metric-alarm \
    --alarm-name billing-alarm \
    --alarm-description "Alert when bill exceeds $10" \
    --metric-name EstimatedCharges \
    --namespace AWS/Billing \
    --statistic Maximum \
    --period 21600 \
    --evaluation-periods 1 \
    --threshold 10.0 \
    --comparison-operator GreaterThanThreshold
```

#### 4. **Use DynamoDB On-Demand for Development**

```kotlin
// On-demand billing (pay per request)
dynamoDb.createTable {
    tableName = "Users"
    billingMode = BillingMode.PayPerRequest  // No upfront capacity
    // ...
}
```

#### 5. **Enable S3 Lifecycle Policies**

```bash
# Auto-delete objects after 30 days
aws s3api put-bucket-lifecycle-configuration \
    --bucket my-test-bucket \
    --lifecycle-configuration '{
      "Rules": [{
        "Id": "Delete old objects",
        "Status": "Enabled",
        "ExpirationInDays": 30
      }]
    }'
```

---

## 📊 Monitoring and Logging

### CloudWatch Logs

```kotlin
// Enable SDK logging to CloudWatch
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("AwsApp")

suspend fun processFile() {
    logger.info("Starting file processing")
    try {
        // Your code
        logger.info("File processed successfully")
    } catch (e: Exception) {
        logger.error("Error processing file", e)
    }
}
```

### AWS X-Ray for Tracing

**`build.gradle.kts` (add):**
```kotlin
dependencies {
    implementation("aws.sdk.kotlin:xray:1.0.57")
}
```

```kotlin
import aws.sdk.kotlin.services.xray.XRayClient

// X-Ray will automatically trace AWS SDK calls
```

### CloudWatch Metrics

```bash
# View S3 metrics
aws cloudwatch get-metric-statistics \
    --namespace AWS/S3 \
    --metric-name NumberOfObjects \
    --dimensions Name=BucketName,Value=my-bucket \
    --start-time 2025-01-01T00:00:00Z \
    --end-time 2025-01-31T23:59:59Z \
    --period 86400 \
    --statistics Average
```

---

## 🎯 Quick Reference: LocalStack → AWS Transition

| Task | LocalStack | Real AWS |
|------|-----------|----------|
| **Endpoint** | `http://localstack:4566` | Auto (AWS region) |
| **Credentials** | `test` / `test` | AWS IAM credentials |
| **Environment** | `USE_LOCALSTACK=true` | `USE_LOCALSTACK=false` |
| **Code Change** | `createS3Client()` | `createS3Client(forceLocalStack=false)` |
| **Cost** | Free | Pay per use |
| **Setup** | `docker-compose up` | `aws configure` |

---

## ✅ Checklist: Moving to Production

- [ ] AWS account created and secured with MFA
- [ ] IAM roles created with least privilege
- [ ] Credentials configured (no hardcoded keys!)
- [ ] Code tested against real AWS
- [ ] Billing alarms set up
- [ ] CloudWatch logging enabled
- [ ] Error handling implemented
- [ ] Cleanup scripts for dev resources
- [ ] Security review completed
- [ ] Cost estimation done

---

## 🚀 Next Steps

1. **Start Testing**: Use Method 1 (Local Dev with Real AWS)
2. **Learn Deployment**: Try EC2 or ECS deployment
3. **Optimize**: Implement cost and security best practices
4. **Monitor**: Set up CloudWatch and billing alarms

---

**Ready to deploy?** 🚀

Continue to: **[Environment Transition Guide](./TRANSITION-GUIDE.md)** for step-by-step migration from LocalStack to AWS.
