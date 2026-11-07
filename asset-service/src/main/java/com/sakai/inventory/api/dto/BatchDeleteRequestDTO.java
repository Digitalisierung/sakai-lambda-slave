package com.sakai.inventory.api.dto;

import java.util.List;

public record BatchDeleteRequestDTO(List<String> articleIds) {
}
