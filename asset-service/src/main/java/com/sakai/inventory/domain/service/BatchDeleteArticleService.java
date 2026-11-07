package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.ArticleDeleteDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.shared.exception.FailedValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BatchDeleteArticleService {

    private final ArticleRepository articleRepository;

    public BatchDeleteArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<String> batchDeleteArticles(List<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            throw new FailedValidationException("articleIds must not be null or empty.");
        }

        List<Map<String, Object>> foundItems = articleRepository.batchFindViaGsi(articleIds);

        Set<String> foundIds = foundItems.stream()
                .map(item -> (String) item.get("id"))
                .collect(Collectors.toSet());

        List<String> notFound = articleIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        List<ArticleDeleteDTO> toDelete = foundItems.stream()
                .map(item -> new ArticleDeleteDTO((String) item.get("id"), (String) item.get("catalogId")))
                .toList();

        List<String> unprocessedFromDb = toDelete.isEmpty() ? List.of() : articleRepository.batchDeleteItems(toDelete);

        List<String> unprocessed = new ArrayList<>(notFound);
        unprocessed.addAll(unprocessedFromDb);
        return unprocessed;
    }
}
