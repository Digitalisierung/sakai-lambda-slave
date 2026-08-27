package com.sakai.inventory.api.dto;

public record CatalogDTO(
        String id,
        String name,
        String description,
        String color,
        Integer productCount,
        String createdAt,
        String updatedAt
) {
}
