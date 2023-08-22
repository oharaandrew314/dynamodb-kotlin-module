package io.andrewohara.dynamokt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.http4k.aws.AwsSdkClient
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Instant

class DataClassTableSchemaMapperTest {

    private val storage = Storage.InMemory<DynamoTable>()

    private val mapper = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder()
                .httpClient(AwsSdkClient(FakeDynamoDb(storage)))
                .credentialsProvider { AwsBasicCredentials.create("id", "secret") }
                .region(Region.CA_CENTRAL_1)
                .build()
        )
        .build()
        .table("cats", DataClassTableSchema(Cat::class))
        .also { it.createTable() }

    private val table get() = storage[mapper.tableName()]!!

    private fun save(vararg attrs: Pair<AttributeName, AttributeValue>) {
        storage[mapper.tableName()] = table.withItem(mapOf(*attrs))
    }

    @Test
    fun `put item with key and default value`() {
        val cat = Cat("Toggles")
        mapper.putItem(cat)

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9)
            )
        )
    }

    @Test
    fun `get item with key and overridden default`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("lives") to AttributeValue.Num(1)
        )

        val expected = Cat(
            name = "Toggles",
            lives = 1
        )

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `get item with key and default`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
        )

        val expected = Cat(
            name = "Toggles",
            lives = 9
        )

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `save item with renamed attribute`() {
        val cat = Cat("Toggles", wuzzy = true)
        mapper.putItem(cat)

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("fuzzy") to AttributeValue.Bool(true)
            )
        )
    }

    @Test
    fun `get item with renamed attribute`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("fuzzy") to AttributeValue.Bool(true)
        )

        val expected = Cat(
            name = "Toggles",
            wuzzy = true
        )

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `save item with string set`() {
        mapper.putItem(Cat("Toggles", nicknames = setOf("Brown", "Tigger")))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("nicknames") to AttributeValue.StrSet(setOf("Brown", "Tigger"))
            )
        )
    }

    @Test
    fun `get item with string set`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("nicknames") to AttributeValue.StrSet(setOf("Brown", "Tigger"))
        )

        val expected = Cat("Toggles", nicknames = setOf("Brown", "Tigger"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with instant`() {
        mapper.putItem(Cat("Toggles", birthDate = Instant.parse("2004-06-01T12:00:00Z")))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("birthDate") to AttributeValue.Str("2004-06-01T12:00:00Z")
            )
        )
    }

    @Test
    fun `get item with instant`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("birthDate") to AttributeValue.Str("2004-06-01T12:00:00Z")
        )

        val expected = Cat("Toggles", birthDate = Instant.parse("2004-06-01T12:00:00Z"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with enum`() {
        mapper.putItem(Cat("Toggles", favouriteFood = Cat.Food.Sushi))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("favouriteFood") to AttributeValue.Str("Sushi")
            )
        )
    }

    @Test
    fun `get item with enum`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("lives") to AttributeValue.Num(9),
            AttributeName.of("favouriteFood") to AttributeValue.Str("Sushi")
        )

        val expected = Cat("Toggles", favouriteFood = Cat.Food.Sushi)

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with list of primitives`() {
        mapper.putItem(Cat("Toggles", notes = listOf("good kitty", "needy kitty")))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("notes") to AttributeValue.List(listOf(
                    AttributeValue.Str("good kitty"),
                    AttributeValue.Str("needy kitty")
                ))
            )
        )
    }

    @Test
    fun `get item with list of primitives`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("notes") to AttributeValue.List(listOf(
                AttributeValue.Str("good kitty"),
                AttributeValue.Str("needy kitty")
            ))
        )

        val expected = Cat("Toggles", notes = listOf("good kitty", "needy kitty"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with list of objects`() {
        mapper.putItem(Cat("Toggles", staff = listOf(
            Cat.Staff(1, "Andrew"),
            Cat.Staff(2, "Lauren")
        )))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("staff") to AttributeValue.List(listOf(
                    AttributeValue.Map(mapOf(
                        AttributeName.of("id") to AttributeValue.Num(1),
                        AttributeName.of("name") to AttributeValue.Str("Andrew")
                    )),
                    AttributeValue.Map(mapOf(
                        AttributeName.of("id") to AttributeValue.Num(2),
                        AttributeName.of("name") to AttributeValue.Str("Lauren")
                    ))
                ))
            )
        )
    }

    @Test
    fun `get item with list of objects`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("staff") to AttributeValue.List(listOf(
                AttributeValue.Map(mapOf(
                    AttributeName.of("id") to AttributeValue.Num(1),
                    AttributeName.of("name") to AttributeValue.Str("Andrew")
                )),
                AttributeValue.Map(mapOf(
                    AttributeName.of("id") to AttributeValue.Num(2),
                    AttributeName.of("name") to AttributeValue.Str("Lauren")
                ))
            ))
        )

        val expected = Cat("Toggles", staff = listOf(
            Cat.Staff(1, "Andrew"),
            Cat.Staff(2, "Lauren")
        ))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with nested class`() {
        mapper.putItem(Cat("Toggles", owner = Cat.Staff(1, "Andrew")))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("owner") to AttributeValue.Map(mapOf(
                    AttributeName.of("id") to AttributeValue.Num(1),
                    AttributeName.of("name") to AttributeValue.Str("Andrew")
                ))
            )
        )
    }

    @Test
    fun `get item with nested class`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("owner") to AttributeValue.Map(mapOf(
                AttributeName.of("id") to AttributeValue.Num(1),
                AttributeName.of("name") to AttributeValue.Str("Andrew")
            ))
        )

        val expected = Cat("Toggles", owner = Cat.Staff(1, "Andrew"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with map`() {
        mapper.putItem(Cat("Toggles", attributes = mapOf("neediness" to "max", "colour" to "brown")))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("attributes") to AttributeValue.Map(mapOf(
                    AttributeName.of("neediness") to AttributeValue.Str("max"),
                    AttributeName.of("colour") to AttributeValue.Str("brown")
                ))
            )
        )
    }

    @Test
    fun `get item with map`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("lives") to AttributeValue.Num(9),
            AttributeName.of("attributes") to AttributeValue.Map(mapOf(
                AttributeName.of("neediness") to AttributeValue.Str("max"),
                AttributeName.of("colour") to AttributeValue.Str("brown")
            ))
        )

        val expected = Cat("Toggles", attributes = mapOf("neediness" to "max", "colour" to "brown"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with custom converter`() {
        mapper.putItem(Cat("Toggles", expires = Instant.ofEpochSecond(9001)))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("expires") to AttributeValue.Num(9001)
            )
        )
    }

    @Test
    fun `get item with custom converter`() {
        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("expires") to AttributeValue.Num(9001)
        )

        val expected = Cat("Toggles", expires = Instant.ofEpochSecond(9001))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with built-in converter`() {
        val time = Instant.parse("2021-01-01T12:00:00Z")
        mapper.putItem(Cat("Toggles", expiresFriendly = time))

        table.items.shouldContainExactly(
            mapOf(
                AttributeName.of("name") to AttributeValue.Str("Toggles"),
                AttributeName.of("lives") to AttributeValue.Num(9),
                AttributeName.of("expiresFriendly") to AttributeValue.Str(time.toString())
            )
        )
    }

    @Test
    fun `get item with built-in converter`() {
        val time = Instant.parse("2021-01-01T12:00:00Z")

        save(
            AttributeName.of("name") to AttributeValue.Str("Toggles"),
            AttributeName.of("expiresFriendly") to AttributeValue.Str(time.toString())
        )

        val expected = Cat("Toggles", expiresFriendly = time)

        mapper.getItem(expected.key()) shouldBe expected
    }
}