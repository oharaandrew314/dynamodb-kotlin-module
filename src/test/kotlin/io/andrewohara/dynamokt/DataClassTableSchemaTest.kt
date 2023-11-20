package io.andrewohara.dynamokt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.nio.ByteBuffer

class DataClassTableSchemaTest {

    data class TestItem(
        val foo: String,
        val bar: Int,
        @DynamoKtAttribute("baz") val third: ByteBuffer? = null
    )

    private val schema = DataClassTableSchema(TestItem::class)

    @Test
    fun `attribute names`() {
        schema.attributeNames().shouldContainExactlyInAnyOrder(
            "foo", "bar", "baz"
        )
    }

    @Test
    fun `get attribute value`() {
        val instance = TestItem("troll", 9001)

        schema.attributeValue(instance, "foo") shouldBe AttributeValue.builder().s("troll").build()
        schema.attributeValue(instance, "bar") shouldBe AttributeValue.builder().n("9001").build()
    }

    @Test
    fun `item to map - don't ignore nulls`() {
        val instance = TestItem("troll", 9001)

        schema.itemToMap(instance, false) shouldBe mapOf(
            "foo" to AttributeValue.builder().s("troll").build(),
            "bar" to AttributeValue.builder().n("9001").build(),
            "baz" to AttributeValue.builder().nul(true).build()
        )
    }

    @Test
    fun `item to map - ignore nulls`() {
        val instance = TestItem("troll", 9001)

        schema.itemToMap(instance, true) shouldBe mapOf(
            "foo" to AttributeValue.builder().s("troll").build(),
            "bar" to AttributeValue.builder().n("9001").build(),
        )
    }

    @Test
    fun `item to map - specific attributes`() {
        val instance = TestItem("troll", 9001, ByteBuffer.wrap("lolcats".toByteArray()))

        schema.itemToMap(instance, setOf("foo", "bar")) shouldBe mapOf(
            "foo" to AttributeValue.builder().s("troll").build(),
            "bar" to AttributeValue.builder().n("9001").build(),
        )
    }

    @Test
    fun `map to item`() {
        val map = mapOf(
            "foo" to AttributeValue.builder().s("troll").build(),
            "bar" to AttributeValue.builder().n("9001").build(),
            "baz" to AttributeValue.builder().b(SdkBytes.fromByteArray("lolcats".toByteArray())).build()
        )

        schema.mapToItem(map) shouldBe TestItem("troll", 9001, ByteBuffer.wrap("lolcats".toByteArray()))
    }

    @Test
    fun `is abstract`() {
        schema.isAbstract shouldBe false
    }

    @Test
    fun `map to item - missing entry for nullable field`() {
        data class Foo(val name: String, val age: Int?)
        val schema = DataClassTableSchema(Foo::class)

        val map = mapOf(
            "name" to AttributeValue.builder().s("Toggles").build()
        )

        schema.mapToItem(map) shouldBe Foo("Toggles", null)
    }

    @Test
    fun `construct schema for invalid data class - non-constructor properties`() {
        shouldThrow<IllegalArgumentException> {
            DataClassTableSchema(Person::class)
        }
    }

    @Test
    fun `non-data class throws error`() {
        shouldThrow<IllegalArgumentException> {
            DataClassTableSchema(String::class)
        }.message shouldBe "class kotlin.String must be a data class"
    }

    @Test
    fun `data class with private constructor`() {
        data class Person private constructor(val name: String)

        shouldThrow<IllegalStateException> {
            DataClassTableSchema(Person::class)
        }.message shouldBe "Person must have a public primary constructor"
    }
}