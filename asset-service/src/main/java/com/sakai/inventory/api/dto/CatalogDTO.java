package com.sakai.inventory.api.dto;

public record CatalogDTO(String catalogId, String name, String description, String color, Integer productCount,
                         String createdAt, String updatedAt) {
}
