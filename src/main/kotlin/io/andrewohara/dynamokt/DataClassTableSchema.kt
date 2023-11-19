package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass
import kotlin.reflect.full.*

class DataClassTableSchema<Item: Any>(dataClass: KClass<Item>): TableSchema<Item> {

    init {
        require(dataClass.isData) { "$dataClass must be a data class" }
    }
    private val metadata = DataClassTableMetadata(dataClass)
    private val type = EnhancedType.documentOf(dataClass.java, this)
    private val constructor = requireNotNull(dataClass.primaryConstructor) { "$dataClass must have a primary constructor"}
    private val attributes = DataClassAttribute.create(dataClass).associateBy { it.attributeName }

    override fun mapToItem(attributeMap: Map<String, AttributeValue>): Item {
        val arguments = attributes.values
            .mapNotNull { attr -> attributes[attr.attributeName]?.unConvert(attributeMap) }
            .toMap()

        return try {
            constructor.callBy(arguments)
        } catch (e: Throwable) {
            throw IllegalArgumentException("Could not map item to ${type.rawClass().simpleName}", e)
        }
    }

    override fun itemToMap(item: Item, ignoreNulls: Boolean): Map<String, AttributeValue> {
        return attributes.values
            .associate { attr -> attr.convert(item) }
            .filterValues { it.nul() != true || !ignoreNulls }
    }

    override fun itemToMap(item: Item, attributes: Collection<String>): Map<String, AttributeValue> {
        return this.attributes.values
            .filter { it.attributeName in attributes }
            .associate { attr -> attr.convert(item) }
    }

    override fun attributeValue(item: Item, attributeName: String): AttributeValue? {
        return attributes[attributeName]?.convert(item)?.second
    }

    override fun tableMetadata() = metadata

    override fun itemType(): EnhancedType<Item> = type

    override fun attributeNames() = attributes.keys.toList()

    override fun isAbstract() = false
}