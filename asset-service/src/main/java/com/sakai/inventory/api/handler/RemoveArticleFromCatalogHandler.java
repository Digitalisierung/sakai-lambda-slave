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
 * BE-17: DELETE /catalogs/{id}/articles/{articleId}
 * Entfernt einen Artikel aus einem Katalog.
 *
 * Vorgehen:
 *   1. Katalog und Artikel prüfen.
 *   2. catalogId auf dem Artikel-Item leeren.
 *   3. productCount auf dem Katalog-Item verringern (-1, minimum 0).
 */
public class RemoveArticleFromCatalogHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(RemoveArticleFromCatalogHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

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
            String catalogId  = pathParams.get("id");
            String articleId  = pathParams.get("articleId");

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

            // Prüfen ob der Artikel wirklich diesem Katalog zugehört
            if (!catalogId.equals(article.getCatalogId())) {
                return Utility.getApiResponse(400,
                        "{\"message\": \"Dieser Artikel ist dem angegebenen Katalog nicht zugewiesen.\"}",
                        Utility.getHeaders());
            }

            String now = Instant.now().toString();

            // Artikel: catalogId leeren — DynamoDB UpdateExpression REMOVE wird über Low-Level-API benötigt,
            // da ignoreNulls das Setzen auf null ignoriert. Wir setzen catalogId auf leeren String als
            // Konvention für "kein Katalog zugewiesen".
            Article articlePatch = new Article();
            articlePatch.setPartitionKey("ARTICLES");
            articlePatch.setSortKey("ARTCL#" + articleId);
            articlePatch.setCatalogId("");
            articlePatch.setUpdatedAt(now);
            articleTable.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(articlePatch)
                    .ignoreNulls(true)
                    .build());

            // Katalog: productCount verringern (minimum 0)
            int newCount = Math.max(0, (catalog.getProductCount() != null ? catalog.getProductCount() : 0) - 1);
            Catalog catalogPatch = new Catalog();
            catalogPatch.setPartitionKey("CATALOGS");
            catalogPatch.setSortKey("CAT#" + catalogId);
            catalogPatch.setProductCount(newCount);
            catalogPatch.setUpdatedAt(now);
            catalogTable.updateItem(UpdateItemEnhancedRequest.builder(Catalog.class)
                    .item(catalogPatch)
                    .ignoreNulls(true)
                    .build());

            LOGGER.info("Artikel {} aus Katalog {} entfernt.", articleId, catalogId);
            return Utility.getApiResponse(200,
                    "{\"message\": \"Artikel erfolgreich aus dem Katalog entfernt.\"}",
                    Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Entfernen des Artikels aus dem Katalog: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
