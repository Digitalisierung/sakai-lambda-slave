package com.sakai.inventory.api.dto;

import java.util.List;

public record BatchDeleteResponseDTO(List<String> unprocessed) {
}
