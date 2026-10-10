package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.domain.service.UpdateArticleService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.Map;

/**
 * Lambda handler for partial update of an existing article.
 * PUT /articles/{id}
 */
public class UpdateArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateArticleHandler.class);

    private final UpdateArticleService articleService;

    public UpdateArticleHandler() {
        super();
        articleService = new UpdateArticleService(createArticleRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LOGGER.info("Update article request received.");

        Map<String, String> pathParameters = request.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing required path parameter: article ID.");
        }
        String articleId = pathParameters.get("id");

        String body = request.getBody();
        if (body == null || body.isBlank()) {
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Request body must not be empty.");
        }

        try {
            Map<String, Object> bodyMap = JsonUtil.parseFromJsonToObject(body, new TypeReference<>() {
            });

            Map<String, Object> updated = articleService.updateAll(articleId, bodyMap);
            String responseBody = JsonUtil.convertToJson(updated);

            LOGGER.info("Update article request completed. articleId='{}'", articleId);
            return ResponseUtil.createApiResponse(HttpStatusCode.OK, responseBody, ResponseUtil.createExpandedHeader());

        } catch (Exception e) {
            LOGGER.error("Failed to handle update article request. articleId='{}'", articleId, e);
            return ExceptionHandler.handleException(e);
        }
    }

    private ArticleRepository<EnhancedDocument> createArticleRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
        return new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
    }
}
