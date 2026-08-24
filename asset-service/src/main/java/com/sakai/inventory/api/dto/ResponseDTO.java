package com.sakai.inventory.api.dto;

public record ResponseDTO(
        int statusCode,
        String message
) {
}
