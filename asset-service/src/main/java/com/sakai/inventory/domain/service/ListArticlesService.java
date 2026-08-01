package com.sakai.inventory.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.PaginatedArticlesResponseDTO;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import com.sakai.inventory.shared.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListArticlesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListArticlesService.class);

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private ArticleRepository articleRepository;

    public ListArticlesService() {
        super();
    }

    public PaginatedArticlesResponseDTO listArticlesPaginated(int pageSize, String nextToken) {
        LOGGER.info("Listing articles...");

        Map<String, AttributeValue> exclusiveStartKey = decodeNextToken(nextToken);

        DynamoDbEnhancedClient enhancedClient = DynamoDbFactory.createEnhancedClient();
        String tableName = DynamoDbFactory.getTableName();
        TableSchema<EnhancedDocument> tableSchema = DynamoDbFactory.createTableSchema();
        articleRepository = new DynamoDbArticleRepository(enhancedClient, tableName, tableSchema);
        PaginatedResult<EnhancedDocument> paginatedResult = articleRepository.findAll(pageSize, exclusiveStartKey);

        List<EnhancedDocument> items = paginatedResult.items();

        String articles = items.stream()
                .map(EnhancedDocument::toJson)
                .collect(Collectors.joining(",", "[", "]"));

        String encodedNextToken = encodeNextToken(paginatedResult.lastEvaluatedKey());

        LOGGER.info("Articles listed successfully. Returned {} items. hasMore='{}'.", items.size(), encodedNextToken != null);
        LOGGER.debug("Returned articles payload: {}", articles);

        return new PaginatedArticlesResponseDTO(articles, encodedNextToken, items.size(), encodedNextToken != null);

    }

    /**
     * Decode nextToken from Base64 JSON to DynamoDb map.
     *
     * @param nextToken
     * @return Map<> - start key for the next page.
     */
    private Map<String, AttributeValue> decodeNextToken(String nextToken) {
        if (nextToken == null || nextToken.isBlank()) {
            return null;
        }

        byte[] decoded = Base64.getDecoder().decode(nextToken);
        String json = new String(decoded);
        LOGGER.debug("Decoded nextToken successfully. Decoded token='{}'", json);

        try {
            Map<String, String> tokenMap = JsonUtil.parseFromJsonToObject(json, new TypeReference<>() {
            });

            Map<String, AttributeValue> startKey = new HashMap<>();
            for (Map.Entry<String, String> entry : tokenMap.entrySet()) {
                if (entry.getValue() != null) {
                    startKey.put(entry.getKey(), AttributeValue.builder()
                            .s(entry.getValue())
                            .build());
                }
            }

            return startKey;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to decode nextToken.", e);
            throw new IllegalArgumentException("Invalid nextToken format.", e);
        }
    }

    private String encodeNextToken(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }

        Map<String, String> tokenMap = new HashMap<>();

        for (Map.Entry<String, AttributeValue> entry : lastEvaluatedKey.entrySet()) {
            AttributeValue value = entry.getValue();
            if (value.s() != null) {
                tokenMap.put(entry.getKey(), value.s());
            } else if (value.n() != null) {
                tokenMap.put(entry.getKey(), value.n());
            }
        }

        try {
            String json = JsonUtil.convertToJson(tokenMap);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes());
            LOGGER.debug("Encoded nextToken successfully. Encoded token='{}'", encoded);

            return encoded;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to encode nextToken.", e);
            throw new RuntimeException("Failed to create pagination token.", e);
        }
    }
}
