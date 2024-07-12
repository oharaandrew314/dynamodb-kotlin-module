package dev.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtPartitionKey

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtSortKey

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtConverted(val converter: KClass<out AttributeConverter<out Any>>)

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtSecondaryPartitionKey(val indexNames: Array<String>)

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtSecondarySortKey(val indexNames: Array<String>)

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtAttribute(val name: String)

@Target(AnnotationTarget.PROPERTY)
annotation class DynamoKtFlatten

@Target(AnnotationTarget.CLASS)
annotation class DynamoKtPreserveEmptyObject
