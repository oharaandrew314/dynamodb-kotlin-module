package dev.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.javaType

private fun KType.toEnhancedType(schemaCache: MetaTableSchemaCache): EnhancedType<out Any> {
    return when(val clazz = classifier as KClass<Any>) {
        List::class -> {
            val listType = arguments.first().type!!.toEnhancedType(schemaCache)
            EnhancedType.listOf(listType)
        }
        Set::class -> {
            val setType = arguments.first().type!!.toEnhancedType(schemaCache)
            EnhancedType.setOf(setType)
        }
        Map::class -> {
            val (key, value) = arguments.map { it.type!!.toEnhancedType(schemaCache) }
            EnhancedType.mapOf(key, value)
        }
        else -> {
            if (clazz.isData) {
                EnhancedType.documentOf(clazz.java, recursiveDataClassTableSchema(clazz, schemaCache)) {
                    if (clazz.findAnnotation<DynamoKtPreserveEmptyObject>() != null) {
                        it.preserveEmptyObject(true)
                    }
                }
            } else {
                EnhancedType.of(javaType)
            }
        }
    }
}

private fun <Attr: Any?> initConverter(clazz: KClass<out AttributeConverter<Attr>>): AttributeConverter<Attr> {
    clazz.constructors.firstOrNull { it.visibility == KVisibility.PUBLIC }
        ?.let { return it.call() }

    return clazz.staticFunctions
        .filter { it.name == "create" }
        .filter { it.visibility == KVisibility.PUBLIC }
        .first { it.parameters.isEmpty() }
        .call() as AttributeConverter<Attr>
}

private fun KProperty1<out Any, *>.tags() = buildList {
    for (annotation in annotations) {
        when(annotation) {
            is DynamoKtPartitionKey -> add(StaticAttributeTags.primaryPartitionKey())
            is DynamoKtSortKey -> add(StaticAttributeTags.primarySortKey())
            is DynamoKtSecondaryPartitionKey -> add(StaticAttributeTags.secondaryPartitionKey(annotation.indexNames.toList()))
            is DynamoKtSecondarySortKey -> add(StaticAttributeTags.secondarySortKey(annotation.indexNames.toList()))
        }
    }

    // add extension tags
    for (annotation in getter.annotations) {
        val tagAnnotation = annotation.annotationClass.findAnnotation<BeanTableSchemaAttributeTag>() ?: continue
        val generator = tagAnnotation.value.staticFunctions.find { it.name == "attributeTagFor" }
            ?: error("static attributeTagFor function required for ${tagAnnotation::class.simpleName}")

        add(generator.call(annotation) as StaticAttributeTag)
    }
}

internal fun <Table: Any, Attr: Any?> KProperty1<Table, Attr>.toImmutableDataClassAttribute(
    dataClass: KClass<Table>,
    schemaCache: MetaTableSchemaCache
): ImmutableAttribute<Table, ImmutableDataClassBuilder, Attr> {
    val converter = findAnnotation<DynamoKtConverted>()
        ?.converter
        ?.let { it as KClass<AttributeConverter<Attr>> }
        ?.let { initConverter(it) }
        ?: AttributeConverterProvider.defaultProvider().converterFor(returnType.toEnhancedType(schemaCache))
        as AttributeConverter<Attr>

    return ImmutableAttribute
        .builder(
            EnhancedType.of(dataClass.java),
            EnhancedType.of(ImmutableDataClassBuilder::class.java),
            converter.type()
        )
        .name(findAnnotation<DynamoKtAttribute>()?.name?: name)
        .getter(::get)
        .setter { builder, value -> builder[name] = value }
        .attributeConverter(converter)
        .tags(tags())
        .build()
}