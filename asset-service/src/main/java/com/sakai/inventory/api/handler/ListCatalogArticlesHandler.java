package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.PaginatedArticlesResponseDTO;
import com.sakai.inventory.domain.service.ListCatalogArticlesService;
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
 * The ListCatalogArticlesHandler class implements the AWS Lambda RequestHandler interface to process a
 * request and provide a response for listing articles by catalog id. It fetches, maps, and returns article data in JSON format.
 */
public class ListCatalogArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCatalogArticlesHandler.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ListCatalogArticlesService articlesService;

    public ListCatalogArticlesHandler() {
        super();
        this.articlesService = new ListCatalogArticlesService(createArticleRepository());
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("List catalog articles request received.");
        Map<String, String> pathParameters = requestEvent.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            LOGGER.warn("Missing required path parameter catalogId.");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing required path parameter catalogId.");
        }
        String catalogId = pathParameters.get("id");
        Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();

        int pageSize = parseLimit(queryStringParameters);
        String nextToken = extractToken(queryStringParameters);
        LOGGER.debug("Query Params: limit {}, has nextToken {}, catalogId {}", pageSize, nextToken != null, catalogId);

        PaginatedArticlesResponseDTO responseDTO = articlesService.findArticlesInCatalog(catalogId, nextToken, pageSize);
        try {
            String body = JsonUtil.convertToJson(responseDTO);
            LOGGER.info("List articles request completed. Has more: {}", responseDTO.hasMore());

            return ResponseUtil.createApiResponse(HttpStatusCode.OK, body, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            String message = String.format("Failed to handle list articles request: catalogId %s, pageSize %s, has nextToken - %s", catalogId, pageSize, nextToken != null);
            LOGGER.error(message, e);
            return ExceptionHandler.handleException(e);
        }
    }

    private int parseLimit(Map<String, String> queryParams) {
        if (queryParams == null || !queryParams.containsKey("limit")) {
            return DEFAULT_PAGE_SIZE;
        }
        String limit = queryParams.get("limit");
        try {
            int pageSize = Integer.parseInt(limit);
            if (pageSize < 0) {
                return DEFAULT_PAGE_SIZE;
            } else return Math.min(pageSize, MAX_PAGE_SIZE);
        } catch (Exception e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    private String extractToken(Map<String, String> queryStringParameters) {
        if (queryStringParameters == null || !queryStringParameters.containsKey("nextToken")) {
            return null;
        }

        return queryStringParameters.get("nextToken");
    }

    private ArticleRepository createArticleRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();

        return new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
    }
}
