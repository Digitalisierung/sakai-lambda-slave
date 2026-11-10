package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.ArticleDeleteDTO;
import com.sakai.inventory.api.dto.BulkDeleteRequestDTO;
import com.sakai.inventory.api.dto.BulkDeleteResponseDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BulkDeleteArticlesService {
    private final ArticleRepository<EnhancedDocument> articleRepository;

    public BulkDeleteArticlesService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public BulkDeleteResponseDTO bulkDeleteArticles(BulkDeleteRequestDTO deleteIds) {
        List<Map<String, Object>> foundItems = articleRepository.bulkFindViaGsi(deleteIds.articleIds());

        // Artikel seinem Katalog zuweisen (catalogId Ermittlung).
        List<ArticleDeleteDTO> toDelete = foundItems.stream()
                .map(item -> new ArticleDeleteDTO((String) item.get("id"), (String) item.get("catalogId")))
                .toList();

        // nur gefundene Artikel löschen.
        List<String> unprocessedIds = articleRepository.bulkDeleteArticles(toDelete);

        List<String> unprocessed = new ArrayList<>(unprocessedIds);

        Set<String> foundIds = foundItems.stream()
                .map(map -> (String) map.get("id"))
                .collect(Collectors.toSet());

        List<String> notFound = deleteIds.articleIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        // DB unprocessed + nicht gefundene Ids (Artikel).
        unprocessed.addAll(notFound);
        return new BulkDeleteResponseDTO(unprocessed);
    }
}
