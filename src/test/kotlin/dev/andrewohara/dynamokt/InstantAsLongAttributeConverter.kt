package dev.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

class InstantAsLongAttributeConverter: AttributeConverter<Instant> {

    override fun transformFrom(input: Instant): AttributeValue = AttributeValue.builder()
        .n(input.epochSecond.toString())
        .build()

    override fun transformTo(input: AttributeValue): Instant = Instant.ofEpochSecond(input.n().toLong())

    override fun type(): EnhancedType<Instant> = EnhancedType.of(Instant::class.java)

    override fun attributeValueType() = AttributeValueType.N
}