package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.dto.UpdateArticleRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import utility.Utility;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * BE-09: PUT /articles/{id}
 * Partial Update — nur gesetzte Felder werden überschrieben.
 * Sucht den Artikel via UUID (letztes sortKey-Segment), da der vollständige sortKey unbekannt ist.
 */
public class UpdateArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(UpdateArticleHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende PUT /articles/{id} Anfragen (Partial Update).
     * Sucht den Artikel anhand der UUID, übernimmt nur die im Body gesetzten Felder
     * und aktualisiert den Zeitstempel "updatedAt" automatisch.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit Pfadparameter "id" und JSON-Body
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 mit dem aktualisierten Artikel, 400/404 bei Fehlern, 500 bei Systemfehler
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

            UpdateArticleRequest req = Utility.objectMapper.readValue(body, UpdateArticleRequest.class);

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            // Vollständigen sortKey via Query ermitteln
            Article existing = KeyHelper.findArticleById(table, id);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            // Patch mit dem tatsächlichen Key aus der DB
            Article patch = new Article();
            patch.setPartitionKey(existing.getPartitionKey());
            patch.setSortKey(existing.getSortKey());
            if (req.name() != null)        patch.setName(req.name());
            if (req.sku() != null)         patch.setSku(req.sku());
            if (req.description() != null) patch.setDescription(req.description());
            if (req.price() != null)       patch.setPrice(req.price());
            if (req.stock() != null)       patch.setStock(req.stock());
            if (req.imageUrl() != null)    patch.setImageUrl(req.imageUrl());
            if (req.state() != null)       patch.setState(req.state());
            if (req.catalogId() != null)   patch.setCatalogId(req.catalogId());
            if (req.isFeatured() != null)  patch.setFeatured(req.isFeatured());
            patch.setUpdatedAt(Instant.now().toString());

            table.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(patch)
                    .ignoreNulls(true)
                    .build());

            // Aktuellen Stand lesen und zurückgeben
            Key key = Key.builder()
                    .partitionValue(existing.getPartitionKey())
                    .sortValue(existing.getSortKey())
                    .build();
            Article updated = table.getItem(key);

            LOGGER.info("Artikel aktualisiert: {}", id);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(mapToDTO(updated)), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Aktualisieren des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    /**
     * Wandelt ein Article-Datenbankobjekt in ein ArticleDTO für die API-Antwort um.
     * Die API-seitige ID wird aus dem letzten Segment des sortKey extrahiert.
     *
     * @param article das aus DynamoDB gelesene Article-Objekt
     * @return das befüllte ArticleDTO-Objekt
     */
    private ArticleDTO mapToDTO(Article article) {
        return new ArticleDTO(
                KeyHelper.extractId(article.getSortKey()),
                article.getName(), article.getSku(), article.getDescription(),
                article.getPrice() != null ? article.getPrice().toString() : null,
                article.getStock() != null ? article.getStock().longValue() : null,
                article.getImageUrl(), article.getCatalogId(),
                true, article.getFeatured(), new HashMap<>(),
                article.getCreatedAt(), article.getUpdatedAt()
        );
    }
}
