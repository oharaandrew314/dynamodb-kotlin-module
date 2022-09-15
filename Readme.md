[![codecov](https://codecov.io/gh/oharaandrew314/dynamodb-kotlin-module/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/dynamodb-kotlin-module)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)

# DynamoDb Kotlin Module

Kotlin Module for the v2 dynamodb-enhanced SDK.

Adapting an idiomatic kotlin data model for use with the v2 dynamodb mapper is a pain, and full of compromises;
data classes emulate a bean, which nullifies much of the advantages of data classes.
This module provides a new `TableSchema` implementation that adds support for kotlin data classes.

- properties can be immutable; i.e. `val` is allowed
- properties don't need default values
- new annotations work directly on kotlin properties, rather than their getters

## Install

[![](https://jitpack.io/v/oharaandrew314/dynamodb-kotlin-module.svg)](https://jitpack.io/#oharaandrew314/dynamodb-kotlin-module)

Follow the instructions on Jitpack.

## Quickstart

```kotlin
// create a data class model, making sure to give it a partition key
data class Cat(
    @DynamoKtPartitionKey val name: String,
    val lives: Int = 9,
)

// use the new table schema provided by this module to init the table mapper
val tableSchema = DataClassTableSchema(Cat::class)
val cats = DynamoDbEnhancedClient.create().table("cats", tableScema)

// use the table mapper however you want!!
cats.createTable()
cats.putItem(Cat("Toggles"))
```

## Annotations

The schema uses a new set of property-friendly annotations.

```kotlin
data class Appointment(
    @DynamoKtPartitionKey  // partition key for main index
    @DynamoKtAttribute(name = "owner_id")  // optionally rename the attribute
    val ownerId: UUID,
    
    @DynamoKtSortKey  // sort key for main index
    val id: UUID,
    
    @DynamoKtSecondaryPartitionKey(indexNames = ["names"])  // partition key for secondary indices
    val lastName: String,
    
    @DynamoKtSecondarySortKey(indexNames = ["names"])  // sort key for secondary indices
    val firstName: String,
    
    @DynamoKtConverted(InstantAsLongAttributeConverter::class)  // override the attribute converter
    val expires: Instant?
)
```

## Enhanced Table Creator

The `DynamoDbTable.createTable()` method provided in the V2 SDK ignores all the indices defined by your annotations,
which can result in some ["surprising behaviour"](https://github.com/aws/aws-sdk-java-v2/issues/1771#issuecomment-1206701986).

With this plugin, you can now use the `DynamoDbTable.createTableWithIndices()` extension method;
using reasonable defaults to fill whatever gaps exist in the table metadata.

```kotlin
data class Person(
    @DynamoKtPartitionKey val id: Int,
    @DynamoKtSecondaryPartitionKey(indexNames = ["names"]) val name: String,
    val dob: Instant
)

val schema = DataClassTableSchema(Person::class)
val table = DynamoDbEnhancedClient.create().table("people", schema)

table.createTableWithIndices()
```

## Samples

See the [Samples](/src/test/kotlin/io/andrewohara/dynamokt/samples)
