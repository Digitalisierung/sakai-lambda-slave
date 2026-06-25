package com.sakai.inventory.infrastructure.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.util.Optional;

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

    private Key buildArticleKey(String id) {
        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue("ARTICLES#SKU-030#" + id)
                .build();
    }
}
