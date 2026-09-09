package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamoDbCatalogRepository implements CatalogRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbCatalogRepository.class);

    private static final String GSI_ENTITY_TYPE = "GSI_entityType";
    private static final String GSI_PARTITION_KEY = "INDEX";
    private static final String GSI_SORT_KEY = "METADATA";
    private static final String SORT_KEY = "METADATA";
    private static final String ACCOUNT_ID = "ACC#default__CAT";

    private final DynamoDbTable<Catalog> dynamoDbTable;

    public DynamoDbCatalogRepository(final DynamoDbEnhancedClient enhancedClient, final String tableName, TableSchema<Catalog> tableSchema) {
        this.dynamoDbTable = enhancedClient.table(tableName, tableSchema);
        LOGGER.info("DynamoDbCatalogRepository initialized. Table name {}", tableName);
    }

    @Override
    public Catalog updateCatalog(Catalog catalog) {
        String catalogId = catalog.getPartitionKey();
        catalog.setPartitionKey(ACCOUNT_ID + "#" + catalogId);
        catalog.setSortKey(SORT_KEY + "#");

        UpdateItemEnhancedRequest<Catalog> request = UpdateItemEnhancedRequest.builder(Catalog.class)
                .item(catalog)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(partitionKey)")
                        .build())
                .build();

        UpdateItemEnhancedResponse<Catalog> response = dynamoDbTable.updateItemWithResponse(request);

        return response.attributes();
    }

    @Override
    public Optional<Catalog> findCatalogById(String id) {
        LOGGER.debug("");
        Key key = Key.builder()
                .partitionValue("ACC#default__CAT#" + id)
                .sortValue(GSI_SORT_KEY + "#")
                .build();

        Catalog catalog = dynamoDbTable.getItem(key);
        return Optional.ofNullable(catalog);
    }

    @Override
    public PaginatedResult<Catalog> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying (all) catalogs using GSI_entityType. Limit {}", limit);

        QueryConditional queryConditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(GSI_PARTITION_KEY)
                .sortValue(GSI_SORT_KEY + "#")
                .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .build();

        Iterator<Page<Catalog>> iterator = dynamoDbTable.index(GSI_ENTITY_TYPE)
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
