package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
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
 *
 * Request-Body: { "articleId": "abc123" }
 *
 * Vorgehen (Single-Table Design):
 *   1. Katalog und Artikel prüfen (beide müssen existieren).
 *   2. articleId auf dem Artikel-Item aktualisieren (catalogId = catalogId).
 *   3. productCount auf dem Katalog-Item erhöhen (+1).
 */
public class AssignArticleToCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(AssignArticleToCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

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

            @SuppressWarnings("unchecked")
            Map<String, String> bodyMap = Utility.objectMapper.readValue(body, Map.class);
            String articleId = bodyMap.get("articleId");
            if (articleId == null || articleId.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'articleId' fehlt.\"}", Utility.getHeaders());
            }

            DynamoDbTable<Catalog> catalogTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));
            DynamoDbTable<Article> articleTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            // Katalog prüfen
            Key catalogKey = Key.builder()
                    .partitionValue("CATALOGS")
                    .sortValue("CAT#" + catalogId)
                    .build();
            Catalog catalog = catalogTable.getItem(catalogKey);
            if (catalog == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            // Artikel prüfen
            Key articleKey = Key.builder()
                    .partitionValue("ARTICLES")
                    .sortValue("ARTCL#" + articleId)
                    .build();
            Article article = articleTable.getItem(articleKey);
            if (article == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            String now = Instant.now().toString();

            // Artikel: catalogId setzen
            Article articlePatch = new Article();
            articlePatch.setPartitionKey("ARTICLES");
            articlePatch.setSortKey("ARTCL#" + articleId);
            articlePatch.setCatalogId(catalogId);
            articlePatch.setUpdatedAt(now);
            articleTable.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(articlePatch)
                    .ignoreNulls(true)
                    .build());

            // Katalog: productCount erhöhen
            int newCount = (catalog.getProductCount() != null ? catalog.getProductCount() : 0) + 1;
            Catalog catalogPatch = new Catalog();
            catalogPatch.setPartitionKey("CATALOGS");
            catalogPatch.setSortKey("CAT#" + catalogId);
            catalogPatch.setProductCount(newCount);
            catalogPatch.setUpdatedAt(now);
            catalogTable.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(catalogPatch)
                    .ignoreNulls(true)
                    .build());

            LOGGER.info("Artikel {} dem Katalog {} zugewiesen.", articleId, catalogId);
            return Utility.getApiResponse(200,
                    "{\"message\": \"Artikel erfolgreich dem Katalog zugewiesen.\"}",
                    Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Zuweisen des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
