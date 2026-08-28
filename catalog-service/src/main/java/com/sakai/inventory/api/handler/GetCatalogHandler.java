package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.domain.service.GetCatalogService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbCatalogRepository;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.Map;

public class GetCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetCatalogHandler.class);

    private GetCatalogService catalogService;

    public GetCatalogHandler() {
        super();
        this.catalogService = new GetCatalogService(createCatalogRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("Get Catalog request received.");
        LOGGER.debug("Path {}", requestEvent.getPath());

        // extract catalog id from the path
        Map<String, String> pathParameters = requestEvent.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            LOGGER.warn("Missing required path parameter: id");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing required path parameter: catalog ID");
        }

        String catalogId = pathParameters.get("id");
        try {
            LOGGER.info("Fetching catalog by id: {}", catalogId);
            return this.catalogService.findCatalogById(catalogId)
                    .map(this::buildSuccessResponse)
                    .orElseGet(this::buildNotFoundResponse);
        } catch (Exception e) {
            String logMessage = String.format("Failed to handle get catalog request. Catalog ID=%s", catalogId);
            LOGGER.error(logMessage, e);
            return ExceptionHandler.handleException(e);
        }
    }

    private APIGatewayProxyResponseEvent buildNotFoundResponse() {
        LOGGER.info("Catalog not found.");
        return ResponseUtil.createApiResponse(HttpStatusCode.NOT_FOUND, null, ResponseUtil.createExpandedHeader());
    }

    private APIGatewayProxyResponseEvent buildSuccessResponse(CatalogDTO catalog) {
        try {
            LOGGER.info("Get catalog request completed.");
            String body = JsonUtil.convertToJson(catalog);
            LOGGER.debug("Returning catalog: {}", catalog);
            return ResponseUtil.createApiResponse(HttpStatusCode.OK, body, ResponseUtil.createExpandedHeader());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert catalog to JSON", e);
            return ResponseUtil.createErrorResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, "Failed to process response");
        }
    }

    private CatalogRepository createCatalogRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        return new DynamoDbCatalogRepository(enhancedClient, tableName, TableSchema.fromBean(Catalog.class));
    }
}
