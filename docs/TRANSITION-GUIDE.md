# 🔄 Transition Guide: LocalStack → Real AWS

This step-by-step guide walks you through transitioning from LocalStack (local development) to real AWS services.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Pre-Transition Checklist](#pre-transition-checklist)
3. [Step-by-Step Transition](#step-by-step-transition)
4. [Service-Specific Migrations](#service-specific-migrations)
5. [Testing Strategy](#testing-strategy)
6. [Rollback Plan](#rollback-plan)
7. [Common Issues](#common-issues)

---

## 🎯 Overview

### Why Transition?

| Scenario | Use LocalStack | Use Real AWS |
|----------|---------------|--------------|
| **Learning basics** | ✅ | ❌ |
| **Unit testing** | ✅ | ❌ |
| **Integration testing** | ⚠️ | ✅ |
| **Performance testing** | ❌ | ✅ |
| **Production** | ❌ | ✅ |
| **Cost-free development** | ✅ | ❌ |

### Transition Phases

```
Phase 1: Setup          Phase 2: Parallel     Phase 3: Migration    Phase 4: Production
(1 hour)                (1-2 days)            (1 week)              (Ongoing)

┌─────────────┐        ┌─────────────┐        ┌─────────────┐       ┌─────────────┐
│ AWS Account │   →    │ Test on     │   →    │ Migrate     │  →    │ Run on      │
│ Credentials │        │ both envs   │        │ services    │       │ AWS only    │
│ IAM Setup   │        │ LocalStack  │        │ one by one  │       │ Monitor     │
└─────────────┘        │ + Real AWS  │        └─────────────┘       └─────────────┘
                       └─────────────┘
```

---

## ✅ Pre-Transition Checklist

Before starting, ensure you have:

### AWS Account Setup
- [ ] AWS account created
- [ ] Root account MFA enabled
- [ ] IAM user created for development
- [ ] Access keys downloaded and saved securely
- [ ] Billing alerts configured
- [ ] Budget set (e.g., $10/month for learning)

### Local Environment
- [ ] All LocalStack examples working
- [ ] Comfortable with AWS SDK concepts
- [ ] Code is in version control (Git)
- [ ] Backup of important data

### Knowledge
- [ ] Understand AWS Free Tier limits
- [ ] Know how to check AWS billing
- [ ] Familiar with IAM permissions
- [ ] Understand credential security

---

## 🚀 Step-by-Step Transition

### Step 1: Configure AWS Credentials (15 minutes)

#### 1.1 Install AWS CLI

```bash
# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify installation
aws --version
# Should output: aws-cli/2.x.x ...
```

#### 1.2 Configure Credentials

```bash
# Run configuration wizard
aws configure

# Enter your IAM user credentials:
# AWS Access Key ID: [Your Access Key]
# AWS Secret Access Key: [Your Secret Key]
# Default region name: us-east-1
# Default output format: json
```

#### 1.3 Test Credentials

```bash
# Test AWS CLI
aws sts get-caller-identity

# Expected output:
{
    "UserId": "AIDAI...",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/dev-user"
}

# If this works, your credentials are valid! ✅
```

---

### Step 2: Create Test Resources in AWS (20 minutes)

#### 2.1 Create S3 Bucket

```bash
# Create a bucket (name must be globally unique)
aws s3 mb s3://my-kotlin-tutorial-$(date +%s)

# Example: s3://my-kotlin-tutorial-1705920000

# Verify
aws s3 ls
```

#### 2.2 Create DynamoDB Table

```bash
# Create table
aws dynamodb create-table \
    --table-name Users-Test \
    --attribute-definitions AttributeName=userId,AttributeType=S \
    --key-schema AttributeName=userId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1

# Verify
aws dynamodb list-tables
```

#### 2.3 Create SQS Queue

```bash
# Create queue
aws sqs create-queue --queue-name tutorial-queue-test

# Get queue URL
aws sqs list-queues
```

#### 2.4 Create SNS Topic

```bash
# Create topic
aws sns create-topic --name tutorial-topic-test

# Get topic ARN
aws sns list-topics
```

---

### Step 3: Run First Example Against Real AWS (10 minutes)

#### 3.1 Update Environment Variable

```bash
# Tell the app to use real AWS (not LocalStack)
export USE_LOCALSTACK=false
export AWS_REGION=us-east-1
```

#### 3.2 Run ListS3Buckets Example

```bash
# Start your Docker environment
docker-compose up -d

# Enter Kotlin container
docker-compose exec kotlin-app bash

# Inside container, set environment
export USE_LOCALSTACK=false
export AWS_REGION=us-east-1

# Mount your AWS credentials into container
# Exit container first
exit

# Update docker-compose.yml to mount credentials
```

**Edit `docker-compose.yml`:**

```yaml
services:
  kotlin-app:
    # ... existing config ...
    volumes:
      - .:/app
      - gradle-cache:/root/.gradle
      - ~/.aws:/root/.aws:ro  # ← Add this line (read-only mount)
    environment:
      - USE_LOCALSTACK=false  # ← Change to false
      - AWS_REGION=us-east-1
      # Remove AWS_ENDPOINT_URL
      # Remove AWS_ACCESS_KEY_ID
      # Remove AWS_SECRET_ACCESS_KEY
```

**Restart container:**

```bash
docker-compose down
docker-compose up -d
```

#### 3.3 Test Connection

```bash
# Enter container
docker-compose exec kotlin-app bash

# Run example
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

**Expected output:**
```
🪣 AWS SDK Kotlin - List S3 Buckets Example
==================================================

📡 Connecting to: Real AWS
🌍 Region: us-east-1
🔍 Listing S3 buckets...

✅ Found 1 bucket(s):

1. my-kotlin-tutorial-1705920000
   Created: 2025-01-22T10:30:00Z

✅ Success!
```

**🎉 If you see this, you're now connected to real AWS!**

---

### Step 4: Parallel Testing (Ongoing)

Run examples on both LocalStack and AWS to compare behavior.

#### 4.1 Create Helper Script

**`scripts/test-both-environments.sh`:**

```bash
#!/bin/bash

EXAMPLE=$1

if [ -z "$EXAMPLE" ]; then
    echo "Usage: ./test-both-environments.sh <ExampleClass>"
    exit 1
fi

echo "Testing: $EXAMPLE"
echo "================================"

echo ""
echo "🐳 Testing on LocalStack..."
echo "----------------------------"
export USE_LOCALSTACK=true
gradle run -PmainClass=com.awssdk.tutorial.$EXAMPLE

echo ""
echo "☁️  Testing on Real AWS..."
echo "----------------------------"
export USE_LOCALSTACK=false
gradle run -PmainClass=com.awssdk.tutorial.$EXAMPLE

echo ""
echo "✅ Tests complete!"
```

#### 4.2 Use the Script

```bash
chmod +x scripts/test-both-environments.sh

# Test ListS3Buckets on both
./scripts/test-both-environments.sh part1.ListS3BucketsKt
```

---

### Step 5: Migrate Services One by One

#### Service Migration Order (Recommended)

1. **S3** - Simple, no state
2. **DynamoDB** - Requires data migration
3. **SQS/SNS** - Messaging patterns
4. **Lambda** - Function deployment
5. **Secrets Manager** - Credential management

---

## 📦 Service-Specific Migrations

### Migrating S3

#### Challenges
- ✅ No schema changes needed
- ✅ No data migration (unless you want to copy files)
- ⚠️ Bucket names must be globally unique

#### Steps

```bash
# 1. Create bucket in AWS
BUCKET_NAME="my-tutorial-bucket-$(date +%s)"
aws s3 mb s3://$BUCKET_NAME

# 2. Update code to use real AWS
export USE_LOCALSTACK=false

# 3. Test upload
docker-compose exec kotlin-app gradle run \
    -PmainClass=com.awssdk.tutorial.part2.s3.UploadFileKt

# 4. Verify in AWS Console
aws s3 ls s3://$BUCKET_NAME

# 5. (Optional) Copy data from LocalStack to AWS
# Not applicable since LocalStack data is temporary
```

#### Verification

```bash
# List buckets
aws s3 ls

# List objects in bucket
aws s3 ls s3://$BUCKET_NAME

# Download test file
aws s3 cp s3://$BUCKET_NAME/test-files/hello.txt /tmp/test.txt
cat /tmp/test.txt
```

---

### Migrating DynamoDB

#### Challenges
- ⚠️ Need to migrate existing data
- ⚠️ Table schemas must match
- ⚠️ Different throughput models

#### Steps

```bash
# 1. Create table in AWS
aws dynamodb create-table \
    --table-name Users \
    --attribute-definitions AttributeName=userId,AttributeType=S \
    --key-schema AttributeName=userId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST

# 2. Export data from LocalStack
docker-compose exec aws-cli \
    aws --endpoint-url=http://localstack:4566 \
    dynamodb scan --table-name Users > /tmp/users-export.json

# 3. Transform and import to AWS (manual process)
# See script below

# 4. Update code
export USE_LOCALSTACK=false

# 5. Test
docker-compose exec kotlin-app gradle run \
    -PmainClass=com.awssdk.tutorial.part2.dynamodb.GetItemKt
```

#### Data Migration Script

**`scripts/migrate-dynamodb-data.sh`:**

```bash
#!/bin/bash

SOURCE_TABLE="Users"
DEST_TABLE="Users"
ENDPOINT="http://localhost:4566"

# Scan all items from LocalStack
aws --endpoint-url=$ENDPOINT dynamodb scan --table-name $SOURCE_TABLE \
    --output json > /tmp/export.json

# Parse and import each item to AWS
cat /tmp/export.json | jq -r '.Items[]' | while read -r item; do
    aws dynamodb put-item \
        --table-name $DEST_TABLE \
        --item "$item"
done

echo "✅ Migration complete!"
```

---

### Migrating SQS/SNS

#### Challenges
- ✅ No data to migrate (messages are ephemeral)
- ✅ Just recreate queues/topics
- ⚠️ Update queue URLs and topic ARNs in code

#### Steps

```bash
# 1. Create queue in AWS
QUEUE_URL=$(aws sqs create-queue --queue-name tutorial-queue --output text)

echo "Queue URL: $QUEUE_URL"
# Example: https://sqs.us-east-1.amazonaws.com/123456789012/tutorial-queue

# 2. Create topic in AWS
TOPIC_ARN=$(aws sns create-topic --name tutorial-topic --output text)

echo "Topic ARN: $TOPIC_ARN"
# Example: arn:aws:sns:us-east-1:123456789012:tutorial-topic

# 3. Update code with new URLs/ARNs (if hardcoded)
# Better: Make them configurable via environment variables
```

#### Make Queue URL Configurable

**Update code:**

```kotlin
// Before (hardcoded for LocalStack)
val queueUrl = "http://localstack:4566/000000000000/my-kotlin-queue"

// After (configurable)
val queueUrl = System.getenv("SQS_QUEUE_URL") ?: run {
    // Discover queue URL dynamically
    sqsClient.getQueueUrl {
        queueName = "tutorial-queue"
    }.queueUrl
}
```

**Set environment variable:**

```bash
export SQS_QUEUE_URL="https://sqs.us-east-1.amazonaws.com/123456789012/tutorial-queue"
```

---

## 🧪 Testing Strategy

### Test Matrix

| Test Type | LocalStack | Real AWS | Production AWS |
|-----------|-----------|----------|----------------|
| **Unit Tests** | ✅ | ❌ | ❌ |
| **Integration Tests** | ✅ | ✅ | ❌ |
| **E2E Tests** | ⚠️ | ✅ | ⚠️ |
| **Performance Tests** | ❌ | ✅ | ✅ |
| **Security Tests** | ❌ | ✅ | ✅ |

### Automated Testing

**`src/test/kotlin/com/awssdk/tutorial/S3IntegrationTest.kt`:**

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

class S3IntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "USE_LOCALSTACK", matches = "true")
    fun `test S3 operations on LocalStack`() {
        // Test using LocalStack
        val s3Client = AwsClientFactory.createS3Client(forceLocalStack = true)
        // ... test logic
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_REAL_AWS", matches = "true")
    fun `test S3 operations on real AWS`() {
        // Test using real AWS
        val s3Client = AwsClientFactory.createS3Client(forceLocalStack = false)
        // ... test logic
    }
}
```

**Run tests:**

```bash
# Test on LocalStack
USE_LOCALSTACK=true gradle test

# Test on real AWS
TEST_REAL_AWS=true USE_LOCALSTACK=false gradle test
```

---

## 🔙 Rollback Plan

If something goes wrong, quickly revert to LocalStack:

### Emergency Rollback

```bash
# 1. Update docker-compose.yml
# Change:
#   USE_LOCALSTACK=false
# To:
#   USE_LOCALSTACK=true

# 2. Restore LocalStack endpoint
#   AWS_ENDPOINT_URL=http://localstack:4566

# 3. Restart containers
docker-compose down
docker-compose up -d

# 4. Verify
docker-compose exec kotlin-app gradle run
```

### Preserve AWS Resources

```bash
# Don't delete AWS resources immediately
# Keep them for reference and debugging

# Tag resources as "test"
aws s3api put-bucket-tagging \
    --bucket my-bucket \
    --tagging 'TagSet=[{Key=Environment,Value=Test}]'
```

---

## ⚠️ Common Issues

### Issue 1: Credentials Not Found

**Symptom:**
```
Unable to load credentials from any of the providers in the chain
```

**Solution:**
```bash
# Verify credentials file exists
cat ~/.aws/credentials

# Verify credentials in Docker container
docker-compose exec kotlin-app cat /root/.aws/credentials

# Check if volume is mounted
docker-compose exec kotlin-app ls -la /root/.aws/

# If not mounted, update docker-compose.yml:
volumes:
  - ~/.aws:/root/.aws:ro
```

### Issue 2: Region Errors

**Symptom:**
```
The bucket is in this region: us-west-2.
Please use this region to retry the request
```

**Solution:**
```bash
# Check bucket region
aws s3api get-bucket-location --bucket my-bucket

# Set correct region
export AWS_REGION=us-west-2

# Or specify in client
S3Client {
    region = "us-west-2"
}
```

### Issue 3: Permission Denied

**Symptom:**
```
Access Denied (Service: S3, Status Code: 403)
```

**Solution:**
```bash
# Check IAM user permissions
aws iam list-attached-user-policies --user-name dev-user

# Attach required policy
aws iam attach-user-policy \
    --user-name dev-user \
    --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

### Issue 4: Billing Shock

**Symptom:**
- Unexpected AWS charges

**Prevention:**
```bash
# Set up billing alarm BEFORE transitioning
aws cloudwatch put-metric-alarm \
    --alarm-name high-billing \
    --alarm-description "Alert when bill > $10" \
    --metric-name EstimatedCharges \
    --namespace AWS/Billing \
    --statistic Maximum \
    --period 21600 \
    --evaluation-periods 1 \
    --threshold 10.0 \
    --comparison-operator GreaterThanThreshold

# Check current charges
aws ce get-cost-and-usage \
    --time-period Start=2025-01-01,End=2025-01-31 \
    --granularity MONTHLY \
    --metrics UnblendedCost
```

### Issue 5: Bucket Name Conflicts

**Symptom:**
```
BucketAlreadyExists: The requested bucket name is not available
```

**Solution:**
```bash
# Use unique names
BUCKET="my-tutorial-$(date +%s)-$(uuidgen | head -c 8)"
aws s3 mb s3://$BUCKET
```

---

## ✅ Post-Transition Checklist

After successfully transitioning:

- [ ] All examples run on real AWS
- [ ] Billing alerts configured
- [ ] No hardcoded credentials in code
- [ ] IAM permissions follow least privilege
- [ ] Resources tagged appropriately
- [ ] Cleanup script created
- [ ] Documentation updated
- [ ] Team notified (if applicable)
- [ ] LocalStack still works (for new features)
- [ ] Cost monitoring dashboard set up

---

## 🧹 Cleanup After Learning

When done with AWS:

```bash
# Delete S3 buckets
aws s3 rb s3://my-bucket --force

# Delete DynamoDB tables
aws dynamodb delete-table --table-name Users

# Delete SQS queues
aws sqs delete-queue --queue-url <queue-url>

# Delete SNS topics
aws sns delete-topic --topic-arn <topic-arn>

# Verify no resources remain
aws resourcegroupstaggingapi get-resources
```

---

## 🎯 Success Criteria

You've successfully transitioned when:

1. ✅ All code runs on both LocalStack and AWS with just env var changes
2. ✅ No credentials are hardcoded
3. ✅ Billing is under control
4. ✅ You can switch between environments easily
5. ✅ Tests pass on both environments

---

## 📚 Summary

**Transition Process:**
1. Set up AWS account and credentials
2. Create test resources in AWS
3. Update environment variables
4. Run examples on real AWS
5. Parallel test both environments
6. Migrate services one by one
7. Monitor costs and performance

**Key Principles:**
- Start small (one service at a time)
- Test thoroughly before full migration
- Keep LocalStack for development
- Monitor costs from day one
- Use IAM roles over access keys

---

**Ready for production?** 🚀

Continue to: **[AWS Deployment Guide](./AWS-DEPLOYMENT-GUIDE.md)** for production deployment scenarios.
