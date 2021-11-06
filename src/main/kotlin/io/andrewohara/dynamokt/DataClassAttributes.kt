package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.javaType

class DataClassAttributes<Item>(
    private val attributes: Collection<DataClassAttribute<Item, *>>
): Iterable<DataClassAttribute<Item, *>> {

    private val byAttributeName = attributes.associateBy { it.attributeName }

    operator fun get(attributeName: String) = byAttributeName[attributeName]

    override fun iterator() = attributes.iterator()

    companion object {
        private val defaultConverterProvider = AttributeConverterProvider.defaultProvider()

        private fun initConverter(clazz: KClass<out AttributeConverter<out Any>>): AttributeConverter<out Any> {
            clazz.constructors.firstOrNull { it.visibility == KVisibility.PUBLIC }
                ?.let { return it.call() }

            clazz.staticFunctions
                .filter { it.name == "create" }
                .filter { it.visibility == KVisibility.PUBLIC }
                .firstOrNull { it.parameters.isEmpty() }
                ?.let { return it.call() as AttributeConverter<Any> }

            throw IllegalArgumentException("Cannot instantiate ${clazz.simpleName}")
        }

        fun <Item: Any> create(type: KClass<Item>): DataClassAttributes<Item> {
            val props = type.declaredMemberProperties.sortedBy { it.name }
            val params = type.primaryConstructor!!.parameters.sortedBy { it.name }

            val attributes = props.zip(params).map { (prop, param) ->
                val converter = prop.findAnnotation<DynamoKtConverted>()
                    ?.converter?.let { initConverter(it) }
                    ?: defaultConverterProvider.converterFor(prop.returnType.toEnhancedType())

                val dynamoName = prop.findAnnotation<DynamoKtAttribute>()?.name?: prop.name

                DataClassAttribute(dynamoName, prop, converter as AttributeConverter<Any?>, param)
            }

            return DataClassAttributes(attributes)
        }
    }
}

private fun KType.toEnhancedType(): EnhancedType<out Any> {
    return when(val clazz = classifier as KClass<Any>) {
        List::class -> {
            val listType = arguments.first().type!!.toEnhancedType()
            EnhancedType.listOf(listType)
        }
        Set::class -> {
            val setType = arguments.first().type!!.toEnhancedType()
            EnhancedType.setOf(setType)
        }
        Map::class -> {
            val (key, value) = arguments.map { it.type!!.toEnhancedType() }
            EnhancedType.mapOf(key, value)
        }
        else -> {
            if (clazz.isData) {
                EnhancedType.documentOf(clazz.java, DataClassTableSchema(clazz))
            } else {
                EnhancedType.of(javaType)
            }
        }
    }
}