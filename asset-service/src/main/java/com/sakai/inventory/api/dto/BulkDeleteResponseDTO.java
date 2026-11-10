package com.sakai.inventory.api.dto;

import java.util.List;

public record BulkDeleteResponseDTO(List<String> unprocessed) {
}
