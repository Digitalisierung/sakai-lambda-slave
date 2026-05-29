package com.sakai.inventory.api.dto;

/**
 * Request-Body für POST /articles
 */
public record CreateArticleRequest(
        String name,
        String sku,
        String description,
        Integer price,
        Integer stock,
        String imageUrl,
        String catalogId,
        Boolean isFeatured
) {}
