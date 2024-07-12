package dev.andrewohara.dynamokt

import java.util.UUID

data class Person(
    val id: UUID
) {
    val name: String? = null
}