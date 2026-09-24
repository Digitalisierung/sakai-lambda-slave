package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.domain.service.CreateCatalogService;
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

public class CreateCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCatalogHandler.class);

    private final CreateCatalogService createCatalogService;
    private final GetCatalogService getCatalogService;

    public CreateCatalogHandler() {
        super();
        var repository = createCatalogRepository();
        this.createCatalogService = new CreateCatalogService(repository);
        this.getCatalogService = new GetCatalogService(repository);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("Create new catalog request received.");
        LOGGER.debug("Create new catalog Request details: request body:: {}", requestEvent.getBody());

        if (requestEvent.getBody() == null || requestEvent.getBody().isEmpty()) {
            LOGGER.warn("Create catalog request received without a body.");
            return ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing request body.");
        }
        String requestBody = requestEvent.getBody();

        try {
            CatalogDTO newCatalog = JsonUtil.parseFromJsonToObject(requestBody, new TypeReference<>() {
            });
            CatalogDTO savedCatalog = createCatalogService.saveCatalog(newCatalog);

            String responseBody = JsonUtil.convertToJson(savedCatalog);

            String host = requestEvent.getHeaders().get("Host");
            String stage = requestEvent.getRequestContext().getStage();
            String path = requestEvent.getPath();
            String catalogId = savedCatalog.id();
            String location = String.format("https://%s/%s%s/%s", host, stage, path, catalogId);

            Map<String, String> responseHeader = ResponseUtil.createExpandedHeader();
            responseHeader.put("Location", location);

            LOGGER.info("Create new catalog request completed successfully.");
            return ResponseUtil.createApiResponse(HttpStatusCode.CREATED, responseBody, responseHeader);
        } catch (Exception e) {
            LOGGER.error("Error during catalog creation.", e);
            return ExceptionHandler.handleException(e);
        }
    }

    private CatalogRepository<Catalog> createCatalogRepository() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<Catalog> tableSchema = DynamoDbFactory.createTableSchema();

        return new DynamoDbCatalogRepository(enhancedClient, tableName, tableSchema);
    }
}
