![Test](https://github.com/oharaandrew314/dynamodb-kotlin-module/workflows/Test/badge.svg)
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

TODO