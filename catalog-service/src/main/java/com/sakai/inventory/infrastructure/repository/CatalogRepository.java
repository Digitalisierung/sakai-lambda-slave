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
     * Find a catalog by its unique identifier.
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
     * Update an existing item.
     *
     * @param id unique item identifier.
     * @param t
     * @return T updated item.
     */
    T update(String id, T t);

    /**
     * Save / create new item.
     *
     * @param t item to save.
     * @return String id of saved item.
     */
    T save(T t);
}
