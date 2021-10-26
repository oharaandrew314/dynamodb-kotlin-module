package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.Key
import java.time.Instant

data class Cat(
    @DynamoKtPartitionKey val name: String,
    val lives: Int = 9,
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
}