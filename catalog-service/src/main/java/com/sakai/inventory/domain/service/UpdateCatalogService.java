package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCatalogService.class);

    private final CatalogRepository catalogRepository;

    public UpdateCatalogService(CatalogRepository catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public CatalogDTO updateCatalog(String catalogId, CatalogDTO catalogDTO) {
        LOGGER.info("Updating catalog with id '{}'", catalogId);
        Catalog catalog = CatalogMapper.MAPPER.toCatalogEntity(catalogDTO);
        Catalog updatedCatalog = this.catalogRepository.updateCatalog(catalogId, catalog);
        return CatalogMapper.MAPPER.toDTO(updatedCatalog);
    }
}
