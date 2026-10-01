package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.BatchCreateResponseDTO;
import com.sakai.inventory.domain.service.CreateNewArticleService;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateNewArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNewArticleHandler.class);
    private static final int MAX_BATCH_SIZE = 25;

    private final CreateNewArticleService articleService;

    public CreateNewArticleHandler() {
        super();
        articleService = new CreateNewArticleService(createArticleRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LOGGER.info("Create articles request received.");

        String body = request.getBody();
        if (body == null || body.isBlank()) {
            LOGGER.warn("Processing aborted. Request body must not be empty.");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Request body must not be empty.");
        }

        try {
            EnhancedDocument doc = EnhancedDocument.fromJson(body);
            List<EnhancedDocument> articles = build(doc);

            if (articles.isEmpty()) {
                return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Article list must not be empty.");
            }

            if (articles.size() > MAX_BATCH_SIZE) {
                return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST,
                        "Batch size exceeds maximum of " + MAX_BATCH_SIZE + " items.");
            }

            BatchCreateResponseDTO response = articleService.batchWriteArticles(articles);

            if (response.created().isEmpty()) {
                LOGGER.info("The creation of articles did not succeed. Created: {} Failed: {}", 0, response.failed().size());
                String responseBody = JsonUtil.convertToJson(response);
                return ResponseUtil.createApiResponse(422, responseBody, ResponseUtil.createExpandedHeader());
            }

            String responseBody = JsonUtil.convertToJson(response);
            LOGGER.info("Create articles request completed. Created: {}, Failed: {}",
                    0, response.failed().size());
            return ResponseUtil.createApiResponse(HttpStatusCode.CREATED, responseBody, ResponseUtil.createExpandedHeader());

        } catch (Exception e) {
            LOGGER.error("Failed to handle create articles request.", e);
            return ExceptionHandler.handleException(e);
        }
    }

    private List<EnhancedDocument> build(EnhancedDocument document) {
        Integer stock = document.get("stock", Integer.class);
        if (stock == null || stock <= 0) {
            return new ArrayList<>();
        }
        List<EnhancedDocument> articles = new ArrayList<>(stock);
        Map<String, AttributeValue> map = document.toMap();

        for (int i = 0; i < stock; i++) {
            EnhancedDocument doc = EnhancedDocument.fromAttributeValueMap(map);
            articles.add(doc);
        }

        return articles;
    }

    private ArticleRepository<EnhancedDocument> createArticleRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
        return new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
    }
}
