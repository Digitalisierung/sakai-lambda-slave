package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

public class CreateNewArticleService {
    private final ArticleRepository<EnhancedDocument> articleRepository;

    public CreateNewArticleService(ArticleRepository<EnhancedDocument> articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }
}
