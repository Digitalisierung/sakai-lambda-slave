package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.dto.UpdateCatalogRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
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
import java.util.regex.Pattern;

/**
 * BE-14: PUT /catalogs/{id}
 * Partial Update — nur gesetzte Felder werden überschrieben.
 * Sucht den Katalog via UUID (letztes sortKey-Segment).
 */
public class UpdateCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(UpdateCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    /**
     * Verarbeitet eingehende PUT /catalogs/{id} Anfragen (Partial Update).
     * Sucht den Katalog anhand der UUID, validiert optional den Farbcode
     * und überschreibt nur die im Body gesetzten Felder (ignoreNulls = true).
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit Pfadparameter "id" und JSON-Body
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 mit dem aktualisierten Katalog, 400/404 bei Fehlern, 500 bei Systemfehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String id = pathParams.get("id");

            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            UpdateCatalogRequest req = Utility.objectMapper.readValue(body, UpdateCatalogRequest.class);

            if (req.color() != null && !COLOR_PATTERN.matcher(req.color()).matches()) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"'color' muss ein gültiger Hex-Farbcode sein (z.B. #FF5733).\"}",
                        Utility.getHeaders());
            }

            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));

            Catalog existing = KeyHelper.findCatalogById(table, id);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            Catalog patch = new Catalog();
            patch.setPartitionKey(existing.getPartitionKey());
            patch.setSortKey(existing.getSortKey());
            if (req.name() != null)        patch.setName(req.name());
            if (req.description() != null) patch.setDescription(req.description());
            if (req.color() != null)       patch.setColor(req.color());
            patch.setUpdatedAt(Instant.now().toString());

            table.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(patch)
                    .ignoreNulls(true)
                    .build());

            Key key = Key.builder()
                    .partitionValue(existing.getPartitionKey())
                    .sortValue(existing.getSortKey())
                    .build();
            Catalog updated = table.getItem(key);

            LOGGER.info("Katalog aktualisiert: {}", id);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(mapToDTO(updated)), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Aktualisieren des Katalogs: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    /**
     * Wandelt ein Catalog-Datenbankobjekt in ein CatalogDTO für die API-Antwort um.
     * Die catalogId wird aus dem letzten Segment des sortKey extrahiert.
     *
     * @param catalog das aus DynamoDB gelesene Catalog-Objekt
     * @return das befüllte CatalogDTO-Objekt
     */
    private CatalogDTO mapToDTO(Catalog catalog) {
        return new CatalogDTO(
                KeyHelper.extractId(catalog.getSortKey()),
                catalog.getName(), catalog.getDescription(), catalog.getColor(),
                catalog.getProductCount() != null ? catalog.getProductCount() : 0,
                catalog.getCreatedAt(), catalog.getUpdatedAt()
        );
    }
}
