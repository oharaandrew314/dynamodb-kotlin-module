package io.andrewohara.dynamokt

import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter
import java.time.Instant

data class Cat(
    @DynamoKtPartitionKey val name: String,
    val lives: Int = 9,
    @DynamoKtConverted(InstantAsStringAttributeConverter::class)
    val birthDate: Instant? = null,
//    @DynamoKtConverted(EnumAttributeConverter<Food>::class.java)
    val favouriteFood: Food? = null,
    val nicknames: Set<String>? = null,
    @DynamoKtAttribute("fuzzy") val wuzzy: Boolean? = null,
    val notes: List<String>? = null,
    val staff: List<Staff>? = null
) {
    enum class Food { Sushi, Taco, Cheeseburger }

    data class Staff(val id: Int, val name: String)

    fun key(): Key = Key.builder().partitionValue(name).build()
    fun mockKey() = MockDynamoItem("name" to MockDynamoValue(name))
}