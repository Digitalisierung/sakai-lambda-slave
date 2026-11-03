package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

public class DeleteArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteArticleService.class);

    private final ArticleRepository<EnhancedDocument> articleRepository;

    public DeleteArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        this.articleRepository = articleRepository;
    }

    public void delete(String articleId) {
        LOGGER.info("Deleting article. articleId='{}'", articleId);
        articleRepository.delete(articleId);
    }
}
