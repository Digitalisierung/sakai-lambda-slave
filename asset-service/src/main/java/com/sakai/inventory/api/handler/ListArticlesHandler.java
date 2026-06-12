package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.dto.PaginatedArticlesResponseDTO;
import com.sakai.inventory.api.model.Article;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import utility.Utility;

import java.util.*;

/**
 * The ListArticlesHandler class implements the AWS Lambda RequestHandler interface to process a
 * request and provide a response for listing articles. It fetches, maps, and returns article data in JSON format.
 */
public class ListArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ListArticlesHandler.class);

    private static final int MAX_PAGE_SIZE = 100;

    public ListArticlesHandler() {
        LOGGER.info("ListArticlesHandler constructor");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> header = Utility.getHeaders();
        header.put("Access-Control-Allow-Origin", "*"); // CORS falls benötigt

        // get Query Parameter
        Map<String, String> queryParams = request.getQueryStringParameters();
        Map<String, AttributeValue> exclusiveStartKey = getExclusiveStartKey(queryParams);
        int pageSize = getPageSize(queryParams);

        LOGGER.info("Request: pageSize={}, hasNextToken={}", pageSize, exclusiveStartKey);

        try {
            PaginationResult result = getAllArticlesPaginated(pageSize, exclusiveStartKey);
            List<ArticleDTO> articlesDTO = mapArticles(result.articles);
            PaginatedArticlesResponseDTO responseDTO = new PaginatedArticlesResponseDTO(
                    articlesDTO,
                    result.nextToken,
                    articlesDTO.size(),
                    result.nextToken != null
            );
            String responseString = Utility.objectMapper.writeValueAsString(responseDTO);
            return Utility.getApiResponse(200, responseString, header);
        } catch (Exception e) {
            LOGGER.error("Failed to list articles", e);
            return Utility.getApiResponse(500, "{\"error\": \"Internal server error\"}", header);
        }
    }

    /*
     *    Reads pageSize from query parameters (with validation).
     */
    private int getPageSize(Map<String, String> pageSize) {
        if (pageSize == null || pageSize.isEmpty()) {
            LOGGER.error("Invalid limit. Limit cannot be null or empty.");
            throw new IllegalArgumentException("Invalid limit. Limit cannot be null or empty.");
        }

        try {
            int size = Integer.parseInt(pageSize.get("limit"));
            if (size < 1 || size > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException("Invalid limit. The limit must be between 1 and " + MAX_PAGE_SIZE + ".");
            }
            return size;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid limit. Limit must be a valid number.");
        }
    }


    /*
     *    Decodes nextToken from query parameters.
     */
    private Map<String, AttributeValue> getExclusiveStartKey(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return null;
        }

        try {
            String token = queryParams.get("nextToken");
            byte[] decoded = Base64.getDecoder().decode(token);
            String json = new String(decoded);

            return Utility.objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.error("invalid nextToken", e);
            throw new IllegalArgumentException("Invalid nextToken");
        }
    }

    /*
     *    Encodes LastEvaluatedKey as a Base64 token.
     */
    private String encodeNextToken(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            throw new IllegalArgumentException("nextToke cannot be null or empty");
        }

        try {
            String json = Utility.objectMapper.writeValueAsString(lastEvaluatedKey);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private PaginationResult getAllArticlesPaginated(Integer pageSize, Map<String, AttributeValue> exclusiveStartKey) {
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .exclusiveStartKey(exclusiveStartKey)
                .limit(pageSize)
                .build();

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .httpClientBuilder(AwsCrtHttpClient.builder())
                .region(Region.EU_CENTRAL_1)
                .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        DynamoDbTable<Article> dynamoDbTable = enhancedClient.table(
                System.getenv("TABLE_NAME"),
                TableSchema.fromBean(Article.class)
        );

        PageIterable<Article> pages = dynamoDbTable.scan(scanRequest);
        Iterator<Page<Article>> pageIterator = pages.iterator();
        if (pageIterator.hasNext()) {
            Page<Article> page = pageIterator.next();
            List<Article> articles = page.items();
            String nextToken = encodeNextToken(page.lastEvaluatedKey());

            return new PaginationResult(articles, nextToken);
        }

        return new PaginationResult(List.of(), null);
    }

//    public APIGatewayProxyResponseEvent __handleRequest(APIGatewayProxyRequestEvent s, Context context) {
//        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "application/json");
//        response.setHeaders(headers);
//
//
//        DynamoDbClient dbClient = DynamoDbClient.builder()
//                .httpClientBuilder(AwsCrtHttpClient.builder())
//                .region(Region.EU_CENTRAL_1)

    /// /                .credentialsProvider(
    /// /                        StaticCredentialsProvider.create(
    /// /                                AwsBasicCredentials.builder()
    /// /                                        .accessKeyId("test")
    /// /                                        .secretAccessKey("test")
    /// /                                        .build()
    /// /                        )
    /// /                )
//                .build();
//
//        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
//                .dynamoDbClient(dbClient)
//                .build();
//
//        DynamoDbTable<Article> articleTable = enhancedClient.table(
//                System.getenv("TABLE_NAME"),
//                TableSchema.fromBean(Article.class)
//        );
//
//        QueryConditional query = QueryConditional.keyEqualTo(
//                Key.builder()
//                        .partitionValue("ARTICLES")
//                        .build()
//        );
//        List<Article> articlesList = articleTable.query(query)
//                .items()
//                .stream()
//                .toList();
//
//        LOGGER.info("Number of Articles: {}", articlesList.size());
//
//        List<ArticleDTO> articleDTOs = mapArticles(articlesList);
//
//        try {
//            response.setBody(Utility.objectMapper.writeValueAsString(articleDTOs));
//            response.setStatusCode(200);
//            LOGGER.info("GetCatalogsHandler request successful");
//        } catch (JsonProcessingException e) {
//            response.setStatusCode(500);
//            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
//            LOGGER.error(e.getMessage(), e);
//        } catch (Exception e) {
//            response.setStatusCode(500);
//            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
//            LOGGER.error(e.getMessage(), e);
//        }
//
//
//        return response;
//    }
    private List<ArticleDTO> mapArticles(List<Article> articles) {
        List<ArticleDTO> articleDTOS = new ArrayList<>();

        for (Article article : articles) {
            articleDTOS.add(new ArticleDTO(
                    article.getPartitionKey(),
                    article.getName(),
                    article.getSku(),
                    article.getDescription(),
                    article.getStock() != null ? article.getStock().longValue() : 0L,
                    article.getImageUrl(),
                    article.getCatalogId(),
                    true,
                    true,
                    new HashMap<>(),
                    article.getCreatedAt(),
                    article.getUpdatedAt(),
                    article.getEntityType()
            ));
        }

        return articleDTOS;
    }

    private record PaginationResult(
            List<Article> articles,
            String nextToken
    ) {
    }
}
