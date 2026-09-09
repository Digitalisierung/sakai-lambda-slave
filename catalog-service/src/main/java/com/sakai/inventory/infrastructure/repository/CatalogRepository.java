package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for catalog domain objects.
 * Provides domain-oriented operations for catalog management.
 */
public interface CatalogRepository {

    /**
     * Find catalog by its unique identifier.
     *
     * @param id equal .
     * @return Optional<Catalog>
     */
    Optional<Catalog> findCatalogById(String id);

    /**
     * Find all catalogs with pagination support.
     *
     * @param limit             Maximum number of catalogs to return.
     * @param exclusiveStartKey DynamoDb pagination token (null for first page).
     * @return PaginatedResult result with catalogs and next page token.
     */
    PaginatedResult<Catalog> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey);

    /**
     *
     * @param catalog
     * @return Catalog
     */
    Catalog updateCatalog(Catalog catalog);
}
