package com.sakai.inventory.domain.service;

import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.util.Optional;

/**
 * Service for managing articles.
 * Contains business logic for operations with single article item.
 */
public class GetArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleService.class);

    private final ArticleRepository articleRepository;

    public GetArticleService(ArticleRepository articleRepository) {
        super();
        this.articleRepository = articleRepository;
    }

    public Optional<EnhancedDocument> findArticleById(String id) {
        return articleRepository.findArticleById("ITEM#" + id);
    }
}
