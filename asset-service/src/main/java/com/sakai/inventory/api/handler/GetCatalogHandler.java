package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Catalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.Map;

/**
 * BE-12: GET /catalogs/{id}
 * Gibt einen einzelnen Katalog anhand seiner UUID zurück.
 * Die UUID ist das letzte Segment des sortKey (CATALOGS#<NAME>#METADATA#<uuid>).
 */
public class GetCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(GetCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende GET /catalogs/{id} Anfragen.
     * Sucht den Katalog anhand der UUID via Query + contains-Filter.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit dem Pfadparameter "id"
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 mit dem gefundenen Katalog, 400 bei fehlender ID, 404 wenn nicht gefunden, 500 bei Fehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String id = pathParams.get("id");

            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));

            Catalog catalog = KeyHelper.findCatalogById(table, id);
            if (catalog == null) {
                LOGGER.warn("Katalog nicht gefunden: {}", id);
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            LOGGER.info("Katalog gefunden: {}", id);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(mapToDTO(catalog)), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen des Katalogs: {}", e.getMessage(), e);
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
                catalog.getName(),
                catalog.getDescription(),
                catalog.getColor(),
                catalog.getProductCount() != null ? catalog.getProductCount() : 0,
                catalog.getCreatedAt(),
                catalog.getUpdatedAt()
        );
    }
}
