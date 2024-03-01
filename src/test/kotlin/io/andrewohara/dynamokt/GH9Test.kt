package io.andrewohara.dynamokt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.http4k.aws.AwsSdkAsyncClient
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import java.util.concurrent.ExecutionException

private const val TABLE_NAME = "INSERT_NAME_OF_YOUR_TABLE_HERE"

class GH9Test {

    private val enhancedClient by lazy {
        DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(
                DynamoDbAsyncClient.builder()
                    .httpClient(AwsSdkAsyncClient(FakeDynamoDb()))
                    .credentialsProvider { AwsBasicCredentials.create("key", "id") }
                    .region(Region.CA_CENTRAL_1)
                    .build()
            )
            .build()
    }

    @Test
    fun `get item with wrong schema`() {
        val legacyClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))
            .also { it.createTable().get() }

        val entity1 = TestEntityV1(id = "valid", name = "valid name")
        val entity2 = TestEntityV1(id = "invalid", name = null)

        legacyClient.putItem(entity1).shouldNotBeNull().get()
        legacyClient.putItem(entity2).shouldNotBeNull().get()

        val newClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV2::class))

        shouldThrow<ExecutionException> {
            newClient.getItem(Key.builder().partitionValue("invalid").build()).get()
        }.cause.shouldBeInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `get item from missing table`() {
        val testClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))

        shouldThrow<ExecutionException> {
            testClient.getItem(Key.builder().partitionValue("foo").build()).get()
        }.cause.shouldBeInstanceOf<ResourceNotFoundException>()
    }

    @Test
    fun `mapping error on scan`() {
        val legacyClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))
            .also { it.createTable().get() }

        val entity1 = TestEntityV1(id = "valid", name = "valid name")
        val entity2 = TestEntityV1(id = "invalid", name = null)

        legacyClient.putItem(entity1).shouldNotBeNull().get()
        legacyClient.putItem(entity2).shouldNotBeNull().get()

        val newClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV2::class))

        shouldThrow<ExecutionException> {
            newClient.scan().subscribe {
                it.items().shouldHaveSize(3)
                it.items().shouldContainExactlyInAnyOrder(entity1, entity2)
            }.get()
        }.cause.shouldBeInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `update missing item`() {
        val testClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))
            .also { it.createTable().get() }

        val item = TestEntityV1("id1", "foo")
        testClient.updateItem(item).get() shouldBe item
    }
}

data class TestEntityV2(
    @DynamoKtPartitionKey
    var id: String,
    val name: String,
    val otherField: String = "abc"
)

data class TestEntityV1(
    @DynamoKtPartitionKey
    var id: String,
    val name: String?,
    val otherField: String = "abc"
)