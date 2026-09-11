package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.domain.service.UpdateCatalogService;
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

public class UpdateCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCatalogHandler.class);
    private final UpdateCatalogService updateCatalogService;

    public UpdateCatalogHandler() {
        super();
        this.updateCatalogService = new UpdateCatalogService(createCatalogRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("Update catalog request received.");
        LOGGER.debug("Update catalog request details: Requested path: '{}', RequestId: {}", requestEvent.getPath(), context.getAwsRequestId());
        Map<String, String> pathParam = requestEvent.getPathParameters();
        if (pathParam == null || !pathParam.containsKey("id")) {
            LOGGER.warn("Missing required path parameter: catalog ID");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing required path parameter: catalog ID");
        }
        String catalogId = pathParam.get("id");

        try {
            String requestBody = requestEvent.getBody();
            CatalogDTO catalogDTO = JsonUtil.parseFromJsonToObject(requestBody, CatalogDTO.class);
            CatalogDTO updatedCatalog = updateCatalogService.updateCatalog(catalogId, catalogDTO);
            String responseBody = JsonUtil.convertToJson(updatedCatalog);

            LOGGER.info("Update catalog request completed successfully.");
            return ResponseUtil.createApiResponse(HttpStatusCode.OK, responseBody, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            LOGGER.error("Failed to update catalog.", e);
            return ExceptionHandler.handleException(e);
        }
    }

    private CatalogRepository createCatalogRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        return new DynamoDbCatalogRepository(enhancedClient, tableName, TableSchema.fromBean(Catalog.class));
    }
}
