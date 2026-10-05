package com.sakai.inventory.api.dto;

import java.util.List;

public record BatchCreateResponseDTO(
        String created,
        List<FailedEntryDTO> failed
) {
    public record FailedEntryDTO(int index, String name, String sku, String error) {
    }
}
