package io.andrewohara.dynamokt.samples

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.dynamokt.DataClassTableSchema
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.andrewohara.dynamokt.DynamoKtSortKey
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

internal data class Person(
    @DynamoKtPartitionKey
    val lastName: String,

    @DynamoKtSortKey
    val firstName: String
) {
    fun fullKey(): Key = Key.builder().partitionValue(lastName).sortValue(firstName).build()
    fun partialKey(): Key = Key.builder().partitionValue(lastName).build()
}

internal object People {
    val johnDoe = Person("Doe", "John")
    val janeDoe = Person("Doe", "Jane")
    val billSmith = Person("Smith", "Bill")
}

class PrimaryRangeKeySample {
    private val table = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2())
        .build()
        .table("people", DataClassTableSchema(Person::class))

    @BeforeEach
    fun seed() {
        table.createTable()
        table.putItem(People.johnDoe)
        table.putItem(People.janeDoe)
        table.putItem(People.billSmith)
    }

    @Test
    fun `get by full name`() {
        table.getItem(People.johnDoe.fullKey()) shouldBe People.johnDoe
    }

    @Test
    fun `query by last name`() {
        table.query(QueryConditional.keyEqualTo(People.johnDoe.partialKey()))
            .items()
            .shouldContainExactly(People.janeDoe, People.johnDoe)
    }
}