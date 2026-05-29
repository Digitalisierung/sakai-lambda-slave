package com.sakai.inventory.api.dto;

/**
 * Request-Body für PUT /catalogs/{id}
 * Alle Felder sind optional — nur gesetzte Felder werden aktualisiert (Partial Update).
 */
public record UpdateCatalogRequest(
        String name,
        String description,
        String color
) {}
