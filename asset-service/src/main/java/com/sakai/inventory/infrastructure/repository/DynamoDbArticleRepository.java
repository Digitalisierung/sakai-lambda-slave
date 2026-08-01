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

import java.util.*;

public class DynamoDbArticleRepository implements ArticleRepository {
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

        EnhancedDocument document = articleTable.getItem(key);

        return Optional.ofNullable(document);
    }

    @Override
    public Optional<EnhancedDocument> findArticleById(String articleId) {
        LOGGER.debug("Querying single article by id='{}' using GSI_entityType.", articleId);
        QueryConditional queryConditional = QueryConditional.keyEqualTo(buildArticleKey(articleId));

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        SdkIterable<Page<EnhancedDocument>> sdkIterable = articleTable.index("GSI_entityType")
                .query(queryRequest);

        return sdkIterable.stream()
                .map(page -> page.items().getFirst())
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

        LOGGER.debug("Found {} articles.", articles.size());

        return new PaginatedResult<>(articles, lastEvaluatedKey);
    }

    private Key buildArticleKey(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Article id must not be null or empty.");
        }

        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue(id)
                .build();
    }

    private Key buildArticleKey() {
        return Key.builder()
                .partitionValue("ARTICLES")
                .build();
    }
}
