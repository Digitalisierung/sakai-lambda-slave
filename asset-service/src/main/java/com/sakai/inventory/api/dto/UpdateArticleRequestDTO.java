package com.sakai.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateArticleRequestDTO(
        String name,
        String sku,
        String description,
        String imageUrl,
        String state,
        Boolean isFeatured
) {
}
