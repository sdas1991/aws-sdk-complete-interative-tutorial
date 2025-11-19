package com.awssdk.tutorial.common

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.lambda.LambdaClient
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Factory for creating AWS service clients
 * Supports both LocalStack (for local development) and real AWS
 */
object AwsClientFactory {

    private val region = System.getenv("AWS_REGION") ?: "us-east-1"
    private val endpointUrl = System.getenv("AWS_ENDPOINT_URL") ?: "http://localstack:4566"
    private val useLocalStack = System.getenv("USE_LOCALSTACK")?.toBoolean() ?: true

    /**
     * Creates an S3 client configured for LocalStack or AWS
     */
    fun createS3Client(forceLocalStack: Boolean? = null): S3Client {
        val useLocal = forceLocalStack ?: useLocalStack
        return S3Client {
            region = this@AwsClientFactory.region

            if (useLocal) {
                endpointUrl = Url.parse(this@AwsClientFactory.endpointUrl)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }
                forcePathStyle = true
            }
        }
    }

    /**
     * Creates a DynamoDB client configured for LocalStack or AWS
     */
    fun createDynamoDbClient(forceLocalStack: Boolean? = null): DynamoDbClient {
        val useLocal = forceLocalStack ?: useLocalStack
        return DynamoDbClient {
            region = this@AwsClientFactory.region

            if (useLocal) {
                endpointUrl = Url.parse(this@AwsClientFactory.endpointUrl)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }
            }
        }
    }

    /**
     * Creates an SQS client configured for LocalStack or AWS
     */
    fun createSqsClient(forceLocalStack: Boolean? = null): SqsClient {
        val useLocal = forceLocalStack ?: useLocalStack
        return SqsClient {
            region = this@AwsClientFactory.region

            if (useLocal) {
                endpointUrl = Url.parse(this@AwsClientFactory.endpointUrl)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }
            }
        }
    }

    /**
     * Creates an SNS client configured for LocalStack or AWS
     */
    fun createSnsClient(forceLocalStack: Boolean? = null): SnsClient {
        val useLocal = forceLocalStack ?: useLocalStack
        return SnsClient {
            region = this@AwsClientFactory.region

            if (useLocal) {
                endpointUrl = Url.parse(this@AwsClientFactory.endpointUrl)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }
            }
        }
    }

    /**
     * Creates a Lambda client configured for LocalStack or AWS
     */
    fun createLambdaClient(forceLocalStack: Boolean? = null): LambdaClient {
        val useLocal = forceLocalStack ?: useLocalStack
        return LambdaClient {
            region = this@AwsClientFactory.region

            if (useLocal) {
                endpointUrl = Url.parse(this@AwsClientFactory.endpointUrl)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = "test"
                    secretAccessKey = "test"
                }
            }
        }
    }

    /**
     * Gets the configured endpoint URL
     */
    fun getEndpointUrl(): String = endpointUrl

    /**
     * Checks if using LocalStack
     */
    fun isUsingLocalStack(): Boolean = useLocalStack
}
