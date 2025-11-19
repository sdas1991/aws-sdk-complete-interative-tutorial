# ⚡ Quick Start Guide

Get started with AWS SDK for Kotlin in **under 5 minutes**!

---

## 🎯 Prerequisites

- ✅ Docker installed
- ✅ Docker Compose installed
- ✅ Terminal access

Check if you have them:
```bash
docker --version
docker-compose --version
```

---

## 🚀 Step 1: Start the Environment

```bash
# Start LocalStack and Kotlin dev environment
docker-compose up -d

# Wait for services to be ready (about 10 seconds)
sleep 10

# Verify LocalStack is running
docker-compose ps
```

You should see:
```
NAME                  STATUS
aws-cli-helper        running
aws-localstack        running
kotlin-aws-sdk-app    running
```

---

## 🎯 Step 2: Run Your First Example

```bash
# Enter the Kotlin development container
docker-compose exec kotlin-app bash

# Inside the container, run the main application
gradle run
```

You should see:
```
🎓 AWS SDK Kotlin Tutorial
==================================================

Welcome to your interactive AWS SDK learning environment!

To start learning, check out the lessons/ directory
or run specific examples from src/main/kotlin/com/awssdk/tutorial/

✅ Environment is ready!
```

---

## 🪣 Step 3: List S3 Buckets

```bash
# Still inside the container, run the S3 example
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

Expected output:
```
🪣 AWS SDK Kotlin - List S3 Buckets Example
==================================================

📡 Connecting to: http://localstack:4566
🔍 Listing S3 buckets...

📭 No buckets found

💡 TIP: Create a bucket first!
```

---

## 🎉 Step 4: Create Your First S3 Bucket

```bash
# Create a bucket using the AWS CLI container
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://my-first-bucket

# Or use the Kotlin example
gradle run -PmainClass=com.awssdk.tutorial.part1.CreateS3BucketKt
```

Now run the list command again:
```bash
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt
```

You should now see your bucket listed! 🎊

---

## 📚 Step 5: Start Learning

Exit the container and start with the tutorials:
```bash
# Exit the container
exit
```

**Choose your learning path:**

1. **[Part 1: Foundation →](./lessons/PART-1-FOUNDATION.md)**
   - Start here if you're new to AWS SDK

2. **[Part 2: S3 →](./lessons/PART-2-S3.md)**
   - Learn file storage operations

3. **[Part 2: DynamoDB →](./lessons/PART-2-DYNAMODB.md)**
   - Learn NoSQL database operations

---

## 🛑 Stopping the Environment

When you're done:

```bash
# Stop all services
docker-compose down

# Or stop and remove all data (clean slate)
docker-compose down -v
```

---

## 🔧 Useful Commands Cheat Sheet

```bash
# Start environment
docker-compose up -d

# View logs
docker-compose logs -f localstack

# Enter Kotlin container
docker-compose exec kotlin-app bash

# Run specific example (from inside container)
gradle run -PmainClass=com.awssdk.tutorial.part1.ListS3BucketsKt

# Use AWS CLI helper
docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 ls

# Stop everything
docker-compose down

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

---

## ❓ Having Issues?

### Issue: Port 4566 already in use
```bash
# Check what's using the port
lsof -i :4566

# Stop the process or change the port in docker-compose.yml
```

### Issue: Container won't start
```bash
# Check logs
docker-compose logs

# Rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Issue: "Connection refused"
```bash
# Make sure LocalStack is running
docker-compose ps

# Restart LocalStack
docker-compose restart localstack
```

---

## 🎓 Next Steps

**You're all set!** Now dive into the tutorials:

👉 **[Start Part 1: Foundation →](./lessons/PART-1-FOUNDATION.md)**

---

## 💡 Pro Tips

1. **Keep Docker running** - The environment needs to be running for examples to work
2. **Use long polling** - LocalStack takes a few seconds to start
3. **Check logs** - `docker-compose logs -f` is your friend
4. **Experiment** - Modify the examples and see what happens!

Happy learning! 🚀
