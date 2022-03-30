package io.andrewohara.dynamokt

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoAttribute
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoSchema
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

class CreateTableTest {

    private data class Person(
        @DynamoKtPartitionKey val id: Int,
        @DynamoKtSecondaryPartitionKey(indexNames = ["names"]) val name: String
    )

    private val backend = MockDynamoBackend()
    private val personTable = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2(backend))
        .build()
        .table("people", DataClassTableSchema(Person::class))

    @Test
    fun `original createTable will lose indices`() {
        personTable.createTable()

        val backendTable = backend.getTable("people").shouldNotBeNull()
        backendTable.globalIndices.shouldBeEmpty()
        backendTable.localIndices.shouldBeEmpty()
    }

    @Test
    fun `createTableWithIndices with indices will have indices intact`() {
        personTable.createTableWithIndices()

        val backendTable = backend.getTable("people").shouldNotBeNull()
        backendTable.globalIndices.shouldContainExactly(
            MockDynamoSchema("names", hashKey = MockDynamoAttribute(MockDynamoAttribute.Type.String, "name"), rangeKey = null)
        )
        backendTable.localIndices.shouldBeEmpty()
    }
}