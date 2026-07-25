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

    private static DynamoDbClient ddbClient;
    private static DynamoDbEnhancedClient enhancedClient;

    private DynamoDbFactory() {
        super();
        LOGGER.error("DynamoDbFactory has been created.");
    }

    /**
     * Get or create DynamoDb client instance.
     *
     * @return DynamoDbClient
     */
    public static DynamoDbClient createDynamoDbClient() {
        if (ddbClient == null) {
            ddbClient = DynamoDbClient.builder()
                    .httpClient(AwsCrtHttpClient.create())
                    .build();
            LOGGER.debug("Creating DynamoDb client.");
        }

        return ddbClient;
    }

    /**
     * Get or create DynamoDb Enhanced client instance.
     *
     * @param client
     * @return DynamoDbEnhancedClient
     */
    public static DynamoDbEnhancedClient createEnhancedClient(DynamoDbClient client) {
        if (enhancedClient == null) {
            enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(client)
                    .build();
        }

        return enhancedClient;
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
        return TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "partitionKey", AttributeValueType.S)
                .addIndexSortKey(TableMetadata.primaryIndexName(), "sortKey", AttributeValueType.S)
                .addIndexPartitionKey("GSI_entityType", "entityType", AttributeValueType.S)
                .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                .build();
    }
}
