package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * BE-07: GET /articles/{id}
 * Gibt einen einzelnen Artikel anhand seiner ID zurück.
 * Die ID entspricht dem sortKey in DynamoDB (z.B. "ARTCL#abc123").
 */
public class GetArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(GetArticleHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String articleId = pathParams.get("id");

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            Key key = Key.builder()
                    .partitionValue("ARTICLES")
                    .sortValue("ARTCL#" + articleId)
                    .build();

            Article article = table.getItem(key);

            if (article == null) {
                LOGGER.warn("Artikel nicht gefunden: {}", articleId);
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            ArticleDTO dto = mapToDTO(article);
            String body = Utility.objectMapper.writeValueAsString(dto);
            LOGGER.info("Artikel gefunden: {}", articleId);
            return Utility.getApiResponse(200, body, Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    private ArticleDTO mapToDTO(Article article) {
        return new ArticleDTO(
                article.getSortKey(),
                article.getName(),
                article.getSku(),
                article.getDescription(),
                article.getPrice() != null ? article.getPrice().toString() : null,
                article.getStock() != null ? article.getStock().longValue() : null,
                article.getImageUrl(),
                article.getCatalogId(),
                true,
                article.getFeatured(),
                new HashMap<>(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
