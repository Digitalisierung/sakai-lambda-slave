package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * repository interface for Article domain objects.
 * Provides domain-oriented operations for article management.
 */
public interface ArticleRepository {
    /**
     * Find article by its unique identifier.
     *
     * @param id equal partitionKey.
     * @return Optional<EnhancedDocument>
     */
    Optional<EnhancedDocument> findById(String id);

    /**
     * Find article by its unique identifier.
     *
     * @param articleId equal UUID part of sortKey.
     * @return Optional<EnhancedDocument> of Articles.
     */
    Optional<EnhancedDocument> findArticleById(String articleId);

    /**
     * Find all articles with pagination support. Uses Query.
     *
     * @param limit             Maximum number of articles to return.
     * @param exclusiveStartKey DynamoDb pagination token (null for first page).
     * @return PaginatedResult result with articles and next page token.
     */
    PaginatedResult<EnhancedDocument> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey);
}
