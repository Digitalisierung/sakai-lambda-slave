package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.ArticleDeleteDTO;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BulkDeleteArticlesService {
    private final ArticleRepository<Map<String, Object>> articleRepository;

    public BulkDeleteArticlesService(ArticleRepository<Map<String, Object>> articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<String> bulkDeleteArticles(List<String> deleteIds) {
        List<ArticleDeleteDTO> articleDeleteDTOS = new ArrayList<>();
        List<Map<String, Object>> items = articleRepository.bulkFindViaGsi(deleteIds);

        for (Map<String, Object> item : items) {
            String articleId = (String) item.get("id");
            String catalogId = (String) item.get("catalogId");
            articleDeleteDTOS.add(new ArticleDeleteDTO(articleId, catalogId));
        }
        return articleRepository.bulkDeleteArticles(articleDeleteDTOS);
    }
}
