package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * DynamoDB-Datenmodell für einen Katalog.
 *
 * Single-Table Design:
 *   partitionKey = "CATALOGS"
 *   sortKey      = "CAT#<catalogId>"
 *   catalogId    = die eigentliche Business-ID (UUID)
 */
@DynamoDbBean
public class Catalog {

    private String partitionKey;
    private String sortKey;
    private String catalogId;
    private String name;
    private String description;
    private String color;
    private Integer productCount;
    private String createdAt;
    private String updatedAt;

    public Catalog() {
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("partitionKey")
    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    /**
     * catalogId wird NICHT in DynamoDB gespeichert.
     * Sie ist die UUID aus dem letzten Segment des sortKey und wird
     * von KeyHelper.extractId(sortKey) abgeleitet.
     */
    @DynamoDbIgnore
    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "partitionKey='" + partitionKey + '\'' +
                ", sortKey='" + sortKey + '\'' +
                ", catalogId='" + catalogId + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", productCount=" + productCount +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
