package com.sakai.inventory.api.dto;

import java.util.List;
import java.util.Map;

public record BatchCreateResponseDTO(
        List<Map<String, Object>> created,
        List<FailedEntryDTO> failed
) {
    public record FailedEntryDTO(int index, String sku, String error) {
    }
}
