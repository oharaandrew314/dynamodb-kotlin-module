package dev.andrewohara.dynamokt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
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

    data class OuterTestItem(
        val bang: String,
        @DynamoKtFlatten
        val inner: TestItem
    )

    private val schema = DataClassTableSchema(TestItem::class)
    private val outerSchema = DataClassTableSchema(OuterTestItem::class)

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
    fun `item to map - flatten`() {
        val instance = OuterTestItem(
            bang = "BANG",
            inner = TestItem("troll", 9001)
        )

        outerSchema.itemToMap(instance, true) shouldBe mapOf(
            "foo" to AttributeValue.fromS("troll"),
            "bar" to AttributeValue.fromN("9001"),
            "bang" to AttributeValue.fromS("BANG")
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
    fun `map to item - flatten`() {
        val map = mapOf(
            "foo" to AttributeValue.fromS("troll"),
            "bar" to AttributeValue.fromN("9001"),
            "bang" to AttributeValue.fromS("BANG")
        )

        outerSchema.mapToItem(map) shouldBe OuterTestItem(
            bang = "BANG",
            inner = TestItem("troll", 9001)
        )
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

    @Test
    fun `recursive model`() {
        data class Item(val id: Int, val inner: Item?)

        val item = Item(1, Item(2, null))

        DataClassTableSchema(Item::class).itemToMap(item, true) shouldBe mapOf(
            "id" to AttributeValue.fromN("1"),
            "inner" to AttributeValue.fromM(mapOf(
                "id" to AttributeValue.fromN("2"),
                "inner" to AttributeValue.fromNul(true)
            ))
        )
    }

    data class Subtype(
        val map: Map<String, Any>
    )

    class SubtypeConverter: AttributeConverter<Subtype> {
        private val jackson = jacksonObjectMapper()

        override fun transformFrom(input: Subtype): AttributeValue = AttributeValue.fromS(jackson.writeValueAsString(input.map))
        override fun transformTo(input: AttributeValue) = Subtype(
            map = jackson.readValue(input.s())
        )
        override fun type(): EnhancedType<Subtype> = EnhancedType.of(Subtype::class.java)
        override fun attributeValueType() = AttributeValueType.S
    }

    @Test
    fun `custom converter for data class`() {
        data class CustomDataClass(
            @DynamoKtPartitionKey
            val id: String,
            @DynamoKtConverted(SubtypeConverter::class)
            val subtype: Subtype
        )

        val schema = DataClassTableSchema(CustomDataClass::class)

        val item = CustomDataClass(
            id = "foo",
            subtype = Subtype(mapOf("foo" to "bar", "num" to 1))
        )

        val map = mapOf(
            "id" to AttributeValue.fromS("foo"),
            "subtype" to AttributeValue.fromS("""{"foo":"bar","num":1}""")
        )

        schema.itemToMap(item, true) shouldBe map
        schema.mapToItem(map) shouldBe item
    }
}