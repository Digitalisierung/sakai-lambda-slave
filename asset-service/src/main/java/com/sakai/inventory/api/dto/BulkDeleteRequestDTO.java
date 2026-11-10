package com.sakai.inventory.api.dto;

import java.util.List;

public record BulkDeleteRequestDTO(List<String> articleIds) {
}
