package io.andrewohara.dynamokt

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType

fun <T: Any> DynamoDbTable<T>.createTableWithIndices() {
    val metadata = tableSchema().tableMetadata()

    val globalIndices = metadata.indices()
        .filter { it.partitionKey().get().name() != metadata.primaryPartitionKey() }
        .map { index ->
            EnhancedGlobalSecondaryIndex.builder()
                .indexName(index.name())
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build()
        }

    val localIndices = metadata.indices()
        .filter { it.partitionKey().get().name() == metadata.primaryPartitionKey() && it.name() != TableMetadata.primaryIndexName() }
        .map { index ->
            EnhancedLocalSecondaryIndex.builder()
                .indexName(index.name())
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build()
        }

    val request = CreateTableEnhancedRequest.builder()
        .globalSecondaryIndices(globalIndices)
        .localSecondaryIndices(localIndices)
        .build()

    createTable(request)
}