package com.sakai.inventory.api.dto;

/**
 * Request-Body für PUT /articles/{id}
 * Alle Felder sind optional — nur gesetzte Felder werden aktualisiert (Partial Update).
 */
public record UpdateArticleRequest(
        String name,
        String sku,
        String description,
        Integer price,
        Integer stock,
        String imageUrl,
        String state,
        String catalogId,
        Boolean isFeatured
) {}
