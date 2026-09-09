package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;

public class UpdateCatalogService {
    private final CatalogRepository catalogRepository;

    public UpdateCatalogService(CatalogRepository catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public CatalogDTO updateCatalog(CatalogDTO catalogDTO) {
        Catalog catalog = CatalogMapper.MAPPER.toCatalogEntity(catalogDTO);
        Catalog updatedCatalog = this.catalogRepository.updateCatalog(catalog);
        return CatalogMapper.MAPPER.toDTO(updatedCatalog);
    }
}
