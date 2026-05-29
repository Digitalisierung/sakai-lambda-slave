package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.dto.UpdateArticleRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
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
 * Aktualisiert einen bestehenden Artikel (Partial Update).
 * Nur Felder, die im Request-Body gesetzt sind, werden überschrieben.
 */
public class UpdateArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(UpdateArticleHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String articleId = pathParams.get("id");

            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            UpdateArticleRequest req = Utility.objectMapper.readValue(body, UpdateArticleRequest.class);

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            // Prüfen ob Artikel existiert
            Key key = Key.builder()
                    .partitionValue("ARTICLES")
                    .sortValue("ARTCL#" + articleId)
                    .build();
            Article existing = table.getItem(key);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            // Nur gesetzte Felder übernehmen (Partial Update)
            Article patch = new Article();
            patch.setPartitionKey("ARTICLES");
            patch.setSortKey("ARTCL#" + articleId);
            if (req.name() != null)      patch.setName(req.name());
            if (req.sku() != null)       patch.setSku(req.sku());
            if (req.description() != null) patch.setDescription(req.description());
            if (req.price() != null)     patch.setPrice(req.price());
            if (req.stock() != null)     patch.setStock(req.stock());
            if (req.imageUrl() != null)  patch.setImageUrl(req.imageUrl());
            if (req.state() != null)     patch.setState(req.state());
            if (req.catalogId() != null) patch.setCatalogId(req.catalogId());
            if (req.isFeatured() != null) patch.setFeatured(req.isFeatured());
            patch.setUpdatedAt(Instant.now().toString());

            // ignoreNulls=true → nur gesetzte Felder werden in DynamoDB geschrieben
            table.updateItem(UpdateItemEnhancedRequest.builder(Article.class)
                    .item(patch)
                    .ignoreNulls(true)
                    .build());

            // Aktuellen Stand aus DB lesen und zurückgeben
            Article updated = table.getItem(key);
            ArticleDTO dto = new ArticleDTO(
                    updated.getSortKey(), updated.getName(), updated.getSku(),
                    updated.getDescription(),
                    updated.getPrice() != null ? updated.getPrice().toString() : null,
                    updated.getStock() != null ? updated.getStock().longValue() : null,
                    updated.getImageUrl(), updated.getCatalogId(),
                    true, updated.getFeatured(), new HashMap<>(),
                    updated.getCreatedAt(), updated.getUpdatedAt()
            );

            LOGGER.info("Artikel aktualisiert: {}", articleId);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(dto), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Aktualisieren des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
