package io.andrewohara.dynamokt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.lang.reflect.InvocationTargetException
import java.net.URI

private const val TABLE_NAME = "INSERT_NAME_OF_YOUR_TABLE_HERE"

@Testcontainers
class GH9Test {

    @Container
    val dynamo: GenericContainer<*> = GenericContainer(DockerImageName.parse("amazon/dynamodb-local:1.15.0"))
        .withExposedPorts(8000)

    private val enhancedClient by lazy {
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(
                DynamoDbClient.builder()
                    .endpointOverride(URI("http://localhost:${dynamo.getMappedPort(8000)}"))
                    .credentialsProvider { AwsBasicCredentials.create("key", "id") }
                    .region(Region.CA_CENTRAL_1)
                    .build()
            )
            .build()
    }

    @Test
    fun `update missing item`() {
        val testClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))
            .also { it.createTable() }

        val item = TestEntityV1(id = "test-id", name = null)
        testClient.updateItem(item)

        testClient.getItem(GetItemEnhancedRequest.builder()
            .consistentRead(true)
            .key(Key.builder().partitionValue("test-id").build())
            .build()) shouldBe item
    }

    @Test
    fun `mapping error on scan`() {
        val legacyClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV1::class))
            .also { it.createTable() }

        legacyClient.putItem(TestEntityV1(id = "valid", name = "valid name")).shouldNotBeNull()
        legacyClient.putItem(TestEntityV1(id = "invalid", name = null)).shouldNotBeNull()

        val newClient = enhancedClient
            .table(TABLE_NAME, DataClassTableSchema(TestEntityV2::class))

        shouldThrow<InvocationTargetException> {
            newClient.scan().count()
        }
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