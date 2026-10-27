package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.shared.exception.FailedValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

public class UpdateArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateArticleService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public UpdateArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public Map<String, Object> update(String articleId, Map<String, Object> requestMap) {
        LOGGER.info("Updating all article. articleId='{}'", articleId);

        // aus map sollte stock entfernt werden, da immutable und createdAt (auch immutable).
        // name und sku, falls vorhanden, dürfen nicht null oder leer sein.
        // updatedAt soll aktualisiert werden.
        String validateError = validateRequest(requestMap);
        if (validateError != null) throw new FailedValidationException(validateError);

        requestMap.remove("stock");
        requestMap.remove("createdAt");
        String updateTime = Instant.now(Clock.systemUTC()).toString();
        requestMap.put("updatedAt", updateTime);
        String catalogId = (String) requestMap.get("catalogId");
        return articleRepository.update(catalogId, articleId, requestMap);
    }

    private String validateRequest(Map<String, Object> request) {
        if (request.containsKey("name")) {
            String name = (String) request.get("name");
            if (name == null || name.isBlank()) return "Name must not be empty";
        }

        if (request.containsKey("sku")) {
            String sku = (String) request.get("sku");
            if (sku == null || sku.isBlank()) return "SKU must not be empty";
        }

        String catalogId = (String) request.get("catalogId");
        if (catalogId == null || catalogId.isBlank()) return "catalogId must not be empty";

        return null;
    }
}
