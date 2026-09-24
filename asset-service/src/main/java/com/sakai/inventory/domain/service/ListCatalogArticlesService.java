package com.sakai.inventory.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.PaginatedArticlesResponseDTO;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.shared.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListCatalogArticlesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCatalogArticlesService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public ListCatalogArticlesService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public PaginatedArticlesResponseDTO findArticlesInCatalog(final String catalogId, final String nextToken, final int pageSize) {
        LOGGER.info("Finding articles in a catalog...");

        Map<String, AttributeValue> exclusiveStartKey = this.decodeToken(nextToken);
        PaginatedResult<EnhancedDocument> paginatedResult = this.articleRepository.findCatalogArticles(catalogId, pageSize, exclusiveStartKey);

        String encodedNextToken = this.encodeToken(paginatedResult.lastEvaluatedKey());

        List<EnhancedDocument> items = paginatedResult.items();
        String articles = "[]";
        int totalReturned = 0;

        if (items != null) {
            totalReturned = items.size();
            articles = items.stream()
                    .map(EnhancedDocument::toJson)
                    .collect(Collectors.joining(",", "[", "]"));
        }

        LOGGER.info("Articles listed successfully. Returned {} items. hasMore='{}'.", totalReturned, encodedNextToken != null);
        LOGGER.debug("Returned articles payload: {}", articles);

        return new PaginatedArticlesResponseDTO(articles, encodedNextToken, totalReturned, encodedNextToken != null);
    }

    /**
     * Decode nextToken from Base64 JSON to DynamoDb map.
     *
     * @param nextToken pointer to the last element of the previous page.
     * @return Map<> - start key for the next page.
     */
    private Map<String, AttributeValue> decodeToken(String nextToken) {
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

            for (Map.Entry<String, String> item : tokenMap.entrySet()) {
                if (item.getValue() != null) {
                    startKey.put(
                            item.getKey(), AttributeValue.builder()
                                    .s(item.getValue())
                                    .build()
                    );
                }
            }
            return startKey;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to decode nextToken.", e);
            throw new IllegalArgumentException("Invalid nextToken format.", e);
        }
    }

    /**
     * Encode nextToken from DynamoDb map to Base64 JSON.
     *
     * @param lastEvaluatedKey - last evaluated key from previous page.
     * @return String - Base64 JSON string.
     */
    private String encodeToken(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }

        /**
         * 1. Convert Map<String, AttributeValue> to Map<String, String>.
         * 2. Convert Map<String, String> to String JSON.
         * 3. Encode String JSON to Base64.
         */
        Map<String, String> tokenMap = new HashMap<>();

        for (Map.Entry<String, AttributeValue> entry : lastEvaluatedKey.entrySet()) {
            AttributeValue value = entry.getValue();
            if (value.s() != null) {
                tokenMap.put(entry.getKey(), value.s());
            } else if (value.n() != null) {
                tokenMap.put(entry.getKey(), value.n());
            } else if (value.bool() != null) {
                tokenMap.put(entry.getKey(), String.valueOf(value.bool()));
            }
        }

        try {
            String json = JsonUtil.convertToJson(tokenMap);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            LOGGER.debug("Encoded nextToken successfully. Encoded token='{}'", encoded);

            return encoded;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to encode nextToken.", e);
            throw new RuntimeException("Failed to create pagination token.", e);
        }
    }
}
