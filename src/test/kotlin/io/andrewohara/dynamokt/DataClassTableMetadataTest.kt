package io.andrewohara.dynamokt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

class DataClassTableMetadataTest {

    data class Person(
        @DynamoKtPartitionKey val id: Int,
        @DynamoKtSecondaryPartitionKey(["search", "names"]) val name: String,
        @DynamoKtSecondarySortKey(["search"]) val dob: Instant
    )

    private val metadata = DataClassTableMetadata(Person::class)

    @Test
    fun `partition key of primary index`() {
        metadata.indexPartitionKey(TableMetadata.primaryIndexName()) shouldBe "id"
    }

    @Test
    fun `partition key of secondary index`() {
        metadata.indexPartitionKey("search") shouldBe "name"
    }

    @Test
    fun `partition key of missing index`() {
        shouldThrow<IllegalArgumentException> {
            metadata.indexPartitionKey("foo")
        }
    }

    @Test
    fun `sort key of primary index`() {
        metadata.indexSortKey(TableMetadata.primaryIndexName()) shouldBe Optional.empty()
    }

    @Test
    fun `sort key of secondary index`() {
        metadata.indexSortKey("search") shouldBe Optional.of("dob")
    }

    @Test
    fun `sort key of missing index`() {
        shouldThrow<IllegalArgumentException> {
            metadata.indexSortKey("foo") shouldBe Optional.empty()
        }
    }

    @Test
    fun `sort key of index without sort key`() {
        metadata.indexSortKey("names") shouldBe Optional.empty()
    }

    @Test
    fun `index keys of primary index`() {
        metadata.indexKeys(TableMetadata.primaryIndexName()).shouldContainExactlyInAnyOrder("id")
    }

    @Test
    fun `index keys of secondary index`() {
        metadata.indexKeys("search").shouldContainExactlyInAnyOrder("name", "dob")
    }

    @Test
    fun `index keys of missing index`() {
        shouldThrow<IllegalArgumentException> {
            metadata.indexKeys("foo").shouldBeEmpty()
        }
    }

    @Test
    fun indices() {
        metadata.indices().shouldContainExactlyInAnyOrder(
            StaticIndexMetadata.builder()
                .name(TableMetadata.primaryIndexName())
                .partitionKey(StaticKeyAttributeMetadata.create("id", AttributeValueType.N))
                .build(),
            StaticIndexMetadata.builder()
                .name("search")
                .partitionKey(StaticKeyAttributeMetadata.create("name", AttributeValueType.S))
                .sortKey(StaticKeyAttributeMetadata.create("dob", AttributeValueType.S))
                .build(),
            StaticIndexMetadata.builder()
                .name("names")
                .partitionKey(StaticKeyAttributeMetadata.create("name", AttributeValueType.S))
                .build()
        )
    }

    @Test
    fun `all keys`() {
        metadata.allKeys().shouldContainExactlyInAnyOrder(
            "id", "name", "dob"
        )
    }

    @Test
    fun `key attributes`() {
        metadata.keyAttributes().shouldContainExactlyInAnyOrder(
            StaticKeyAttributeMetadata.create("id", AttributeValueType.N),
            StaticKeyAttributeMetadata.create("name", AttributeValueType.S),
            StaticKeyAttributeMetadata.create("dob", AttributeValueType.S)
        )
    }

    @Test
    fun `scalar attribute type of id`() {
        metadata.scalarAttributeType("id") shouldBe Optional.of(ScalarAttributeType.N)
    }

    @Test
    fun `scalar attribute type of name`() {
        metadata.scalarAttributeType("name") shouldBe Optional.of(ScalarAttributeType.S)
    }

    @Test
    fun `scalar attribute type of dob`() {
        metadata.scalarAttributeType("dob") shouldBe Optional.of(ScalarAttributeType.S)
    }

    @Test
    fun `scalar attribute type of missing`() {
        shouldThrow<IllegalArgumentException> {
            metadata.scalarAttributeType("missing") shouldBe Optional.empty()
        }
    }
}