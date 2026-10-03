package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.BatchCreateResponseDTO;
import com.sakai.inventory.api.dto.BatchCreateResponseDTO.FailedEntryDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.util.ArrayList;
import java.util.List;
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
        List<EnhancedDocument> unprocessedItems = new ArrayList<>();

        // Prüfen, dass mindestens Name, SKU und createdAt vorhanden sind.
        for (EnhancedDocument doc : articles) {
            String validationError = validate(doc);
            if (validationError != null) {
                unprocessedItems.add(doc);
                continue;
            }
            itemsToSave.add(doc);
        }

        if (itemsToSave.isEmpty()) {
            String message = String.format("All %d articles failed validation.", articles.size());
            LOGGER.info(message);
            List<FailedEntryDTO> failed = unprocessedItems.stream()
                    .map(doc -> {
                        return new FailedEntryDTO(-1, doc.getString("name"), doc.getString("sku"), "validationError");
                    })
                    .toList();
            return new BatchCreateResponseDTO("[]", failed);
        }

        List<EnhancedDocument> unprocessed = articleRepository.batchSave(itemsToSave);
        // jetzt unprecessed von itemsToSave entfernen und nach uprocessedItems verschieben.
        if (unprocessed != null && !unprocessed.isEmpty()) {
            itemsToSave.removeAll(unprocessed);
            unprocessedItems.addAll(unprocessed);
        }


        String created = itemsToSave.stream()
                .map(EnhancedDocument::toJson)
                .collect(Collectors.joining(",", "[", "]"));

        List<FailedEntryDTO> failed = unprocessedItems.stream()
                .map(this::buildFailedEntryDTO)
                .toList();

        return new BatchCreateResponseDTO(created, failed);
    }

    private String validate(EnhancedDocument doc) {
        String sku = doc.get("sku", String.class);
        if (sku == null || sku.isBlank()) return "SKU is required";
        String name = doc.get("name", String.class);
        if (name == null || name.isBlank()) return "Name is required";
        String createdAt = doc.get("createdAt", String.class);
        if (createdAt == null || createdAt.isBlank()) return "CreatedAt is required";
        return null;
    }

    private FailedEntryDTO buildFailedEntryDTO(EnhancedDocument document) {
        String name = document.get("name", String.class);
        String sku = document.get("sku", String.class);
        return new FailedEntryDTO(-1, name, sku, "DynamoDB write failed (unprocessed item)");
    }
}
