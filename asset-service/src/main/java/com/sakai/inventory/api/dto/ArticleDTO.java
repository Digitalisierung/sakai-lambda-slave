package com.sakai.inventory.api.dto;

import java.util.Map;

public record ArticleDTO(String articleId, String name, String sku, String description, String price, Long inventory,
                         String imageUrl, String catalogId, Boolean isActive, Boolean isFeatured,
                         Map<String, DynamicFieldsDTO> dynamicFields, String createdAt, String updatedAt) {
}
