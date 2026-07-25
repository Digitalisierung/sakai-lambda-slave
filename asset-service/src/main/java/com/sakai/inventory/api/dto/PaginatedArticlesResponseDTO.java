package com.sakai.inventory.api.dto;

public record PaginatedArticlesResponseDTO(
        String articles,
        String nextToken,
        int totalReturned,
        boolean hasMore
) {
}
