#!/bin/bash

# Helper script to run Kotlin examples easily
# Usage: ./scripts/run-example.sh part1.ListS3Buckets

if [ -z "$1" ]; then
    echo "❌ Error: Please provide an example name"
    echo ""
    echo "Usage: ./scripts/run-example.sh <ExampleName>"
    echo ""
    echo "Examples:"
    echo "  ./scripts/run-example.sh part1.ListS3Buckets"
    echo "  ./scripts/run-example.sh part1.CreateS3Bucket"
    echo "  ./scripts/run-example.sh part2.s3.UploadFile"
    echo "  ./scripts/run-example.sh part2.dynamodb.CreateTable"
    echo ""
    exit 1
fi

EXAMPLE=$1
MAIN_CLASS="com.awssdk.tutorial.${EXAMPLE}Kt"

echo "🚀 Running example: $EXAMPLE"
echo "=================================================="

docker-compose exec kotlin-app gradle run -PmainClass=$MAIN_CLASS
