package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.dto.CreateCatalogRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Catalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * BE-13: POST /catalogs
 * Erstellt einen neuen Katalog.
 * Pflichtfelder mit Validierung: name (nicht leer), color (Hex-Format #RRGGBB oder #RGB).
 *
 * sortKey-Format: CATALOGS#<NAME_UPPER>#METADATA#<uuid>
 * Entspricht dem Format der vorhandenen Testdaten.
 */
public class CreateCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(CreateCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    /**
     * Verarbeitet eingehende POST /catalogs Anfragen.
     * Validiert Pflichtfelder (name, color) und das Hex-Format des Farbcodes.
     * Generiert eine UUID als ID und baut den sortKey nach dem Format
     * "CATALOGS#&lt;NAME_GROSS&gt;#METADATA#&lt;uuid&gt;" auf.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit dem JSON-Body
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 201 mit dem erstellten Katalog, 400 bei Validierungsfehlern, 500 bei Systemfehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            CreateCatalogRequest req = Utility.objectMapper.readValue(body, CreateCatalogRequest.class);

            if (req.name() == null || req.name().isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'name' fehlt.\"}", Utility.getHeaders());
            }
            if (req.color() == null || req.color().isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'color' fehlt.\"}", Utility.getHeaders());
            }
            if (!COLOR_PATTERN.matcher(req.color()).matches()) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"'color' muss ein gültiger Hex-Farbcode sein (z.B. #FF5733).\"}",
                        Utility.getHeaders());
            }

            String uuid = UUID.randomUUID().toString();
            String now = Instant.now().toString();

            // sortKey-Format passend zu den Testdaten: CATALOGS#<NAME>#METADATA#<uuid>
            String nameSegment = req.name().toUpperCase().replaceAll("[^A-Z0-9]", "_");
            String sortKey = "CATALOGS#" + nameSegment + "#METADATA#" + uuid;

            Catalog catalog = new Catalog();
            catalog.setPartitionKey("CATALOGS");
            catalog.setSortKey(sortKey);
            catalog.setName(req.name());
            catalog.setDescription(req.description());
            catalog.setColor(req.color());
            catalog.setProductCount(0);
            catalog.setCreatedAt(now);
            catalog.setUpdatedAt(now);

            DynamoDbTable<Catalog> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));
            table.putItem(catalog);

            LOGGER.info("Neuer Katalog erstellt: {}", uuid);

            CatalogDTO dto = new CatalogDTO(uuid, catalog.getName(), catalog.getDescription(),
                    catalog.getColor(), 0, catalog.getCreatedAt(), catalog.getUpdatedAt());
            return Utility.getApiResponse(201, Utility.objectMapper.writeValueAsString(dto), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Erstellen des Katalogs: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
