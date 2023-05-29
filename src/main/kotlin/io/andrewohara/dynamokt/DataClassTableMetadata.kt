package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

object DataClassTableMetadata {
    private val attributeConverterProvider = DefaultAttributeConverterProvider()

    private fun <T: Any> KProperty1<T, *>.attributeType(): AttributeValueType {
        val converter = findAnnotation<DynamoKtConverted>()?.converter?.createInstance() ?: let {
            val type = returnType.classifier as KClass<*>
            val enhancedType = EnhancedType.of(type.java)

            attributeConverterProvider.converterFor(enhancedType)
        }

        return converter.attributeValueType()
    }

    operator fun invoke(dataClass: KClass<out Any>): TableMetadata {
        val propNames = dataClass.declaredMemberProperties.associateWith { prop ->
            prop.findAnnotation<DynamoKtAttribute>()?.name ?: prop.name
        }

        return StaticTableMetadata.builder().apply {
            for (prop in dataClass.declaredMemberProperties) {
                val type by lazy { prop.attributeType() }

                val propName = propNames[prop] ?: prop.name

                for (annotation in prop.annotations) {
                    when(annotation) {
                        is DynamoKtPartitionKey -> addIndexPartitionKey(TableMetadata.primaryIndexName(), propName, type)
                        is DynamoKtSortKey -> addIndexSortKey(TableMetadata.primaryIndexName(), propName, type)
                        is DynamoKtSecondaryPartitionKey -> {
                            for (indexName in annotation.indexNames) {
                                addIndexPartitionKey(indexName, propName, type)
                            }
                        }
                        is DynamoKtSecondarySortKey -> {
                            for (indexName in annotation.indexNames) {
                                addIndexSortKey(indexName, propName, type)
                            }
                        }
                    }
                }
            }
        }.build()
    }
}

