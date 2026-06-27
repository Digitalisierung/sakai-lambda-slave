package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class DynamoDbArticleRepository implements ArticleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbArticleRepository.class);

    private final DynamoDbTable<EnhancedDocument> articleTable;

    public DynamoDbArticleRepository(final DynamoDbEnhancedClient enhancedClient, final String tableName, TableSchema<EnhancedDocument> tableSchema) {
        this.articleTable = enhancedClient.table(tableName, tableSchema);
        LOGGER.info("DynamoDbArticleRepository initialized with table {}", tableName);
    }

    @Override
    public Optional<EnhancedDocument> findById(String id) {
        LOGGER.debug("Finding article by id {}", id);

        Key key = buildArticleKey(id);

        EnhancedDocument document = articleTable.getItem(key);

        return Optional.ofNullable(document);
    }

    @Override
    public PaginatedResult<EnhancedDocument> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Finding all articles with: limit {}, startKey {}", limit, exclusiveStartKey != null);

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .build();

        PageIterable<EnhancedDocument> pages = articleTable.scan(scanRequest);
        Iterator<Page<EnhancedDocument>> pageIterator = pages.iterator();

        List<EnhancedDocument> articles = new ArrayList<>(limit);
        Map<String, AttributeValue> lastKey = null;

        while (pageIterator.hasNext()) {
            Page<EnhancedDocument> page = pageIterator.next();
            articles.addAll(page.items());
            lastKey = page.lastEvaluatedKey();

            LOGGER.debug("Found {} articles", articles.size());
        }

        return new PaginatedResult<>(articles, lastKey);
    }

    private Key buildArticleKey(String id) {
        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue("ARTICLES#SKU-030#" + id)
                .build();
    }
}
