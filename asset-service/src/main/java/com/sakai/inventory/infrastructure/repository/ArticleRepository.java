package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Article domain objects.
 * Provides domain-oriented operations for article management.
 */
public interface ArticleRepository<T> {
    /**
     * Find an article by its unique identifier.
     *
     * @param id equal partitionKey.
     * @return Optional<T>
     */
    Optional<T> findById(String id);

    /**
     * Find an article by its unique identifier.
     *
     * @param articleId equal UUID part of sortKey.
     * @return Optional<EnhancedDocument> of Articles.
     */
    Optional<T> findByIdViaGsi(String articleId);

    /**
     * Find all articles with pagination support. Uses Query.
     *
     * @param limit             Maximum number of articles to return.
     * @param exclusiveStartKey DynamoDb pagination token (null for first page).
     * @return PaginatedResult result with articles and next page token.
     */
    PaginatedResult<T> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey);

    /**
     * Find articles in catalog with pagination support.
     *
     * @param id                Catalog ID.
     * @param limit             Maximum number of articles to return.
     * @param exclusiveStartKey DynamoDb pagination token (null for first page).
     * @return PaginatedResult result with articles and next page token.
     */
    PaginatedResult<T> findCatalogArticles(String id, int limit, Map<String, AttributeValue> exclusiveStartKey);
}
