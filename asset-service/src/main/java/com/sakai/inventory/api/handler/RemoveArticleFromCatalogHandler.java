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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import utility.Utility;

import java.time.Instant;
import java.util.Map;

/**
 * BE-17: DELETE /catalogs/{id}/articles/{articleId}
 * Entfernt einen Artikel aus einem Katalog.
 *
 * Vorgehen:
 *   1. Katalog und Artikel per UUID-Query suchen.
 *   2. Prüfen ob der Artikel wirklich diesem Katalog zugehört.
 *   3. catalogId am Artikel leeren.
 *   4. productCount am Katalog verringern (minimum 0).
 */
public class RemoveArticleFromCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(RemoveArticleFromCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende DELETE /catalogs/{id}/articles/{articleId} Anfragen.
     * Prüft die Existenz von Katalog und Artikel sowie deren Zugehörigkeit zueinander.
     * Leert die catalogId am Artikel-Item und verringert den productCount am Katalog (minimum 0).
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit den Pfadparametern "id" und "articleId"
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 bei erfolgreichem Entfernen, 400/404 bei Fehlern, 500 bei Systemfehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null
                    || !pathParams.containsKey("id")
                    || !pathParams.containsKey("articleId")) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"Pfadparameter 'id' und 'articleId' sind erforderlich.\"}",
                        Utility.getHeaders());
            }
            String catalogUuid  = pathParams.get("id");
            String articleUuid  = pathParams.get("articleId");

            DynamoDbTable<Catalog> catalogTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Catalog.class));
            DynamoDbTable<Article> articleTable = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            Catalog catalog = KeyHelper.findCatalogById(catalogTable, catalogUuid);
            if (catalog == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Katalog nicht gefunden.\"}", Utility.getHeaders());
            }

            Article article = KeyHelper.findArticleById(articleTable, articleUuid);
            if (article == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            // Prüfen ob der Artikel diesem Katalog zugeordnet ist
            if (!catalogUuid.equals(article.getCatalogId())) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"Dieser Artikel ist dem angegebenen Katalog nicht zugewiesen.\"}",
                        Utility.getHeaders());
            }

            String now = Instant.now().toString();

            // Artikel: catalogId auf leeren String setzen ("kein Katalog zugewiesen")
            Article articlePatch = new Article();
            articlePatch.setPartitionKey(article.getPartitionKey());
            articlePatch.setSortKey(article.getSortKey());
            articlePatch.setCatalogId("");
            articlePatch.setUpdatedAt(now);
            articleTable.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(articlePatch).ignoreNulls(true).build());

            // Katalog: productCount verringern (minimum 0)
            int newCount = Math.max(0, (catalog.getProductCount() != null ? catalog.getProductCount() : 0) - 1);
            Catalog catalogPatch = new Catalog();
            catalogPatch.setPartitionKey(catalog.getPartitionKey());
            catalogPatch.setSortKey(catalog.getSortKey());
            catalogPatch.setProductCount(newCount);
            catalogPatch.setUpdatedAt(now);
            catalogTable.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(catalogPatch).ignoreNulls(true).build());

            LOGGER.info("Artikel {} aus Katalog {} entfernt.", articleUuid, catalogUuid);
            return Utility.getApiResponse(200,
                    "{\"message\": \"Artikel erfolgreich aus dem Katalog entfernt.\"}",
                    Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Entfernen: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
