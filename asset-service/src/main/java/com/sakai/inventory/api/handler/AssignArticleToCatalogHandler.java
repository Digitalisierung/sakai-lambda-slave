package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Article;
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
 * BE-16: POST /catalogs/{id}/articles
 * Weist einen Artikel einem Katalog zu.
 * Request-Body: { "articleId": "<uuid>" }
 *
 * Vorgehen:
 *   1. Katalog und Artikel per UUID-Query suchen (vollständige sortKeys ermitteln).
 *   2. catalogId am Artikel-Item aktualisieren.
 *   3. productCount am Katalog erhöhen (+1).
 */
public class AssignArticleToCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(AssignArticleToCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende POST /catalogs/{id}/articles Anfragen.
     * Prüft die Existenz von Katalog und Artikel, setzt die catalogId am Artikel-Item
     * und erhöht den productCount am Katalog-Item atomisch um 1.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit Pfadparameter "id"
     *                und JSON-Body mit "articleId"
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 bei erfolgreicher Zuweisung, 400/404 bei Fehlern, 500 bei Systemfehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String catalogUuid = pathParams.get("id");

            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            @SuppressWarnings("unchecked")
            Map<String, String> bodyMap = Utility.objectMapper.readValue(body, Map.class);
            String articleUuid = bodyMap.get("articleId");
            if (articleUuid == null || articleUuid.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'articleId' fehlt.\"}", Utility.getHeaders());
            }

            DynamoDbTable<Catalog> catalogTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));
            DynamoDbTable<Article> articleTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            // Beide per UUID-Query suchen
            Catalog catalog = KeyHelper.findCatalogById(catalogTable, catalogUuid);
            if (catalog == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            Article article = KeyHelper.findArticleById(articleTable, articleUuid);
            if (article == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            String now = Instant.now().toString();

            // Artikel: catalogId auf die UUID setzen
            Article articlePatch = new Article();
            articlePatch.setPartitionKey(article.getPartitionKey());
            articlePatch.setSortKey(article.getSortKey());
            articlePatch.setCatalogId(catalogUuid);
            articlePatch.setUpdatedAt(now);
            articleTable.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(articlePatch).ignoreNulls(true).build());

            // Katalog: productCount erhöhen
            int newCount = (catalog.getProductCount() != null ? catalog.getProductCount() : 0) + 1;
            Catalog catalogPatch = new Catalog();
            catalogPatch.setPartitionKey(catalog.getPartitionKey());
            catalogPatch.setSortKey(catalog.getSortKey());
            catalogPatch.setProductCount(newCount);
            catalogPatch.setUpdatedAt(now);
            catalogTable.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(catalogPatch).ignoreNulls(true).build());

            LOGGER.info("Artikel {} dem Katalog {} zugewiesen.", articleUuid, catalogUuid);
            return Utility.getApiResponse(200,
                    "{\"message\": \"Artikel erfolgreich dem Katalog zugewiesen.\"}",
                    Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Zuweisen: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
