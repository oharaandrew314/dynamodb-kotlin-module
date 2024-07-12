[![codecov](https://codecov.io/gh/oharaandrew314/dynamodb-kotlin-module/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/dynamodb-kotlin-module)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central Version](https://img.shields.io/maven-central/v/dev.andrewohara/dynamokt)](https://central.sonatype.com/artifact/dev.andrewohara/dynamokt)

# DynamoDb Kotlin Module

> **WARNING**
> During the move to Maven Central, the package prefix has changed from `io.andrewohara` to `dev.andrewohara`

Kotlin Module for the v2 dynamodb-enhanced SDK.

Adapting an idiomatic kotlin data model for use with the v2 dynamodb mapper is a pain, and full of compromises;
data classes emulate a bean, which nullifies much of the advantages of data classes.
This module provides a new `TableSchema` implementation that adds support for kotlin data classes.

- properties can be immutable; i.e. `val` is allowed
- properties don't need default values
- new annotations work directly on kotlin properties, rather than their getters

## Requirements

Java 11, 17, and 21 are officially supported.

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
    val expires: Instant?,

    @DynamoKtFlatten // flatten properties of annotated class with this document    
    val metadata: Metadata
)

// If an instance of this type is empty, don't coerce it to null
@DynamoKtPreserveEmptyObject
data class Metadata(val created: Instant?, val refereceNumber: Int?)
```

## Support for Extensions

Bean table extensions are supported.  Register the extension to the client like normal,
and then add the `DynamoDbBean` extension annotation to the getter of the property.

```kotlin
data class Post(
    @DynamoKtPartitionKey
    val id: UUID,
    
    val title: String,
    
    @get:DynamoDbVersionAttribute  // add extension annotation to the getter  (ie get:<annotation>)
    val version: Int
)
```

## Gotchas

The `DynamoDbAsyncClient.createTable` method does not create the indices your table definition might have defined.
This is unlike the `DynamoDbClient`, which, in recent versions of the AWS SDK, performs as you would expect.

To work around this, `DynamoKt` offers a `createTableWithIndices` extension method to provide the expected behaviour.

## Samples

See the [Samples](/src/test/kotlin/dev/andrewohara/dynamokt/samples)
