package com.sakai.inventory.infrastructure.mapper;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface CatalogMapper {
    CatalogMapper MAPPER = Mappers.getMapper(CatalogMapper.class);

    @Mapping(target = "id", expression = "java(extractId(entity.getPartitionKey()))")
    @Mapping(target = "productCount", constant = "0")
    CatalogDTO toDTO(Catalog entity);

    List<CatalogDTO> toDomainList(List<Catalog> entities);

    default String extractId(String partitionKey) {
        if (partitionKey == null || partitionKey.isBlank()) {
            return partitionKey;
        }

        return partitionKey.substring(partitionKey.lastIndexOf("#") + 1);
    }
}
