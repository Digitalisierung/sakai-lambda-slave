package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.mapper.DynDbMapper;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Service for managing articles.
 * Contains business logic for operations with single article item.
 */
public class GetArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleService.class);

    private ArticleRepository articleRepository;
    private DynDbMapper mapper;

    public GetArticleService() {
        super();
    }

    public GetArticleService(ArticleRepository repository, DynDbMapper mapper) {
        this.articleRepository = repository;
        this.mapper = mapper;
    }

    public String findArticleById(String id) {
        LOGGER.info("Find article by id: {}", id);

        try (DynamoDbClient client = DynamoDbFactory.createDynamoDbClient()) {
            DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient(client);
            TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
            String tableName = DynamoDbFactory.getTableName();
            articleRepository = new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
            EnhancedDocument document = articleRepository.findById(id)
                    .orElseThrow();
            return document.toJson();
        } catch (Exception e) {
            LOGGER.error("Error", e);
            throw e;
        }
    }
}
