package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GetCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetCatalogService.class);

    private final CatalogRepository catalogRepository;

    public GetCatalogService(CatalogRepository catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public Optional<CatalogDTO> findCatalogById(String id) {
        Optional<Catalog> catalogOptional = catalogRepository.findCatalogById(id);

        return catalogOptional.map(CatalogMapper.MAPPER::toDTO);
    }
}
