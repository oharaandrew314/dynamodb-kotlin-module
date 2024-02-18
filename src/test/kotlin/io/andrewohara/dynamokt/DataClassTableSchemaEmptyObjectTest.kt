package io.andrewohara.dynamokt

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class DataClassTableSchemaEmptyObjectTest {

    data class ContainerNoPreserve(
        val id: String,
        val nested: NestedNoPreserve?
    )

    data class NestedNoPreserve(
        val foo: String?,
        val bar: String?
    )

    @Test
    fun `do not preserve empty object`() {
        val schema = DataClassTableSchema(ContainerNoPreserve::class)

        val item = mapOf(
            "id" to AttributeValue.fromS("123"),
            "nested" to AttributeValue.fromM(emptyMap())
        )

        schema.mapToItem(item) shouldBe ContainerNoPreserve("123", null)
    }

    data class ContainerYesPreserve(
        val id: String,
        val nested: NestedYesPreserve?
    )

    @DynamoKtPreserveEmptyObject
    data class NestedYesPreserve(
        val foo: String?,
        val bar: String?
    )

    @Test
    fun `preserve empty object`() {
        val schema = DataClassTableSchema(ContainerYesPreserve::class)

        val item = mapOf(
            "id" to AttributeValue.fromS("123"),
            "nested" to AttributeValue.fromM(emptyMap())
        )

        schema.mapToItem(item) shouldBe ContainerYesPreserve("123", NestedYesPreserve(null, null))
    }
}