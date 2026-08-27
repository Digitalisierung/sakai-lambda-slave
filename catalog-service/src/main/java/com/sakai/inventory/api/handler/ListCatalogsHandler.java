package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.PaginatedCatalogsResponseDTO;
import com.sakai.inventory.domain.service.ListCatalogsService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbCatalogRepository;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.Map;

public class ListCatalogsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCatalogsHandler.class);

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ListCatalogsService catalogService;

    public ListCatalogsHandler() {
        super();
        this.catalogService = new ListCatalogsService(createCatalogRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiRequest, Context context) {
        LOGGER.info("List catalogs request received.");

        Map<String, String> queryStringParameters = apiRequest.getQueryStringParameters();
        int pageSize = parseLimit(queryStringParameters);
        String nextToken = extractToken(queryStringParameters);
        LOGGER.debug("Query Params: limit {}, has nextToken - {}", pageSize, nextToken != null);

        try {
            PaginatedCatalogsResponseDTO responseDTO = catalogService.listCatalogsPaginated(pageSize, nextToken);
            String body = JsonUtil.convertToJson(responseDTO);
            LOGGER.debug("Query Params: limit {}, has nextToken - {}", pageSize, nextToken != null);

            return ResponseUtil.createApiResponse(HttpStatusCode.OK, body, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            String message = String.format("Failed to handle list catalogs request: pageSize %s, has nextToken - %s", pageSize, nextToken != null);
            LOGGER.error(message, e);
            return ExceptionHandler.handleException(e);
        }
    }

    private int parseLimit(Map<String, String> queryStringParameters) {
        if (queryStringParameters == null || !queryStringParameters.containsKey("limit")) {
            return DEFAULT_PAGE_SIZE;
        }

        String limit = queryStringParameters.get("limit");
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

    private CatalogRepository createCatalogRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        return new DynamoDbCatalogRepository(enhancedClient, tableName, DynamoDbFactory.createTableSchema());
    }
}
