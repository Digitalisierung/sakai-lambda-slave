package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynamoDbCatalogRepository implements CatalogRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbCatalogRepository.class);

    private final DynamoDbTable<Catalog> dynamoDbTable;

    public DynamoDbCatalogRepository(final DynamoDbEnhancedClient enhancedClient, final String tableName, TableSchema<Catalog> tableSchema) {
        this.dynamoDbTable = enhancedClient.table(tableName, tableSchema);
        LOGGER.info("DynamoDbCatalogRepository initialized. Table name {}", tableName);
    }

    @Override
    public PaginatedResult<Catalog> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying (all) catalogs using GSI_entityType. Limit {}", limit);

        QueryConditional queryConditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue("INDEX")
                .sortValue("METADATA#")
                .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .build();

        Iterator<Page<Catalog>> iterator = dynamoDbTable.index("GSI_entityType")
                .query(queryRequest)
                .iterator();

        if (iterator.hasNext()) {
            Page<Catalog> page = iterator.next();
            LOGGER.debug("Found {} catalogs on the current page.", page.items().size());
            return new PaginatedResult<>(page.items(), page.lastEvaluatedKey());
        }

        LOGGER.debug("No catalogs found.");
        return new PaginatedResult<>(List.of(), Map.of());
    }
}
