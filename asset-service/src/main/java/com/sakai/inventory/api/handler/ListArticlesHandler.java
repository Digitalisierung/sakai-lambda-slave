package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import utility.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ListArticlesHandler class implements the AWS Lambda RequestHandler interface to process a
 * request and provide a response for listing articles. It fetches, maps, and returns article data in JSON format.
 */
public class ListArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ListArticlesHandler.class);

    public ListArticlesHandler() {
        LOGGER.info("ListArticlesHandler constructor");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent s, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);


        DynamoDbClient dbClient = DynamoDbClient.builder()
                .httpClientBuilder(AwsCrtHttpClient.builder())
                .region(Region.EU_CENTRAL_1)
//                .credentialsProvider(
//                        StaticCredentialsProvider.create(
//                                AwsBasicCredentials.builder()
//                                        .accessKeyId("test")
//                                        .secretAccessKey("test")
//                                        .build()
//                        )
//                )
                .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dbClient)
                .build();

        DynamoDbTable<Article> articleTable = enhancedClient.table(
                System.getenv("TABLE_NAME"),
                TableSchema.fromBean(Article.class)
        );

        QueryConditional query = QueryConditional.keyEqualTo(
                Key.builder()
                        .partitionValue("ARTICLES")
                        .build()
        );
        List<Article> articlesList = articleTable.query(query)
                .items()
                .stream()
                .toList();

        LOGGER.info("Number of Articles: {}", articlesList.size());

        List<ArticleDTO> articleDTOs = mapArticles(articlesList);

        try {
            response.setBody(Utility.objectMapper.writeValueAsString(articleDTOs));
            response.setStatusCode(200);
            LOGGER.info("GetCatalogsHandler request successful");
        } catch (JsonProcessingException e) {
            response.setStatusCode(500);
            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
            LOGGER.error(e.getMessage(), e);
        }


        return response;
    }

    private List<ArticleDTO> mapArticles(List<Article> articles) {
        List<ArticleDTO> articleDTOS = new ArrayList<>();

        for (Article article : articles) {
            articleDTOS.add(new ArticleDTO(
                    article.getSortKey(),
                    article.getName(),
                    article.getSku(),
                    article.getDescription(),
                    article.getPrice().toString(),
                    article.getInventory().longValue(),
                    article.getImageUrl(),
                    article.getCatalogId(),
                    true,
                    true,
                    new HashMap<>(),
                    article.getCreatedAt(),
                    article.getUpdatedAt()));
        }

        return articleDTOS;
    }
}
