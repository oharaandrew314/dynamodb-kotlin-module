package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

fun KType.toEnhancedType(): EnhancedType<out Any> {
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