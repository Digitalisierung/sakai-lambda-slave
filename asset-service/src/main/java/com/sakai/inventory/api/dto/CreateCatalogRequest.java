package com.sakai.inventory.api.dto;

/**
 * Request-Body für POST /catalogs
 * name und color sind Pflichtfelder.
 */
public record CreateCatalogRequest(
        String name,
        String description,
        String color
) {}
