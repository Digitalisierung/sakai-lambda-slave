package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;

public class CreateCatalogService {
    private final CatalogRepository catalogRepository;

    public CreateCatalogService(CatalogRepository catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public String saveCatalog(CatalogDTO catalogDTO) {
        Catalog catalog = CatalogMapper.MAPPER.toCatalogEntity(catalogDTO);
        return catalogRepository.save(catalog);
    }
}
