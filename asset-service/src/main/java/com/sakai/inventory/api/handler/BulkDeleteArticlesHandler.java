package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.BulkDeleteRequestDTO;
import com.sakai.inventory.api.dto.BulkDeleteResponseDTO;
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
        if (requestBody == null || requestBody.isBlank()) {
            LOGGER.warn("Processing aborted. Request body must not be empty.");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Request body must not be empty.");
        }

        try {
            BulkDeleteRequestDTO deleteIds = JsonUtil.parseFromJsonToObject(requestBody, new TypeReference<>() {
            });
            BulkDeleteResponseDTO responseDTO = deleteArticlesService.bulkDeleteArticles(deleteIds);
            String responseBody = JsonUtil.convertToJson(responseDTO);

            if (responseDTO.unprocessed().size() == deleteIds.articleIds().size()) {
                LOGGER.warn("All delete operations failed.");
                return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, responseBody);
            }

            LOGGER.info("Bulk delete articles request completed. Processed {} articles. Successfully deleted: {} articles.", deleteIds.articleIds().size(), (deleteIds.articleIds().size() - responseDTO.unprocessed().size()));
            return ResponseUtil.createApiResponse(HttpStatusCode.OK, responseBody, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            LOGGER.error("Failed to handle bulk delete articles request.", e);
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
