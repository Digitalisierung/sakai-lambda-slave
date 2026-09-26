package com.sakai.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateArticleRequestDTO(
        String name,
        String sku,
        String description,
        Integer stock,
        String imageUrl,
        Boolean isFeatured,
        String catalogId,
        String createdAt
) {
}
