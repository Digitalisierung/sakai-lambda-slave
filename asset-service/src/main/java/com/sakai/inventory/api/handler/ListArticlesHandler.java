package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.infrastructure.KeyHelper;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import utility.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BE-06: GET /articles
 * Gibt alle Artikel zurück. Unterstützt Query-Parameter zur Filterung:
 *   - name:      Teiltext-Suche im Namen
 *   - state:     Exakter Statuswert (AVAILABLE, SOLD, UNDER_EVALUATION, RESERVED)
 *   - catalogId: Filtert nach Katalog-ID
 *   - sku:       Exakte SKU-Suche
 */
public class ListArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(ListArticlesHandler.class);
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    /**
     * Verarbeitet eingehende GET /articles Anfragen.
     * Liest alle Artikel aus DynamoDB (partitionKey = "ARTICLES") und wendet
     * optionale Filterausdrücke auf Basis der Query-Parameter an.
     *
     * @param request das eingehende API-Gateway-Request-Objekt
     * @param context der Lambda-Ausführungskontext
     * @return HTTP 200 mit JSON-Liste der Artikel, oder 500 bei einem Fehler
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();

            DynamoDbTable<Article> table = DynamoDbClientFactory.getEnhancedClient()
                    .table(TABLE_NAME, TableSchema.fromBean(Article.class));

            QueryEnhancedRequest.Builder queryBuilder = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(
                            Key.builder().partitionValue("ARTICLES").build()
                    ));

            if (queryParams != null && !queryParams.isEmpty()) {
                buildFilterExpression(queryParams, queryBuilder);
            }

            List<Article> articles = table.query(queryBuilder.build())
                    .items()
                    .stream()
                    .toList();

            LOGGER.info("Anzahl gefundener Artikel: {}", articles.size());

            List<ArticleDTO> result = articles.stream().map(this::mapToDTO).toList();
            return Utility.getApiResponse(200, Utility.objectMapper.writeValueAsString(result), Utility.getHeaders());

        } catch (JsonProcessingException e) {
            LOGGER.error("JSON-Fehler: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"Interner Fehler beim Serialisieren.\"}", Utility.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }

    /**
     * Baut eine DynamoDB FilterExpression aus den übergebenen Query-Parametern auf.
     * Unterstützte Parameter: state (exakter Vergleich), catalogId (exakter Vergleich),
     * sku (exakter Vergleich), name (contains-Suche).
     * Mehrere Parameter werden mit AND verknüpft.
     *
     * @param queryParams  die Query-Parameter aus dem HTTP-Request
     * @param queryBuilder der QueryEnhancedRequest-Builder, dem der Filter hinzugefügt wird
     */
    private void buildFilterExpression(Map<String, String> queryParams,
                                       QueryEnhancedRequest.Builder queryBuilder) {
        List<String> conditions = new ArrayList<>();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        if (queryParams.containsKey("state")) {
            conditions.add("#state = :state");
            expressionNames.put("#state", "state");
            expressionValues.put(":state", AttributeValue.fromS(queryParams.get("state")));
        }
        if (queryParams.containsKey("catalogId")) {
            conditions.add("catalogId = :catalogId");
            expressionValues.put(":catalogId", AttributeValue.fromS(queryParams.get("catalogId")));
        }
        if (queryParams.containsKey("sku")) {
            conditions.add("sku = :sku");
            expressionValues.put(":sku", AttributeValue.fromS(queryParams.get("sku")));
        }
        if (queryParams.containsKey("name")) {
            conditions.add("contains(#name, :name)");
            expressionNames.put("#name", "name");
            expressionValues.put(":name", AttributeValue.fromS(queryParams.get("name")));
        }

        if (!conditions.isEmpty()) {
            Expression.Builder expr = Expression.builder()
                    .expression(String.join(" AND ", conditions))
                    .expressionValues(expressionValues);
            if (!expressionNames.isEmpty()) {
                expr.expressionNames(expressionNames);
            }
            queryBuilder.filterExpression(expr.build());
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
        // UUID aus sortKey extrahieren (letztes Segment nach '#')
        String id = KeyHelper.extractId(article.getSortKey());
        return new ArticleDTO(
                id,
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
