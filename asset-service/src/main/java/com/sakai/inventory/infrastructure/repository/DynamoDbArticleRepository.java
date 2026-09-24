package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

import java.util.*;

public class DynamoDbArticleRepository implements ArticleRepository<EnhancedDocument> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbArticleRepository.class);

    private final DynamoDbTable<EnhancedDocument> articleTable;

    public DynamoDbArticleRepository(final DynamoDbEnhancedClient enhancedClient, final String tableName, TableSchema<EnhancedDocument> tableSchema) {
        this.articleTable = enhancedClient.table(tableName, tableSchema);
        LOGGER.info("DynamoDbArticleRepository initialized, table {}", tableName);
    }

    @Override
    public Optional<EnhancedDocument> findById(String id) {
        LOGGER.debug("Finding article by id {}", id);

        Key key = buildArticleKey(id);

        EnhancedDocument rohDocument = articleTable.getItem(key);
        EnhancedDocument document = replaceIdInDocument(rohDocument);

        return Optional.ofNullable(document);
    }

    @Override
    public Optional<EnhancedDocument> findByIdViaGsi(String id) {
        LOGGER.debug("Querying single article by id='{}' using GSI_entityType.", id);
        QueryConditional queryConditional = QueryConditional.keyEqualTo(buildArticleKey(id));

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        SdkIterable<Page<EnhancedDocument>> sdkIterable = articleTable.index("GSI_entityType")
                .query(queryRequest);

        return sdkIterable.stream()
                .flatMap(page -> page.items().stream())
                .map(this::replaceIdInDocument)
                .findFirst();
    }

    @Override
    public PaginatedResult<EnhancedDocument> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying (all) articles using GSI_entityType. Limit {}, start key {}", limit, exclusiveStartKey != null);

        Key key = buildArticleKey();

        QueryConditional query = QueryConditional.keyEqualTo(key);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(query)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .build();

        SdkIterable<Page<EnhancedDocument>> sdkIterable = articleTable.index("GSI_entityType")
                .query(queryRequest);

        Optional<Page<EnhancedDocument>> pageOptional = sdkIterable.stream()
                .findFirst();

        final List<EnhancedDocument> articles = new ArrayList<>();
        final Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        pageOptional.ifPresent(page -> {
            articles.addAll(page.items());
            if (page.lastEvaluatedKey() != null) {
                lastEvaluatedKey.putAll(page.lastEvaluatedKey());
            }
        });

        List<EnhancedDocument> replacedList = articles.stream()
                .map(this::replaceIdInDocument)
                .toList();

        LOGGER.debug("Found {} articles.", replacedList.size());

        return new PaginatedResult<>(replacedList, lastEvaluatedKey);
    }

    @Override
    public PaginatedResult<EnhancedDocument> findCatalogArticles(final String id, final int limit, final Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying articles for catalog id '{}'. Limit {}, start key {}", id, limit, exclusiveStartKey != null);

        Key key = Key.builder()
                .partitionValue("ACC#default__CAT#" + id)
                .sortValue("ITEM#")
                .build();

        QueryConditional query = QueryConditional.sortBeginsWith(key);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(query)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build();

        Iterator<Page<EnhancedDocument>> pageIterator = articleTable.query(queryRequest).iterator();

        if (!pageIterator.hasNext()) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        Page<EnhancedDocument> documentPage = pageIterator.next();

        List<EnhancedDocument> articles = documentPage.items().stream()
                .map(this::replaceIdInDocument)
                .toList();

        return new PaginatedResult<>(articles, documentPage.lastEvaluatedKey());
    }

    private Key buildArticleKey(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Article id must not be null or empty.");
        }

        String sortKey = "ITEM#" + id;

        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue(sortKey)
                .build();
    }

    private Key buildArticleKey() {
        return Key.builder()
                .partitionValue("ARTICLES")
                .build();
    }

    private EnhancedDocument replaceIdInDocument(EnhancedDocument document) {
        String partitionKey = document.getString("partitionKey");
        String pk = extractIdFromPartitionKey(partitionKey);

        String sortKey = document.getString("sortKey");
        String sk = extractIdFromSortKey(sortKey);

        Map<String, AttributeValue> map = document.toMap();
        map.replace("partitionKey", AttributeValue.builder().s(pk).build());
        map.replace("sortKey", AttributeValue.builder().s(sk).build());

        return EnhancedDocument.fromAttributeValueMap(map);
    }


    private String extractIdFromPartitionKey(String partitionKey) {
        if (partitionKey == null || partitionKey.isBlank()) {
            return null;
        }

        String[] split = partitionKey.split("__");
        if (split.length < 2) {
            return null;
        }

        return extractIdFromSortKey(split[1]);
    }

    private String extractIdFromSortKey(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return null;
        }

        String[] split = sortKey.split("#");
        if (split.length < 2) {
            return null;
        }

        return split[1];
    }
}
