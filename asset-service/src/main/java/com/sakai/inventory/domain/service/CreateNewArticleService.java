package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.BatchCreateResponseDTO;
import com.sakai.inventory.api.dto.BatchCreateResponseDTO.FailedEntryDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateNewArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNewArticleService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public CreateNewArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public BatchCreateResponseDTO batchWriteArticles(List<EnhancedDocument> articles) {
        List<EnhancedDocument> itemsToSave = new ArrayList<>();
        List<FailedEntryDTO> failed = new ArrayList<>();

        // Prüfen, dass mindestens Name, SKU und createdAt vorhanden sind (Pflichtfelder).
        for (EnhancedDocument doc : articles) {
            String validationError = validateRequiredFields(doc);
            if (validationError != null) {
                failed.add(buildFailedEntryDTO(doc, validationError));
                continue;
            }
            itemsToSave.add(doc);
        }

        if (itemsToSave.isEmpty()) {
            String message = String.format("All %d articles failed validation.", articles.size());
            LOGGER.warn(message);
            return new BatchCreateResponseDTO("[]", failed);
        }

        // weitere Validierung: für active, isFeatured und updatedAt default-Werte setzen, wenn keine Werte vorhanden sind.
        setDefault(itemsToSave);

        List<EnhancedDocument> unprocessed = articleRepository.batchSave(itemsToSave);
        // jetzt unprecessed aus itemsToSave entfernen und nach failed verschieben.
        if (unprocessed != null && !unprocessed.isEmpty()) {
            itemsToSave.removeAll(unprocessed);
            unprocessed.stream()
                    .map(doc -> buildFailedEntryDTO(doc, "DynamoDB write failed (unprocessed item)"))
                    .forEach(failed::add);
        }


        String created = itemsToSave.stream()
                .map(EnhancedDocument::toJson)
                .collect(Collectors.joining(",", "[", "]"));

        return new BatchCreateResponseDTO(created, failed);
    }

    private String validateRequiredFields(EnhancedDocument doc) {
        String sku = doc.get("sku", String.class);
        if (sku == null || sku.isBlank()) return "SKU is required";
        String name = doc.get("name", String.class);
        if (name == null || name.isBlank()) return "Name is required";
        String createdAt = doc.get("createdAt", String.class);
        if (createdAt == null || createdAt.isBlank()) return "CreatedAt is required";
        return null;
    }

    private void setDefault(List<EnhancedDocument> documents) {
        for (EnhancedDocument doc : documents) {
            Map<String, AttributeValue> map = doc.toMap();
            AttributeValue createdAt = map.get("updatedAt");
            if (createdAt == null || createdAt.s() == null || createdAt.s().isBlank()) {
                map.put("updatedAt", AttributeValue.builder()
                        .s(map.get("createdAt").s())
                        .build()
                );
            }
            AttributeValue active = map.get("active");
            if (active == null || active.bool() == null) {
                map.put("active", AttributeValue.builder()
                        .bool(false)
                        .build()
                );
            }
            AttributeValue featured = map.get("isFeatured");
            if (featured == null || featured.bool() == null) {
                map.put("isFeatured", AttributeValue.builder()
                        .bool(false)
                        .build()
                );
            }
        }
    }

    private FailedEntryDTO buildFailedEntryDTO(EnhancedDocument document, String message) {
        String name = document.getString("name");
        String sku = document.getString("sku");
        return new FailedEntryDTO(-1, name, sku, message);
    }
}
