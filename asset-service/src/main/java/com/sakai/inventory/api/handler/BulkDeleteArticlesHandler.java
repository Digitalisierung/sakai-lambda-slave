package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.domain.service.BulkDeleteArticlesService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.List;

public class BulkDeleteArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkDeleteArticlesHandler.class);

    private final BulkDeleteArticlesService deleteArticlesService;

    public BulkDeleteArticlesHandler() {
        super();
        this.deleteArticlesService = new BulkDeleteArticlesService(createArticleRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("Bulk delete articles request received.");
        String requestBody = requestEvent.getBody();
        try {
            List<String> deleteIds = JsonUtil.parseFromJsonToObject(requestBody, new TypeReference<>() {
            });
            List<String> unprocessedIds = deleteArticlesService.bulkDeleteArticles(deleteIds);

            int httpStatusCode = (unprocessedIds.size() == deleteIds.size())
                    ? HttpStatusCode.BAD_REQUEST
                    : HttpStatusCode.OK;
            String responseBody = JsonUtil.convertToJson(unprocessedIds);
            return ResponseUtil.createApiResponse(httpStatusCode, responseBody, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            return ExceptionHandler.handleException(e);
        }
    }

    private ArticleRepository<EnhancedDocument> createArticleRepository() {
        return new DynamoDbArticleRepository(
                DynamoDbFactory.createEnhancedClient(),
                DynamoDbFactory.getTableName(),
                DynamoDbFactory.createTableSchema()
        );
    }
}
