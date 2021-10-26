package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1

class DataClassAttribute<Item, Attribute>(
    val attributeName: String,
    private val prop: KProperty1<Item, Attribute>,
    private val converter: AttributeConverter<Attribute>,
    private val constructorParam: KParameter
) {
    val optional = !constructorParam.isOptional

    fun convert(item: Item): Pair<String, AttributeValue> {
        val value = prop.get(item)
            ?.let { converter.transformFrom(it) }
            ?: AttributeValue.builder().nul(true).build()

        return attributeName to value
    }

    fun unConvert(attrs: Map<String, AttributeValue>): Pair<KParameter, Attribute>? {
        val value = attrs[attributeName] ?: return null
        return constructorParam to converter.transformTo(value)
    }
}