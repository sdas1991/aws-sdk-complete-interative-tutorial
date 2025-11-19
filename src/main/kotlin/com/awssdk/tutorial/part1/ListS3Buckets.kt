package com.awssdk.tutorial.part1

import aws.sdk.kotlin.services.s3.model.S3Exception
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

/**
 * Example 1: List all S3 buckets
 *
 * This demonstrates:
 * - Creating an S3 client
 * - Making an async API call
 * - Proper resource management with use {}
 * - Error handling
 */
fun main() = runBlocking {
    println("🪣 AWS SDK Kotlin - List S3 Buckets Example")
    println("=".repeat(50))

    try {
        // Create S3 client (will auto-close after use block)
        AwsClientFactory.createS3Client().use { s3Client ->

            println("\n📡 Connecting to: ${AwsClientFactory.getEndpointUrl()}")
            println("🔍 Listing S3 buckets...\n")

            // Call AWS API - listBuckets is a suspend function
            val response = s3Client.listBuckets()

            // Check if we have buckets
            val buckets = response.buckets

            if (buckets.isNullOrEmpty()) {
                println("📭 No buckets found")
                println("\n💡 TIP: Create a bucket first!")
                println("   Run the following command:")
                println("   docker-compose exec aws-cli aws --endpoint-url=http://localstack:4566 s3 mb s3://my-test-bucket")
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
        // AWS S3-specific errors
        println("❌ S3 Error: ${e.message}")
        println("   Error Code: ${e.sdkErrorMetadata.errorCode}")
    } catch (e: Exception) {
        // General errors (network, etc.)
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
