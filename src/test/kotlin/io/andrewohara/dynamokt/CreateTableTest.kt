package io.andrewohara.dynamokt

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.http4k.aws.AwsSdkClient
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndexResponse
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.KeyType
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.ProjectionType
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Instant

class CreateTableTest {

    private data class Person(
        @DynamoKtPartitionKey val id: Int,
        @DynamoKtSecondaryPartitionKey(indexNames = ["names"]) val name: String,
        @DynamoKtSecondarySortKey(indexNames = ["names"]) val dob: Instant
    )

    private val storage = Storage.InMemory<DynamoTable>()

    private val personTable = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder()
                .httpClient(AwsSdkClient(FakeDynamoDb(storage)))
                .credentialsProvider { AwsBasicCredentials.create("key", "id") }
                .region(Region.CA_CENTRAL_1)
                .build()
        )
        .build()
        .table("people", DataClassTableSchema(Person::class))


    @Test
    fun createTable() {
        personTable.createTable()

        val table = storage["people"].shouldNotBeNull().table

        table.GlobalSecondaryIndexes.shouldContainExactlyInAnyOrder(
            GlobalSecondaryIndexResponse(
                IndexName = "names",
                KeySchema = listOf(
                    KeySchema(AttributeName.of("name"), KeyType.HASH),
                    KeySchema(AttributeName.of("dob"), KeyType.RANGE)
                ),
                Projection = Projection(ProjectionType = ProjectionType.ALL)
            )
        )

        table.LocalSecondaryIndexes.shouldBeNull()
    }
}