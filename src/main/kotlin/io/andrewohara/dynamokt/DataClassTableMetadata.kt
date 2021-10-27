package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

// TODO use StaticTableMetadata to eliminate any chance of reflection after init
class DataClassTableMetadata<I: Any>(private val dataClass: KClass<I>): TableMetadata {

    // TODO perform all reflection during init
    private val partitionKey = dataClass.declaredMemberProperties.find { it.hasAnnotation<DynamoKtPartitionKey>() }
    private val sortKey  = dataClass.declaredMemberProperties.find { it.hasAnnotation<DynamoKtSortKey>() }
    private val indexNames = dataClass.declaredMemberProperties
        .mapNotNull { it.findAnnotation<DynamoKtSecondaryPartitionKey>() }
        .flatMap { it.indexNames.toList() }
        .toSet()

    init {
        require(dataClass.isData)
    }

    override fun indexPartitionKey(indexName: String): String {
        val prop = indexPartitionProp(indexName) ?: throw IllegalArgumentException()
        return prop.name
    }

    override fun indexSortKey(indexName: String): Optional<String> {
        val prop = indexSortProp(indexName)
        return Optional.ofNullable(prop?.name)
    }

    override fun indexKeys(indexName: String): Collection<String> {
        val index = indices().find { it.name() == indexName } ?: return emptyList()

        return listOfNotNull(
            index.partitionKey().map { it.name() }.orElse(null),
            index.sortKey().map { it.name() }.orElse(null)
        )
    }

    override fun indices(): Collection<IndexMetadata> {
        val primaryIndex = let {
            StaticIndexMetadata.builder()
                .name(TableMetadata.primaryIndexName())
                .partitionKey(partitionKey?.keyAttributeMetadata())
                .sortKey(sortKey?.keyAttributeMetadata())
                .build()
        }

        val secondaryIndices = indexNames.map { indexName ->
            StaticIndexMetadata.builder()
                .name(indexName)
                .partitionKey(indexPartitionProp(indexName)?.keyAttributeMetadata())
                .sortKey(indexSortProp(indexName)?.keyAttributeMetadata())
                .build()
        }

        return listOf(primaryIndex) + secondaryIndices
    }

    // TODO implement?
    override fun customMetadata(): Map<String, Any> = emptyMap()
    override fun <T : Any?> customMetadataObject(key: String, objectClass: Class<out T>) = Optional.empty<T>()

    override fun allKeys() = keyAttributes().map { it.name() }
    override fun keyAttributes(): Collection<KeyAttributeMetadata> {
        return dataClass.declaredMemberProperties
            .filter { it.isIndex() }
            .map { it.keyAttributeMetadata() }
    }

    override fun scalarAttributeType(keyAttribute: String): Optional<ScalarAttributeType> {
        val result = keyAttributes().find { it.name() == keyAttribute } ?: return Optional.empty()
        return Optional.of(result.attributeValueType().scalarAttributeType())
    }

    // helpers

    private fun <T: Any> KProperty1<T, *>.isIndex(): Boolean {
        return hasAnnotation<DynamoKtPartitionKey>() || hasAnnotation<DynamoKtSortKey>() || hasAnnotation<DynamoKtSecondaryPartitionKey>() || hasAnnotation<DynamoKtSecondarySortKey>()
    }

    private fun indexPartitionProp(indexName: String): KProperty1<out Any, *>? {
        if (indexName == TableMetadata.primaryIndexName()) return partitionKey

        return dataClass.declaredMemberProperties.find { prop ->
            val indices = prop.findAnnotation<DynamoKtSecondaryPartitionKey>()?.indexNames ?: emptyArray()
            indexName in indices
        }
    }

    private fun indexSortProp(indexName: String): KProperty1<out Any, *>? {
        if (indexName == TableMetadata.primaryIndexName()) return sortKey

        return dataClass.declaredMemberProperties.find { prop ->
            val indices = prop.findAnnotation<DynamoKtSecondarySortKey>()?.indexNames ?: emptyArray()
            indexName in indices
        }
    }

    private fun <T: Any> KProperty1<T, *>.keyAttributeMetadata(): KeyAttributeMetadata {
        val attrType = when(scalarAttributeType()) {
            ScalarAttributeType.B -> AttributeValueType.B
            ScalarAttributeType.N -> AttributeValueType.N
            ScalarAttributeType.S -> AttributeValueType.S
            ScalarAttributeType.UNKNOWN_TO_SDK_VERSION -> throw IllegalStateException()
        }

        return StaticKeyAttributeMetadata.create(name, attrType)
    }

    private val attributeConverterProvider = DefaultAttributeConverterProvider()

    private fun <T: Any> KProperty1<T, *>.scalarAttributeType(): ScalarAttributeType {
        findAnnotation<DynamoKtConverted>()
            ?.converter?.createInstance()
            ?.attributeValueType()?.scalarAttributeType()
            ?.also { return it }

        val type = returnType.classifier as KClass<*>
        val enhancedType = EnhancedType.of(type.java)

        return attributeConverterProvider.converterFor(enhancedType)
            .attributeValueType().scalarAttributeType()
    }
}