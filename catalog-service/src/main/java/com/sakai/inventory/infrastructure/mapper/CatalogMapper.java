package com.sakai.inventory.infrastructure.mapper;

import com.sakai.inventory.api.dto.CatalogDTO;
import com.sakai.inventory.domain.model.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;

@Mapper(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface CatalogMapper {
    CatalogMapper MAPPER = Mappers.getMapper(CatalogMapper.class);

    @Mapping(target = "id", source = "partitionKey", qualifiedByName = "extractIdFromPartitionKey")
    @Mapping(target = "articleCount", constant = "0")
    @Mapping(target = "updatedAt", expression = "java(formatInstant(entity.getUpdatedAt()))")
    CatalogDTO toDTO(Catalog entity);

    @Mapping(target = "partitionKey", source = "id")
    @Mapping(target = "sortKey", ignore = true)
    @Mapping(target = "rendered", ignore = true)
    @Mapping(target = "entityType", constant = "INDEX")
    @Mapping(target = "updatedAt", expression = "java(parseInstant(dto.updatedAt()))")
    Catalog toCatalogEntity(CatalogDTO dto);

    List<CatalogDTO> toDomainList(List<Catalog> entities);

    @Named("extractIdFromPartitionKey")
    default String extractId(String partitionKey) {
        if (partitionKey == null || partitionKey.isBlank()) {
            return partitionKey;
        }

        return partitionKey.substring(partitionKey.lastIndexOf("#") + 1);
    }

    default String formatInstant(Instant updatedAt) {
        return updatedAt == null
                ? null
                : updatedAt.toString();
    }

    default Instant parseInstant(String updatedAt) {
        return (updatedAt == null || updatedAt.isBlank())
                ? null
                : Instant.parse(updatedAt);
    }
}
