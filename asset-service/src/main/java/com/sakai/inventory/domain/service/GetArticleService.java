package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import com.sakai.inventory.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

/**
 * Service for managing articles.
 * Contains business logic for operations with single article item.
 */
public class GetArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleService.class);

    private ArticleRepository articleRepository;

    public GetArticleService() {
        super();
    }

    public String findArticleById(String id) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
        String tableName = DynamoDbFactory.getTableName();
        articleRepository = new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);

        EnhancedDocument document = articleRepository.findArticleById("ITEM#" + id)
                .orElseThrow(() -> new NotFoundException("Article not found with id: " + id));

        LOGGER.info("Successfully fetched article: {}", id);
        LOGGER.debug("Returned articles payload: {}", document.toJson());
        return document.toJson();

    }
}
