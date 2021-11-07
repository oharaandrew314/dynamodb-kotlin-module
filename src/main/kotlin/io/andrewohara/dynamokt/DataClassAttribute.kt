package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.staticFunctions

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

    companion object {
        private val defaultConverterProvider = AttributeConverterProvider.defaultProvider()

        private fun initConverter(clazz: KClass<out AttributeConverter<out Any>>): AttributeConverter<out Any> {
            clazz.constructors.firstOrNull { it.visibility == KVisibility.PUBLIC }
                ?.let { return it.call() }

            return clazz.staticFunctions
                .filter { it.name == "create" }
                .filter { it.visibility == KVisibility.PUBLIC }
                .first { it.parameters.isEmpty() }
                .call() as AttributeConverter<Any>
        }

        fun <Item: Any> create(type: KClass<Item>): Collection<DataClassAttribute<Item, Any?>> {
            val props = type.declaredMemberProperties.sortedBy { it.name }
            val params = type.primaryConstructor!!.parameters.sortedBy { it.name }

            val attributes = props.zip(params).map { (prop, param) ->
                val converter = prop.findAnnotation<DynamoKtConverted>()
                    ?.converter?.let { initConverter(it) }
                    ?: defaultConverterProvider.converterFor(prop.returnType.toEnhancedType())

                val dynamoName = prop.findAnnotation<DynamoKtAttribute>()?.name?: prop.name

                DataClassAttribute(dynamoName, prop, converter as AttributeConverter<Any?>, param)
            }

            return attributes
        }
    }
}