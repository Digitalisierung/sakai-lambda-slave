package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.UpdateArticleRequestDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateArticleService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public UpdateArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public EnhancedDocument update(String catalogId, String articleId, UpdateArticleRequestDTO request) {
        LOGGER.info("Updating article. articleId='{}'", articleId);

        validateRequest(request);

        Map<String, AttributeValue> fields = buildUpdateFields(request);

        EnhancedDocument updated = articleRepository.update(catalogId, articleId, fields);
        LOGGER.info("Article updated successfully. articleId='{}'", articleId);
        return updated;
    }

    private void validateRequest(UpdateArticleRequestDTO request) {
        if (request.name() != null && request.name().isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (request.sku() != null && request.sku().isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
    }

    private Map<String, AttributeValue> buildUpdateFields(UpdateArticleRequestDTO request) {
        Map<String, AttributeValue> fields = new LinkedHashMap<>();

        // updatedAt wird immer vom Backend gesetzt
        fields.put("updatedAt", AttributeValue.builder().s(Instant.now().toString()).build());

        if (request.name() != null) fields.put("name", AttributeValue.builder().s(request.name()).build());
        if (request.sku() != null) fields.put("sku", AttributeValue.builder().s(request.sku()).build());
        if (request.description() != null)
            fields.put("description", AttributeValue.builder().s(request.description()).build());
        if (request.imageUrl() != null) fields.put("imageUrl", AttributeValue.builder().s(request.imageUrl()).build());
        if (request.state() != null) fields.put("state", AttributeValue.builder().s(request.state()).build());
        if (request.isFeatured() != null)
            fields.put("isFeatured", AttributeValue.builder().bool(request.isFeatured()).build());

        // stock, catalogId, entityType, id, createdAt werden bewusst ignoriert

        return fields;
    }
}
