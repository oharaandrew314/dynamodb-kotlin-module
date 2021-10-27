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

## Quickstart

```kotlin
data class Cat(
    @DynamoKtPartitionKey val name: String,
    val lives: Int = 9,
)

val tableSchema = DataClassTableSchema(Cat::class)
val mapper = DynamoDbEnhancedClient.create().table("cats", tableScema)

mapper.createTable()
mapper.putItem(Cat("Toggles"))
```