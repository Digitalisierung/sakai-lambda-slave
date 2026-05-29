package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * BE-07: GET /articles/{id}
 * Gibt einen einzelnen Artikel anhand seiner UUID zurück.
 * Die UUID ist das letzte Segment des sortKey (z.B. "ARTICLES#ART-001#SKU-7001#<uuid>").
 */
public class GetArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(GetArticleHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende GET /articles/{id} Anfragen.
     * Sucht den Artikel anhand der UUID via Query + contains-Filter,
     * da der vollständige sortKey aus der ID allein nicht rekonstruiert werden kann.
     *
     * @param request das eingehende API-Gateway-Request-Objekt mit dem Pfadparameter "id"
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 mit dem gefundenen Artikel, 400 bei fehlender ID, 404 wenn nicht gefunden, 500 bei Fehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return Utility.getApiResponse(400, "{\"message\": \"Pfadparameter 'id' fehlt.\"}", Utility.getHeaders());
            }
            String id = pathParams.get("id");

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            // Query + contains-Filter, da der vollständige sortKey unbekannt ist
            Article article = KeyHelper.findArticleById(table, id);

            if (article == null) {
                LOGGER.warn("Artikel nicht gefunden: {}", id);
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            LOGGER.info("Artikel gefunden: {}", id);
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(mapToDTO(article)), Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    /**
     * Wandelt ein Article-Datenbankobjekt in ein ArticleDTO für die API-Antwort um.
     * Die API-seitige ID wird dabei aus dem letzten Segment des sortKey extrahiert.
     *
     * @param article das aus DynamoDB gelesene Article-Objekt
     * @return das befüllte ArticleDTO-Objekt
     */
    private ArticleDTO mapToDTO(Article article) {
        return new ArticleDTO(
                KeyHelper.extractId(article.getSortKey()),
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
