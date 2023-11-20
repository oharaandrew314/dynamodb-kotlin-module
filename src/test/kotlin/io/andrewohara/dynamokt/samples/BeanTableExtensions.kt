package io.andrewohara.dynamokt.samples

import io.andrewohara.dynamokt.DataClassTableSchema
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.http4k.aws.AwsSdkClient
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedTimestampRecordExtension
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAutoGeneratedTimestampAttribute
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class BeanTableExtensions {

    private val time = Instant.parse("2023-11-19T12:00:00Z")
    private val clock = object: Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = TODO()
        override fun instant() = time
    }

    private val dynamo = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder()
                .httpClient(AwsSdkClient(FakeDynamoDb()))
                .credentialsProvider { AwsBasicCredentials.create("key", "id") }
                .region(Region.CA_CENTRAL_1)
                .build()
        )
        .extensions(
            AutoGeneratedTimestampRecordExtension.builder().baseClock(clock).build(),
            VersionedRecordExtension.builder().build()
        )
        .build()

    @Test
    fun `auto generated timestamp`() {
        data class Person(
            @DynamoKtPartitionKey
            val name: String,

            @get:DynamoDbAutoGeneratedTimestampAttribute
            var dob: Instant? = null
        )

        val table = dynamo
            .table("people", DataClassTableSchema(Person::class))
            .also { it.createTable() }

        table.putItem(Person("John"))

        table.getItem(Key.builder().partitionValue("John").build())
            .shouldNotBeNull()
            .dob shouldBe time
    }

    @Test
    fun `version attribute`() {
        data class Person(
            @DynamoKtPartitionKey
            val id: Int,

            val name: String,

            @get:DynamoDbVersionAttribute
            val version: Int = 0
        )

        val table = dynamo
            .table("people", DataClassTableSchema(Person::class))
            .also { it.createTable() }

        // increment version on insert
        table.putItem(Person(1337,"John", 0))
        table.getItem(Key.builder().partitionValue(1337).build())
            .shouldNotBeNull()
            .version shouldBe 1

        // increment version on update
        table.putItem(Person(1337, "Jim", 1))
        table.getItem(Key.builder().partitionValue(1337).build())
            .shouldNotBeNull()
            .version shouldBe 2
    }
}