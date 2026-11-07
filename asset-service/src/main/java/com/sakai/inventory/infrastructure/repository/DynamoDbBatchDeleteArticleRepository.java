package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.api.dto.ArticleDeleteDTO;

import java.util.List;
import java.util.Map;

public class DynamoDbBatchDeleteArticleRepository implements BatchDeleteArticleRepository {

    private final ArticleRepository<Map<String, Object>> articleRepository;

    public DynamoDbBatchDeleteArticleRepository(ArticleRepository<Map<String, Object>> articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public List<Map<String, Object>> findByIds(List<String> articleIds) {
        return articleRepository.batchFindViaGsi(articleIds);
    }

    @Override
    public List<String> deleteItems(List<ArticleDeleteDTO> deleteItems) {
        return articleRepository.batchDeleteItems(deleteItems);
    }
}
