package com.sakai.inventory.api.dto;

import java.util.List;

public record PaginatedCatalogsResponseDTO(
        List<CatalogDTO> catalogs,
        String nextToken,
        int totalReturned,
        boolean hasMore
) {
}
