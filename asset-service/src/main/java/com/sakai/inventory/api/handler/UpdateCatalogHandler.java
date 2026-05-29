package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.dto.UpdateCatalogRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Catalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import utility.Utility;

import java.time.Instant;
import java.util.Map;

/**
 * BE-14: PUT /catalogs/{id}
 * Aktualisiert Katalog-Details (Partial Update).
 * Nur gesetzte Felder werden überschrieben.
 */
public class UpdateCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(UpdateCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    private static final java.util.regex.Pattern COLOR_PATTERN =
            java.util.regex.Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String catalogId = pathParams.get("id");

            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            UpdateCatalogRequest req = Utility.objectMapper.readValue(body, UpdateCatalogRequest.class);

            // Farbcode validieren, wenn gesetzt
            if (req.color() != null && !COLOR_PATTERN.matcher(req.color()).matches()) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"'color' muss ein gültiger Hex-Farbcode sein (z.B. #FF5733).\"}",
                        Utility.getHeaders());
            }

            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));

            Key key = Key.builder()
                    .partitionValue("CATALOGS")
                    .sortValue("CAT#" + catalogId)
                    .build();

            Catalog existing = table.getItem(key);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            Catalog patch = new Catalog();
            patch.setPartitionKey("CATALOGS");
            patch.setSortKey("CAT#" + catalogId);
            if (req.name() != null)        patch.setName(req.name());
            if (req.description() != null) patch.setDescription(req.description());
            if (req.color() != null)       patch.setColor(req.color());
            patch.setUpdatedAt(Instant.now().toString());

            table.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(patch)
                    .ignoreNulls(true)
                    .build());

            Catalog updated = table.getItem(key);
            CatalogDTO dto = new CatalogDTO(
                    updated.getCatalogId(), updated.getName(), updated.getDescription(),
                    updated.getColor(),
                    updated.getProductCount() != null ? updated.getProductCount() : 0,
                    updated.getCreatedAt(), updated.getUpdatedAt()
            );

            LOGGER.info("Katalog aktualisiert: {}", catalogId);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(dto), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Aktualisieren des Katalogs: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
