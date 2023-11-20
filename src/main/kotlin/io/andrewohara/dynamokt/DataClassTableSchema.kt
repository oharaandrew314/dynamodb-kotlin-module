package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor


fun <Item: Any> DataClassTableSchema(dataClass: KClass<Item>): TableSchema<Item> {
    val props = dataClass.declaredMemberProperties.sortedBy { it.name }
    val params = dataClass.primaryConstructor!!.parameters.sortedBy { it.name }

    require(dataClass.isData) { "$dataClass must be a data class" }
    require(props.size == params.size) { "$dataClass properties MUST all be declared in the constructor" }
    val constructor = requireNotNull(dataClass.primaryConstructor) { "$dataClass must have a primary constructor"}

    return StaticImmutableTableSchema.builder(dataClass.java, ImmutableDataClassBuilder::class.java)
        .newItemBuilder({ ImmutableDataClassBuilder(constructor) }, { it.build() as Item })
        .attributes(dataClass.declaredMemberProperties.map { it.toImmutableDataClassAttribute(dataClass) })
        .build()
}