package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.domain.service.CreateNewArticleService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

public class CreateNewArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNewArticleHandler.class);

    private final CreateNewArticleService articleService;

    public CreateNewArticleHandler() {
        super();
        articleService = new CreateNewArticleService(createArticleRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LOGGER.info("Create article request received.");
        return new APIGatewayProxyResponseEvent();
    }

    private ArticleRepository<EnhancedDocument> createArticleRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
        return new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
    }
}
