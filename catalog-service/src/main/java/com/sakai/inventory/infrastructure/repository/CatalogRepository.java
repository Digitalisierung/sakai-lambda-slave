package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for catalog domain objects.
 * Provides domain-oriented operations for catalog management.
 */
public interface CatalogRepository<T> {

    /**
     * Find catalog by its unique identifier.
     *
     * @param id equal .
     * @return Optional<T>
     */
    Optional<T> findById(String id);

    /**
     * Find all catalogs with pagination support.
     *
     * @param limit             Maximum number of catalogs to return.
     * @param exclusiveStartKey DynamoDb pagination token (null for first page).
     * @return PaginatedResult result with catalogs and next page token.
     */
    PaginatedResult<T> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey);

    /**
     * Update existing catalog.
     *
     * @param id
     * @param t
     * @return Catalog
     */
    T update(String id, T t);

    /**
     * Save new catalog.
     *
     * @param t
     * @return
     */
    String save(T t);
}
