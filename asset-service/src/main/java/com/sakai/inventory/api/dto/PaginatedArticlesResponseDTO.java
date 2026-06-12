package com.sakai.inventory.api.dto;

import java.util.List;

public record PaginatedArticlesResponseDTO(
        List<ArticleDTO> articleS,
        String nextToken,
        int totalReturned,
        boolean hasMore
) {
}
