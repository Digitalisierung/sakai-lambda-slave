package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import jakarta.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CreateCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCatalogService.class);
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final CatalogRepository<Catalog> catalogRepository;

    public CreateCatalogService(CatalogRepository<Catalog> catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public CatalogDTO saveCatalog(CatalogDTO catalogDTO) {
        Catalog catalog = CatalogMapper.MAPPER.toCatalogEntity(catalogDTO);

        Set<ConstraintViolation<Catalog>> violations = VALIDATOR.validate(catalog);

        if (!violations.isEmpty()) {
            LOGGER.warn("Catalog validation failed. Violations: {}", violations);
            throw new ConstraintViolationException("Catalog validation failed.", violations);
        }

        Catalog newCatalog = catalogRepository.save(catalog);
        return CatalogMapper.MAPPER.toDTO(newCatalog);
    }
}
