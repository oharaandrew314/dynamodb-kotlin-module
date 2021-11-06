package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.reflect.KClass
import kotlin.reflect.full.*

class DataClassTableSchema<Item: Any>(dataClass: KClass<Item>): TableSchema<Item> {

    init {
        require(dataClass.isData)
    }
    private val metadata = DataClassTableMetadata(dataClass)
    private val type = EnhancedType.documentOf(dataClass.java, this)
    private val constructor = dataClass.primaryConstructor!!
    private val attributes = DataClassAttributes.create(dataClass)

    override fun mapToItem(attributeMap: Map<String, AttributeValue>): Item {
        val arguments = attributes
            .filterNot { attr -> attr.attributeName !in attributeMap && attr.optional } // omit missing values that are optional
            .mapNotNull { attr -> attributes[attr.attributeName]?.unConvert(attributeMap) }
            .toMap()

        return constructor.callBy(arguments)
    }

    override fun itemToMap(item: Item, ignoreNulls: Boolean): Map<String, AttributeValue> {
        return attributes
            .associate { attr -> attr.convert(item) }
            .filterValues { it.nul() != true || !ignoreNulls }
    }

    override fun itemToMap(item: Item, attributes: Collection<String>): Map<String, AttributeValue> {
        return this.attributes
            .filter { it.attributeName in attributes }
            .associate { attr -> attr.convert(item) }
    }

    override fun attributeValue(item: Item, attributeName: String): AttributeValue? {
        return attributes[attributeName]?.convert(item)?.second
    }

    override fun tableMetadata() = metadata

    override fun itemType(): EnhancedType<Item> = type

    override fun attributeNames() = attributes.map { it.attributeName }

    override fun isAbstract() = false
}