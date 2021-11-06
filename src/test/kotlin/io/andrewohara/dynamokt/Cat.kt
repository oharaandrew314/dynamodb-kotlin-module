package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter
import java.time.Instant

data class Cat(
    @DynamoKtPartitionKey val name: String,
    val lives: Int = 9,
    val birthDate: Instant? = null,
    val favouriteFood: Food? = null,
    val nicknames: Set<String>? = null,
    @DynamoKtAttribute("fuzzy") val wuzzy: Boolean? = null,
    val notes: List<String>? = null,
    val staff: List<Staff>? = null,
    val owner: Staff? = null,
    val attributes: Map<String, String>? = null,
    @DynamoKtConverted(InstantAsLongAttributeConverter::class) val expires: Instant? = null,
    @DynamoKtConverted(InstantAsStringAttributeConverter::class) val expiresFriendly: Instant? = null
) {
    enum class Food { Sushi, Taco, Cheeseburger }

    data class Staff(val id: Int, val name: String)

    fun key(): Key = Key.builder().partitionValue(name).build()
}