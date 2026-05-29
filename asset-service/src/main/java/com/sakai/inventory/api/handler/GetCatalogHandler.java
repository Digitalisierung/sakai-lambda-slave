package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Catalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.Map;

/**
 * BE-12: GET /catalogs/{id}
 * Gibt einen einzelnen Katalog anhand seiner ID zurück.
 */
public class GetCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(GetCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String catalogId = pathParams.get("id");

            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));

            Key key = Key.builder()
                    .partitionValue("CATALOGS")
                    .sortValue("CAT#" + catalogId)
                    .build();

            Catalog catalog = table.getItem(key);
            if (catalog == null) {
                LOGGER.warn("Katalog nicht gefunden: {}", catalogId);
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            CatalogDTO dto = new CatalogDTO(
                    catalog.getCatalogId(),
                    catalog.getName(),
                    catalog.getDescription(),
                    catalog.getColor(),
                    catalog.getProductCount() != null ? catalog.getProductCount() : 0,
                    catalog.getCreatedAt(),
                    catalog.getUpdatedAt()
            );

            LOGGER.info("Katalog gefunden: {}", catalogId);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(dto), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen des Katalogs: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
