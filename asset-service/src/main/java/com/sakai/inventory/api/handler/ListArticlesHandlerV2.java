package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.dto.PaginatedArticlesResponseDTO;
import com.sakai.inventory.domain.model.Article;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.*;

public class ListArticlesHandlerV2 implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListArticlesHandlerV2.class);

    private static final int MAX_PAGE_SIZE = 100;

    public ListArticlesHandlerV2() {
        super();
        LOGGER.info("ListArticlesHandlerV2");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> header = ResponseUtil.createHeaders();
        header.put("Access-Control-Allow-Origin", "*"); // CORS falls benötigt

        // get Query Parameter
        Map<String, String> queryParams = request.getQueryStringParameters();
        Map<String, AttributeValue> exclusiveStartKey = getExclusiveStartKey(queryParams);
        int pageSize = getPageSize(queryParams);

        LOGGER.info("Request: pageSize={}, hasNextToken={}", pageSize, exclusiveStartKey);

        try {
            ListArticlesHandlerV2.PaginationResult result = getAllArticlesPaginated(pageSize, exclusiveStartKey);
            List<ArticleDTO> articlesDTO = mapArticles(result.articles);
            PaginatedArticlesResponseDTO responseDTO = new PaginatedArticlesResponseDTO(
                    articlesDTO,
                    result.nextToken,
                    articlesDTO.size(),
                    result.nextToken != null
            );
            String responseString = ResponseUtil.objectMapper.writeValueAsString(responseDTO);
            return ResponseUtil.createApiResponse(200, responseString, header);
        } catch (Exception e) {
            LOGGER.error("Failed to list articles", e);
            return ResponseUtil.createApiResponse(500, "{\"error\": \"Internal server error\"}", header);
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

            Map<String, String> value = ResponseUtil.objectMapper.readValue(json, new TypeReference<>() {
            });
            Map<String, AttributeValue> startPage = new HashMap<>();

            startPage.put(
                    "partitionKey",
                    AttributeValue.builder()
                            .s(value.get("partitionKey"))
                            .build()
            );
            startPage.put(
                    "sortKey",
                    AttributeValue.builder()
                            .s(value.get("sortKey"))
                            .build()
            );
            return startPage;
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
            Map<String, String> nextPage = new HashMap<>();

            for (Map.Entry<String, AttributeValue> entry : lastEvaluatedKey.entrySet()) {
                AttributeValue attributeValue = entry.getValue();

                if (attributeValue.s() != null) {
                    nextPage.put(entry.getKey(), attributeValue.s());
                } else if (attributeValue.n() != null) {
                    nextPage.put(entry.getKey(), attributeValue.n());
                }
            }
            String json = ResponseUtil.objectMapper.writeValueAsString(nextPage);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private ListArticlesHandlerV2.PaginationResult getAllArticlesPaginated(Integer pageSize, Map<String, AttributeValue> exclusiveStartKey) {
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

            return new ListArticlesHandlerV2.PaginationResult(articles, nextToken);
        }

        return new ListArticlesHandlerV2.PaginationResult(List.of(), null);
    }

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
