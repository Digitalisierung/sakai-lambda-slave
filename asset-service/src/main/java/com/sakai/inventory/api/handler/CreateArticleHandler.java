package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.dto.CreateArticleRequest;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

/**
 * BE-08: POST /articles
 * Legt einen neuen Artikel an.
 * Pflichtfelder: name, sku
 *
 * sortKey-Format: ARTICLES#<uuid>
 * Die UUID ist gleichzeitig die API-seitige ID des Artikels.
 */
public class CreateArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(CreateArticleHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Request-Body fehlt.\"}", Utility.getHeaders());
            }

            CreateArticleRequest req = Utility.objectMapper.readValue(body, CreateArticleRequest.class);

            if (req.name() == null || req.name().isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'name' fehlt.\"}", Utility.getHeaders());
            }
            if (req.sku() == null || req.sku().isBlank()) {
                return Utility.getApiResponse(400, "{\"message\": \"Pflichtfeld 'sku' fehlt.\"}", Utility.getHeaders());
            }

            String uuid = UUID.randomUUID().toString();
            String now = Instant.now().toString();

            // sortKey-Format: ARTICLES#<uuid> — UUID ist letztes Segment, daher von KeyHelper extrahierbar
            Article article = new Article();
            article.setPartitionKey("ARTICLES");
            article.setSortKey("ARTICLES#" + uuid);
            article.setName(req.name());
            article.setSku(req.sku());
            article.setDescription(req.description());
            article.setPrice(req.price());
            article.setStock(req.stock() != null ? req.stock() : 0);
            article.setImageUrl(req.imageUrl());
            article.setCatalogId(req.catalogId());
            article.setFeatured(req.isFeatured() != null ? req.isFeatured() : false);
            article.setState("AVAILABLE");
            article.setCreatedAt(now);
            article.setUpdatedAt(now);

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));
            table.putItem(article);

            LOGGER.info("Neuer Artikel erstellt: {}", uuid);

            ArticleDTO dto = new ArticleDTO(
                    uuid,
                    article.getName(), article.getSku(), article.getDescription(),
                    article.getPrice() != null ? article.getPrice().toString() : null,
                    article.getStock() != null ? article.getStock().longValue() : null,
                    article.getImageUrl(), article.getCatalogId(),
                    true, article.getFeatured(), new HashMap<>(),
                    article.getCreatedAt(), article.getUpdatedAt()
            );

            return Utility.getApiResponse(201, Utility.objectMapper.writeValueAsString(dto), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Erstellen des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
