package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

object DataClassTableMetadata {
    private val attributeConverterProvider = DefaultAttributeConverterProvider()

    private fun <T: Any> KProperty1<T, *>.attributeType(): AttributeValueType {
        val converter = findAnnotation<DynamoKtConverted>()
            ?.converter
            ?.createInstance()
            ?: let {
                val type = returnType.classifier as KClass<*>
                val enhancedType = EnhancedType.of(type.java)

                attributeConverterProvider.converterFor(enhancedType)
            }

        return converter.attributeValueType()
    }

    operator fun invoke(dataClass: KClass<out Any>): TableMetadata {
        require(dataClass.isData)

        return StaticTableMetadata.builder().apply {
            for (prop in dataClass.declaredMemberProperties) {
                val type by lazy { prop.attributeType() }

                prop.findAnnotation<DynamoKtPartitionKey>()?.let {
                    addIndexPartitionKey(TableMetadata.primaryIndexName(), prop.name, type)
                }
                prop.findAnnotation<DynamoKtSortKey>()?.let {
                    addIndexSortKey(TableMetadata.primaryIndexName(), prop.name, type)
                }
                prop.findAnnotation<DynamoKtSecondaryPartitionKey>()?.let { annotation ->
                    for (indexName in annotation.indexNames) {
                        addIndexPartitionKey(indexName, prop.name, type)
                    }
                }
                prop.findAnnotation<DynamoKtSecondarySortKey>()?.let { annotation ->
                    for (indexName in annotation.indexNames) {
                        addIndexSortKey(indexName, prop.name, type)
                    }
                }
            }
        }.build()
    }
}

