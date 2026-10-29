package com.sakai.inventory.domain.service;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import com.sakai.inventory.infrastructure.mapper.CatalogMapper;
import com.sakai.inventory.infrastructure.repository.CatalogRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class UpdateCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCatalogService.class);
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final CatalogRepository<Catalog> catalogRepository;

    public UpdateCatalogService(CatalogRepository<Catalog> catalogRepository) {
        super();
        this.catalogRepository = catalogRepository;
    }

    public CatalogDTO updateCatalog(String catalogId, CatalogDTO catalogDTO) {
        LOGGER.info("Updating catalog with id '{}'", catalogId);
        Catalog catalog = CatalogMapper.MAPPER.toCatalogEntity(catalogDTO);

        Set<ConstraintViolation<Catalog>> violations = VALIDATOR.validate(catalog);

        if (!violations.isEmpty()) {
            LOGGER.warn("Catalog validation failed. Violations: {}", violations);
            throw new jakarta.validation.ConstraintViolationException("Catalog validation failed.", violations);
        }

        Catalog updatedCatalog = this.catalogRepository.update(catalogId, catalog);
        return CatalogMapper.MAPPER.toDTO(updatedCatalog);
    }
}
