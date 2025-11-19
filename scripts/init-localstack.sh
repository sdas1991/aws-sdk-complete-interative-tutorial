#!/bin/bash

# Initialize LocalStack with sample resources
# This script creates sample buckets, tables, queues, etc.

set -e

echo "🚀 Initializing LocalStack with sample resources..."
echo "=================================================="

ENDPOINT="http://localstack:4566"

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to be ready..."
sleep 5

# Create S3 buckets
echo ""
echo "📦 Creating S3 buckets..."
aws --endpoint-url=$ENDPOINT s3 mb s3://tutorial-bucket-1 2>/dev/null || echo "  ⚠️  Bucket already exists: tutorial-bucket-1"
aws --endpoint-url=$ENDPOINT s3 mb s3://tutorial-bucket-2 2>/dev/null || echo "  ⚠️  Bucket already exists: tutorial-bucket-2"
echo "  ✅ S3 buckets created"

# Create DynamoDB table
echo ""
echo "🗄️  Creating DynamoDB tables..."
aws --endpoint-url=$ENDPOINT dynamodb create-table \
    --table-name Users \
    --attribute-definitions \
        AttributeName=userId,AttributeType=S \
    --key-schema \
        AttributeName=userId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    2>/dev/null || echo "  ⚠️  Table already exists: Users"
echo "  ✅ DynamoDB tables created"

# Create SQS queues
echo ""
echo "📬 Creating SQS queues..."
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name tutorial-queue 2>/dev/null || echo "  ⚠️  Queue already exists: tutorial-queue"
aws --endpoint-url=$ENDPOINT sqs create-queue --queue-name notification-queue 2>/dev/null || echo "  ⚠️  Queue already exists: notification-queue"
echo "  ✅ SQS queues created"

# Create SNS topics
echo ""
echo "📣 Creating SNS topics..."
aws --endpoint-url=$ENDPOINT sns create-topic --name user-events 2>/dev/null || echo "  ⚠️  Topic already exists: user-events"
aws --endpoint-url=$ENDPOINT sns create-topic --name system-notifications 2>/dev/null || echo "  ⚠️  Topic already exists: system-notifications"
echo "  ✅ SNS topics created"

echo ""
echo "=================================================="
echo "✅ LocalStack initialization complete!"
echo ""
echo "📊 Summary:"
echo "  - 2 S3 buckets created"
echo "  - 1 DynamoDB table created"
echo "  - 2 SQS queues created"
echo "  - 2 SNS topics created"
echo ""
echo "🎯 You're ready to start learning!"
echo "   Run: docker-compose exec kotlin-app gradle run"
echo "=================================================="
