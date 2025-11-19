# 🗄️ PART 2 — DynamoDB (NoSQL Database)

Welcome to **DynamoDB**! Learn AWS's fully managed NoSQL database that scales to any workload.

---

## 🎯 Learning Objectives

✅ Create DynamoDB tables
✅ PutItem - Insert data
✅ GetItem - Retrieve data
✅ UpdateItem - Modify data
✅ DeleteItem - Remove data
✅ Query vs Scan
✅ Handle pagination
✅ Use Global Secondary Indexes (GSI)
✅ Batch operations
✅ Build project: **User Profile CRUD Microservice**

---

## 📚 Lesson 2.2.1: DynamoDB Basics

### What is DynamoDB?

**Amazon DynamoDB** is:
- **NoSQL** - Key-value and document database
- **Serverless** - No servers to manage
- **Scalable** - Handles millions of requests/second
- **Fast** - Single-digit millisecond latency
- **Flexible** - Schema-less design

### Key Concepts

```
Table: Users
┌──────────────┬──────────────┬────────────────────┐
│ Partition Key│  Sort Key    │   Attributes       │
│ (userId)     │  (optional)  │   (flexible)       │
├──────────────┼──────────────┼────────────────────┤
│ user-123     │      -       │ {name, email, ...} │
│ user-456     │      -       │ {name, email, ...} │
└──────────────┴──────────────┴────────────────────┘

Table: OrdersWithSortKey
┌──────────────┬──────────────┬────────────────────┐
│ customerId   │  orderDate   │   Attributes       │
│ (PK)         │  (SK)        │   (flexible)       │
├──────────────┼──────────────┼────────────────────┤
│ cust-123     │ 2025-01-15   │ {total, items}     │
│ cust-123     │ 2025-01-20   │ {total, items}     │
│ cust-456     │ 2025-01-18   │ {total, items}     │
└──────────────┴──────────────┴────────────────────┘
```

### Primary Key Types

| Type | Description | Use Case |
|------|-------------|----------|
| **Partition Key Only** | Single attribute (hash key) | Simple key-value (userId → User) |
| **Partition + Sort Key** | Composite key | One-to-many (customerId + orderDate) |

---

## 🏃 Exercise 1: Create a Table

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/dynamodb/CreateTable.kt`

```kotlin
package com.awssdk.tutorial.part2.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.*
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val tableName = "Users"

    println("🗄️  Creating DynamoDB Table")
    println("=".repeat(50))

    try {
        AwsClientFactory.createDynamoDbClient().use { dynamoDb ->

            // Create table
            dynamoDb.createTable {
                this.tableName = tableName

                // Define key schema
                keySchema = listOf(
                    KeySchemaElement {
                        attributeName = "userId"
                        keyType = KeyType.Hash  // Partition key
                    }
                )

                // Define attribute definitions
                attributeDefinitions = listOf(
                    AttributeDefinition {
                        attributeName = "userId"
                        attributeType = ScalarAttributeType.S  // String
                    }
                )

                // Billing mode
                billingMode = BillingMode.PayPerRequest  // On-demand

                // Note: For provisioned mode, use:
                // billingMode = BillingMode.Provisioned
                // provisionedThroughput {
                //     readCapacityUnits = 5
                //     writeCapacityUnits = 5
                // }
            }

            println("✅ Table '$tableName' created successfully!")

            // Wait for table to be active
            println("⏳ Waiting for table to become active...")
            dynamoDb.waitUntilTableExists {
                this.tableName = tableName
            }

            println("✅ Table is now active!")

            // Describe table
            val description = dynamoDb.describeTable {
                this.tableName = tableName
            }

            println("\n📊 Table Details:")
            println("   Name: ${description.table?.tableName}")
            println("   Status: ${description.table?.tableStatus}")
            println("   Item Count: ${description.table?.itemCount}")
            println("   Size: ${description.table?.tableSizeBytes} bytes")
        }

    } catch (e: DynamoDbException) {
        if (e.message?.contains("Table already exists") == true) {
            println("⚠️  Table already exists: $tableName")
        } else {
            println("❌ Error: ${e.message}")
        }
    }
}
```

---

## 🏃 Exercise 2: Put Item (Insert)

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/dynamodb/PutItem.kt`

```kotlin
package com.awssdk.tutorial.part2.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val tableName = "Users"

    println("➕ Inserting item into DynamoDB")
    println("=".repeat(50))

    try {
        AwsClientFactory.createDynamoDbClient().use { dynamoDb ->

            val user = mapOf(
                "userId" to AttributeValue.S("user-12345"),
                "name" to AttributeValue.S("Alice Johnson"),
                "email" to AttributeValue.S("alice@example.com"),
                "age" to AttributeValue.N("28"),
                "isActive" to AttributeValue.Bool(true),
                "tags" to AttributeValue.L(
                    listOf(
                        AttributeValue.S("kotlin"),
                        AttributeValue.S("aws"),
                        AttributeValue.S("developer")
                    )
                )
            )

            dynamoDb.putItem {
                this.tableName = tableName
                item = user
            }

            println("✅ User created successfully!")
            println("\n📄 User Details:")
            println("   ID: user-12345")
            println("   Name: Alice Johnson")
            println("   Email: alice@example.com")
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 3: Get Item (Retrieve)

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/dynamodb/GetItem.kt`

```kotlin
package com.awssdk.tutorial.part2.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val tableName = "Users"
    val userId = "user-12345"

    println("🔍 Retrieving item from DynamoDB")
    println("=".repeat(50))

    try {
        AwsClientFactory.createDynamoDbClient().use { dynamoDb ->

            val response = dynamoDb.getItem {
                this.tableName = tableName
                key = mapOf(
                    "userId" to AttributeValue.S(userId)
                )
            }

            val item = response.item

            if (item.isNullOrEmpty()) {
                println("❌ User not found: $userId")
            } else {
                println("✅ User found!\n")
                println("📄 User Details:")
                println("   ID: ${item["userId"]?.asS()}")
                println("   Name: ${item["name"]?.asS()}")
                println("   Email: ${item["email"]?.asS()}")
                println("   Age: ${item["age"]?.asN()}")
                println("   Active: ${item["isActive"]?.asBool()}")

                // Print tags if they exist
                item["tags"]?.asL()?.let { tags ->
                    println("   Tags: ${tags.joinToString(", ") { it.asS() }}")
                }
            }
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 4: Update Item

**File:** `src/main/kotlin/com/awssdk/tutorial/part2/dynamodb/UpdateItem.kt`

```kotlin
package com.awssdk.tutorial.part2.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val tableName = "Users"
    val userId = "user-12345"

    println("🔄 Updating item in DynamoDB")
    println("=".repeat(50))

    try {
        AwsClientFactory.createDynamoDbClient().use { dynamoDb ->

            val response = dynamoDb.updateItem {
                this.tableName = tableName

                key = mapOf(
                    "userId" to AttributeValue.S(userId)
                )

                // Update expression
                updateExpression = "SET age = :newAge, #n = :newName, isActive = :active"

                // Expression attribute values
                expressionAttributeValues = mapOf(
                    ":newAge" to AttributeValue.N("29"),
                    ":newName" to AttributeValue.S("Alice M. Johnson"),
                    ":active" to AttributeValue.Bool(true)
                )

                // Expression attribute names (for reserved keywords)
                expressionAttributeNames = mapOf(
                    "#n" to "name"  // 'name' is a reserved word
                )

                // Return updated values
                returnValues = ReturnValue.AllNew
            }

            println("✅ User updated successfully!\n")
            println("📄 Updated Values:")
            response.attributes?.forEach { (key, value) ->
                when (value) {
                    is AttributeValue.S -> println("   $key: ${value.asS()}")
                    is AttributeValue.N -> println("   $key: ${value.asN()}")
                    is AttributeValue.Bool -> println("   $key: ${value.asBool()}")
                    else -> println("   $key: $value")
                }
            }
        }

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
    }
}
```

---

## 🏃 Exercise 5: Query vs Scan

### Query (Efficient - uses index)

```kotlin
package com.awssdk.tutorial.part2.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import com.awssdk.tutorial.common.AwsClientFactory
import kotlinx.coroutines.runBlocking

fun queryExample() = runBlocking {
    AwsClientFactory.createDynamoDbClient().use { dynamoDb ->
        val response = dynamoDb.query {
            tableName = "Orders"

            // Query by partition key
            keyConditionExpression = "customerId = :customerId AND orderDate > :date"

            expressionAttributeValues = mapOf(
                ":customerId" to AttributeValue.S("cust-123"),
                ":date" to AttributeValue.S("2025-01-01")
            )

            // Optional: limit results
            limit = 10
        }

        println("Found ${response.count} items")
        response.items?.forEach { item ->
            println(item)
        }
    }
}
```

### Scan (Expensive - reads entire table)

```kotlin
fun scanExample() = runBlocking {
    AwsClientFactory.createDynamoDbClient().use { dynamoDb ->
        val response = dynamoDb.scan {
            tableName = "Users"

            // Filter (still scans entire table!)
            filterExpression = "age > :minAge"

            expressionAttributeValues = mapOf(
                ":minAge" to AttributeValue.N("25")
            )
        }

        println("Scanned ${response.scannedCount} items")
        println("Filtered to ${response.count} items")
    }
}
```

**⚠️ Important:** Use Query whenever possible. Scan is expensive!

---

## 🎓 Mini Project: User Profile CRUD Service

Build a complete user management service:

```kotlin
// src/main/kotlin/com/awssdk/tutorial/part2/dynamodb/project/UserService.kt

class UserService(private val dynamoDb: DynamoDbClient) {
    suspend fun createUser(user: User): User { /* ... */ }
    suspend fun getUser(userId: String): User? { /* ... */ }
    suspend fun updateUser(userId: String, updates: Map<String, Any>): User { /* ... */ }
    suspend fun deleteUser(userId: String): Boolean { /* ... */ }
    suspend fun listUsers(limit: Int = 10): List<User> { /* ... */ }
}
```

Check `part2/dynamodb/project/` for full implementation!

---

## 🧪 Quiz

### Question 1
What's the difference between Query and Scan?
- A) No difference
- B) Query uses indexes, Scan reads entire table
- C) Scan is faster
- D) Query can't filter results

<details>
<summary>Answer</summary>

**B) Query uses indexes, Scan reads entire table**

Query is efficient and uses partition/sort keys. Scan reads every item and is expensive.
</details>

---

## ✅ Checkpoint

Can you:

- [ ] Create a DynamoDB table
- [ ] Insert items with PutItem
- [ ] Retrieve items with GetItem
- [ ] Update items with UpdateItem
- [ ] Explain Query vs Scan

---

## 🎯 Next Steps

1. **[Part 2: SQS/SNS →](./PART-2-MESSAGING.md)** - Messaging
2. **[Part 3: Advanced DynamoDB →](./PART-3-DYNAMODB-ADVANCED.md)** - GSI, LSI, Streams

**What do you want to learn next?** 🚀
