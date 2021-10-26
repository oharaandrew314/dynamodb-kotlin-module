package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata
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
        .filter { it.hasAnnotation<DynamoKtSecondaryPartitionKey>() }
        .map { it.name }

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
        return dataClass.declaredMemberProperties.filter { prop ->
            prop.findAnnotation<DynamoKtSecondaryPartitionKey>()?.indexNames?.forEach {
                if (it == indexName) return@filter true
            }


            prop.findAnnotation<DynamoKtSecondarySortKey>()?.indexNames?.forEach {
                if (it == indexName) return@filter true
            }
            false
        }.map { it.name }
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
                .partitionKey(indexPartitionProp(indexName)!!.keyAttributeMetadata())
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
        val type = dataClass.declaredMemberProperties
            .find { it.name == keyAttribute }
            ?.scalarAttributeType()

        return Optional.ofNullable(type)
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

    private fun <T: Any> KProperty1<T, *>.scalarAttributeType(): ScalarAttributeType {
        findAnnotation<DynamoKtConverted>()
            ?.converter?.createInstance()
            ?.attributeValueType()?.scalarAttributeType()
            ?.also { return it }

        return when(returnType) {
            String::class.createType() -> ScalarAttributeType.S
            Number::class.createType() -> ScalarAttributeType.N
            ByteArray::class.createType() -> ScalarAttributeType.B
            else -> throw IllegalArgumentException("Unsupported key type: $returnType")
        }
    }
}