package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.BatchCreateResponseDTO;
import com.sakai.inventory.api.dto.BatchCreateResponseDTO.FailedEntryDTO;
import com.sakai.inventory.api.dto.CreateArticleRequestDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class CreateNewArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNewArticleService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public CreateNewArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public BatchCreateResponseDTO createArticles(List<CreateArticleRequestDTO> requests) {
        List<Map<String, Object>> toSave = new ArrayList<>();
        List<FailedEntryDTO> failed = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateArticleRequestDTO req = requests.get(i);
            String validationError = validate(req);
            if (validationError != null) {
                LOGGER.debug("Validation failed for index {}: {}", i, validationError);
                failed.add(new FailedEntryDTO(i, req.sku(), validationError));
                continue;
            }
            toSave.add(buildDocumentMap(req));
        }

        if (toSave.isEmpty()) {
            LOGGER.info("All {} articles failed validation.", requests.size());
            return new BatchCreateResponseDTO(Collections.emptyList(), failed);
        }

        List<EnhancedDocument> documents = toSave.stream()
                .map(m -> EnhancedDocument.fromAttributeValueMap(toAttributeValueMap(m)))
                .toList();

        List<EnhancedDocument> unprocessed = articleRepository.batchSave(documents);

        Set<String> unprocessedSortKeys = new HashSet<>();
        for (EnhancedDocument doc : unprocessed) {
            unprocessedSortKeys.add(doc.getString("sortKey"));
        }

        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> docMap : toSave) {
            String sortKey = (String) docMap.get("sortKey");
            if (unprocessedSortKeys.contains(sortKey)) {
                String sku = (String) docMap.get("sku");
                failed.add(new FailedEntryDTO(-1, sku, "DynamoDB write failed (unprocessed item)"));
            } else {
                created.add(buildResponseMap(docMap));
            }
        }

        LOGGER.info("Batch create completed. Created: {}, Failed: {}", created.size(), failed.size());
        return new BatchCreateResponseDTO(created, failed);
    }

    private String validate(CreateArticleRequestDTO req) {
        if (req.name() == null || req.name().isBlank()) return "name must not be blank";
        if (req.sku() == null || req.sku().isBlank()) return "sku must not be blank";
        if (req.createdAt() == null || req.createdAt().isBlank()) return "createdAt must not be blank";
        return null;
    }

    private Map<String, Object> buildDocumentMap(CreateArticleRequestDTO req) {
        String id = UUID.randomUUID().toString();
        String catalogId = (req.catalogId() == null || req.catalogId().isBlank()) ? "default" : req.catalogId();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("partitionKey", "ACC#default__CAT#" + catalogId);
        map.put("sortKey", "ITEM#" + id);
        map.put("entityType", "ARTICLES");
        map.put("id", id);
        map.put("name", req.name());
        map.put("sku", req.sku());
        map.put("catalogId", catalogId);
        map.put("createdAt", req.createdAt());
        map.put("updatedAt", req.createdAt());
        map.put("state", "AVAILABLE");
        if (req.description() != null) map.put("description", req.description());
        if (req.stock() != null) map.put("stock", req.stock());
        if (req.imageUrl() != null) map.put("imageUrl", req.imageUrl());
        if (req.isFeatured() != null) map.put("isFeatured", req.isFeatured());
        return map;
    }

    private Map<String, AttributeValue> toAttributeValueMap(Map<String, Object> map) {
        Map<String, AttributeValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof String s) {
                result.put(entry.getKey(), AttributeValue.builder().s(s).build());
            } else if (val instanceof Integer n) {
                result.put(entry.getKey(), AttributeValue.builder().n(n.toString()).build());
            } else if (val instanceof Boolean b) {
                result.put(entry.getKey(), AttributeValue.builder().bool(b).build());
            }
        }
        return result;
    }

    private Map<String, Object> buildResponseMap(Map<String, Object> docMap) {
        Map<String, Object> response = new LinkedHashMap<>(docMap);
        response.remove("partitionKey");
        response.remove("sortKey");
        return response;
    }
}
