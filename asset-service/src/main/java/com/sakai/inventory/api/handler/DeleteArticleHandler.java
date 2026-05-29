package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.infrastructure.DynamoDbClientFactory;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import utility.Utility;

import java.util.Map;

/**
 * BE-10: DELETE /articles/{id}
 * Löscht einen Artikel anhand seiner ID.
 */
public class DeleteArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger(DeleteArticleHandler.class);
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

            Article existing = table.getItem(key);
            if (existing == null) {
                return Utility.getApiResponse(404, "{\"message\": \"Artikel nicht gefunden.\"}", Utility.getHeaders());
            }

            table.deleteItem(key);
            LOGGER.info("Artikel gelöscht: {}", articleId);
            return Utility.getApiResponse(204, "", Utility.getHeaders());

        } catch (Exception e) {
            LOGGER.error("Fehler beim Löschen des Artikels: {}", e.getMessage(), e);
            return Utility.getApiResponse(500, "{\"message\": \"" + e.getMessage() + "\"}", Utility.getHeaders());
        }
    }
}
