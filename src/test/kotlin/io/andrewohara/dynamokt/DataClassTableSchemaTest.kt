package io.andrewohara.dynamokt

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Instant

class DataClassTableSchemaTest {

    private val backend = MockDynamoBackend()

    val foo = DynamoDbClient.create()
    val bar = DynamoDbEnhancedClient.create()

    private val mapper = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2(backend))
        .build()
        .table("cats", DataClassTableSchema(Cat::class))
        .also { it.createTable() }

    private val table = backend.getTable(mapper.tableName())

    @Test
    fun `put item with key and default value`() {
        val cat = Cat("Toggles")
        mapper.putItem(cat)

        table.items.shouldContainExactly(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(n = 9)
            )
        )
    }

    @Test
    fun `get item with key and overridden default`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(1)
            )
        )

        val expected = Cat(
            name = "Toggles",
            lives = 1
        )

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `get item with key and default`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles")
            )
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
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "fuzzy" to MockDynamoValue(bool = true)
            )
        )
    }

    @Test
    fun `get item with renamed attribute`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "fuzzy" to MockDynamoValue(bool = true)
            )
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
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "nicknames" to MockDynamoValue(ss = setOf("Brown", "Tigger"))
            )
        )
    }

    @Test
    fun `get item with string set`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "nicknames" to MockDynamoValue(ss = setOf("Brown", "Tigger"))
            )
        )

        val expected = Cat("Toggles", nicknames = setOf("Brown", "Tigger"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with instant`() {
        mapper.putItem(Cat("Toggles", birthDate = Instant.parse("2004-06-01T12:00:00Z")))

        table.items.shouldContainExactly(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "birthDate" to MockDynamoValue(s = "2004-06-01T12:00:00Z")
            )
        )
    }

    @Test
    fun `get item with instant`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "birthDate" to MockDynamoValue(s = "2004-06-01T12:00:00Z")
            )
        )

        val expected = Cat("Toggles", birthDate = Instant.parse("2004-06-01T12:00:00Z"))

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with enum`() {
        mapper.putItem(Cat("Toggles", favouriteFood = Cat.Food.Sushi))

        table.items.shouldContainExactly(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "favouriteFood" to MockDynamoValue(s = "Sushi")
            )
        )
    }

    @Test
    fun `get item with enum`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "favouriteFood" to MockDynamoValue(s = "Sushi")
            )
        )

        val expected = Cat("Toggles", favouriteFood = Cat.Food.Sushi)

        mapper.getItem(expected.key()) shouldBe expected
    }

    @Test
    fun `put item with list of primitives`() {
        mapper.putItem(Cat("Toggles", notes = listOf("good kitty", "needy kitty")))

        table.items.shouldContainExactly(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "notes" to MockDynamoValue(list = listOf(
                    MockDynamoValue(s = "good kitty"),
                    MockDynamoValue(s = "needy kitty")
                ))
            )
        )
    }

    @Test
    fun `get item with list of primitives`() {
        table.save(
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "notes" to MockDynamoValue(list = listOf(
                    MockDynamoValue(s = "good kitty"),
                    MockDynamoValue(s = "needy kitty")
                ))
            )
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
            MockDynamoItem(
                "name" to MockDynamoValue("Toggles"),
                "lives" to MockDynamoValue(9),
                "staff" to MockDynamoValue(list = listOf(
                    MockDynamoValue(map = MockDynamoItem(
                        "id" to MockDynamoValue(1),
                        "name" to MockDynamoValue("Andrew")
                    )),
                    MockDynamoValue(map = MockDynamoItem(
                        "id" to MockDynamoValue(2),
                        "name" to MockDynamoValue("Lauren")
                    ))
                ))
            )
        )
    }

    // TODO validate created table schema

    // TODO TTL?

    // TODO type converters

    // TODO lists of objects

    // TODO maps (nested data class)

    // TODO maps (nested non-data class not allowed)
}