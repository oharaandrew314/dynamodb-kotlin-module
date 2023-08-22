package io.andrewohara.dynamokt.samples

import io.andrewohara.dynamokt.*
import io.kotest.matchers.collections.shouldContainExactly
import org.http4k.aws.AwsSdkClient
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Instant

internal data class PersonById(
    @DynamoKtPartitionKey
    val id: Int,

    @DynamoKtSecondaryPartitionKey(indexNames = ["names"])
    val name: String,

    @DynamoKtSecondarySortKey(indexNames = ["names"])
    val dob: Instant
)

class SecondaryIndexSample {
    private val table = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder()
                .httpClient(AwsSdkClient(FakeDynamoDb()))
                .credentialsProvider { AwsBasicCredentials.create("id", "secret") }
                .region(Region.CA_CENTRAL_1)
                .build()
        )
        .build()
        .table("people", DataClassTableSchema(PersonById::class))

    @BeforeEach
    fun seed() {
        table.createTable()
        table.putItem(PersonById(1, "John", Instant.ofEpochSecond(9001)))
        table.putItem(PersonById(2, "Jane", Instant.ofEpochSecond(1337)))
        table.putItem(PersonById(3, "John", Instant.ofEpochSecond(4242)))
    }

    @Test
    fun `search index by name`() {
        val request = QueryEnhancedRequest.builder()
            .scanIndexForward(true)
            .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("John").build()))
            .build()

        table.index("names").query(request).flatMap { it.items() }.shouldContainExactly(
            PersonById(3, "John", Instant.ofEpochSecond(4242)),
            PersonById(1, "John", Instant.ofEpochSecond(9001))
        )
    }

    @Test
    fun `search index by name and dob`() {
        val condition = QueryConditional
            .keyEqualTo(Key.builder().partitionValue("John").sortValue(Instant.ofEpochSecond(4242).toString()).build())

        table.index("names").query(condition).flatMap { it.items() }.shouldContainExactly(
            PersonById(3, "John", Instant.ofEpochSecond(4242))
        )
    }
}