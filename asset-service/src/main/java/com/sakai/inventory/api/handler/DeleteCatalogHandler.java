package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Catalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.Map;

/**
 * BE-15: DELETE /catalogs/{id}
 * Löscht einen Katalog anhand seiner UUID.
 */
public class DeleteCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(DeleteCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende DELETE /catalogs/{id} Anfragen.
     * Sucht den Katalog anhand der UUID, ermittelt den vollständigen sortKey
     * und löscht das Item aus DynamoDB.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit dem Pfadparameter "id"
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 204 bei erfolgreichem Löschen, 400/404 bei Fehlern, 500 bei Systemfehler
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

            Catalog existing = KeyHelper.findCatalogById(table, id);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            Key key = Key.builder()
                    .partitionValue(existing.getPartitionKey())
                    .sortValue(existing.getSortKey())
                    .build();
            table.deleteItem(key);

            LOGGER.info("Katalog gelöscht: {}", id);
            return Utility.getApiResponse(204, "", Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Löschen des Katalogs: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
