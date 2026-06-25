package com.sakai.inventory.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

import java.util.Optional;

/**
 * repository interface for Article domain objects.
 * Provides domain-oriented operations for article management.
 */
public interface ArticleRepository {
    /**
     * Find article by its unique identifier.
     *
     * @param id
     * @return
     */
    Optional<EnhancedDocument> findById(String id);
}
