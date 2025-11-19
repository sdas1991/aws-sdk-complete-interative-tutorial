package com.awssdk.tutorial.part1

import aws.sdk.kotlin.services.s3.model.S3Exception
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

/**
 * Example 2: Create an S3 bucket
 *
 * This demonstrates:
 * - Creating a new S3 bucket
 * - Checking if bucket already exists
 * - Error handling for duplicate buckets
 */
fun main() = runBlocking {
    println("🪣 AWS SDK Kotlin - Create S3 Bucket Example")
    println("=".repeat(50))

    // Bucket name (must be globally unique and follow naming rules)
    val bucketName = "my-kotlin-tutorial-bucket-${System.currentTimeMillis()}"

    println("\n📝 Bucket name: $bucketName")

    try {
        AwsClientFactory.createS3Client().use { s3Client ->

            println("📡 Connecting to: ${AwsClientFactory.getEndpointUrl()}")
            println("🚀 Creating bucket...\n")

            // Create the bucket
            s3Client.createBucket {
                bucket = bucketName
            }

            println("✅ Bucket created successfully!")
            println("\n📊 Verifying bucket exists...")

            // List all buckets to verify
            val response = s3Client.listBuckets()
            val bucket = response.buckets?.find { it.name == bucketName }

            if (bucket != null) {
                println("✅ Verification successful!")
                println("   Bucket: ${bucket.name}")
                println("   Created: ${bucket.creationDate}")
            } else {
                println("⚠️  Bucket created but not found in list (eventual consistency)")
            }

            println("\n💡 Next step: Try uploading a file to this bucket!")
            println("   Bucket name: $bucketName")
        }

    } catch (e: S3Exception) {
        when (e.sdkErrorMetadata.errorCode) {
            "BucketAlreadyExists", "BucketAlreadyOwnedByYou" -> {
                println("⚠️  Bucket already exists: $bucketName")
                println("   Try running the program again (it generates a unique name)")
            }
            else -> {
                println("❌ S3 Error: ${e.message}")
                println("   Error Code: ${e.sdkErrorMetadata.errorCode}")
            }
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
