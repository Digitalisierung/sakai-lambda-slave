package com.sakai.inventory.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.api.dto.PaginatedCatalogsResponseDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import com.sakai.inventory.shared.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCatalogsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCatalogsService.class);

    private final CatalogRepository catalogRepository;

    public ListCatalogsService(final CatalogRepository catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    // TODO: Anzahl von Artikeln im Katalog ermitteln (articleCount).

    /**
     *
     * @param pageSize
     * @param nextToken
     * @return PaginatedCatalogsResponseDTO
     */
    public PaginatedCatalogsResponseDTO listCatalogsPaginated(int pageSize, final String nextToken) {
        LOGGER.info("Listing catalogs with page size {} and nextToken {}.", pageSize, nextToken);

        Map<String, AttributeValue> exclusiveStartKey = decodeNextToken(nextToken);
        PaginatedResult<Catalog> paginatedResult = catalogRepository.findAll(pageSize, exclusiveStartKey);

        List<Catalog> catalogs = paginatedResult.items();

        // mappen to DTO
        List<CatalogDTO> dtoList = CatalogMapper.MAPPER.toDomainList(catalogs);
        String encodedNextToken = encodeNextToken(paginatedResult.lastEvaluatedKey());
        // create PaginatedCatalogsResponseDTO
        return new PaginatedCatalogsResponseDTO(dtoList, encodedNextToken, dtoList.size(), encodedNextToken != null);
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

    /**
     *
     * @param lastEvaluatedKey
     * @return String - encoded nextToken.
     */
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
            } else if (value.bool() != null) {
                tokenMap.put(entry.getKey(), String.valueOf(value.bool()));
            }
        }

        try {
            String json = JsonUtil.convertToJson(tokenMap);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes());
            LOGGER.debug("Encoded nextToken successfully. Encoded token='{}'", encoded);
            return encoded;
        } catch (Exception e) {
            LOGGER.error("Failed to encode nextToken.", e);
            throw new RuntimeException("Failed to create pagination token.", e);
        }
    }
}
