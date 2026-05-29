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
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import utility.Utility;

import java.util.List;

/**
 * BE-11: GET /catalogs
 * Gibt alle Kataloge zurück inkl. productCount (Statistik-Aggregation).
 * productCount wird direkt auf dem Katalog-Item gespeichert und bei
 * Zuweisung/Entfernung von Artikeln aktualisiert.
 */
public class ListCatalogsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(ListCatalogsHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));

            QueryConditional query = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue("CATALOGS").build()
            );

            List<CatalogDTO> result = table.query(query)
                    .items()
                    .stream()
                    .map(this::mapToDTO)
                    .toList();

            LOGGER.info("Anzahl Kataloge: {}", result.size());
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(result), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen der Kataloge: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    private CatalogDTO mapToDTO(Catalog catalog) {
        return new CatalogDTO(
                catalog.getCatalogId(),
                catalog.getName(),
                catalog.getDescription(),
                catalog.getColor(),
                catalog.getProductCount() != null ? catalog.getProductCount() : 0,
                catalog.getCreatedAt(),
                catalog.getUpdatedAt()
        );
    }
}
