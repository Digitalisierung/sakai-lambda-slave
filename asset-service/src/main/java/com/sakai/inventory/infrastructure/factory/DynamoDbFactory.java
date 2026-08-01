package com.sakai.inventory.infrastructure.factory;

import com.sakai.inventory.shared.util.EnvironmentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Factory for DynamoDb Clients and DynamoDb resources.
 * Provides singleton instances to avoid creating multiple connections.
 */
public class DynamoDbFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbFactory.class);

    private static final DynamoDbClient DDB_CLIENT = DynamoDbClient.builder()
            .httpClient(AwsCrtHttpClient.create())
            .build();

    private static final DynamoDbEnhancedClient ENHANCED_CLIENT = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(DDB_CLIENT)
            .build();

    private DynamoDbFactory() {
        super();
    }

    /**
     * Get or create DynamoDb client instance.
     *
     * @return DynamoDbClient
     */
    public static DynamoDbClient createDynamoDbClient() {
        return DDB_CLIENT;
    }

    /**
     * Get or create DynamoDb Enhanced client instance.
     *
     * @return DynamoDbEnhancedClient
     */
    public static DynamoDbEnhancedClient createEnhancedClient() {
        return ENHANCED_CLIENT;
    }

    /**
     * Get table name from environment variable.
     *
     * @return String
     */
    public static String getTableName() {
        String tableName = EnvironmentLoader.getEnv("TABLE_NAME");

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("TABLE_NAME environment variable is not set.");
        }

        return tableName;
    }

    public static TableSchema<EnhancedDocument> createTableSchema() {
        LOGGER.debug("Table Schema will be created.");
        return TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "partitionKey", AttributeValueType.S)
                .addIndexSortKey(TableMetadata.primaryIndexName(), "sortKey", AttributeValueType.S)
                .addIndexPartitionKey("GSI_entityType", "entityType", AttributeValueType.S)
                .addIndexSortKey("GSI_entityType", "sortKey", AttributeValueType.S)
                .addIndexPartitionKey("GSI_ItemsInCatalogs", "catalogId", AttributeValueType.S)
                .addIndexSortKey("GSI_ItemsInCatalogs", "sortKey", AttributeValueType.S)
                .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                .build();
    }
}
