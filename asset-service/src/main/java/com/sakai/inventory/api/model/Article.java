package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class Article {

    //  = "ARTICLES"
    private String partitionKey;
    // = "ARTiCLES#SKU-120#40eb8f95-852b-40f6-8fa4-dc744132db4a"
    private String sortKey;
    private String catalogId;
    //  = "2026-02-05T14:30:00Z"
    private String createdAt;
    private String description;
    // = "http://s3.img01.png"
    private String imageUrl;
    private Integer inventory;
    private Boolean isFeatured;
    //  = "iPhone 15"
    private String name;
    private Integer price;
    // = "SKU-120"
    private String sku;
    //  = "AVAILABLE"
    private String state;
    //  = "2026-02-05T14:30:00Z"
    private String updatedAt;


    public Article() {
        super();
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

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getInventory() {
        return inventory;
    }

    public void setInventory(Integer inventory) {
        this.inventory = inventory;
    }

    public Boolean getFeatured() {
        return isFeatured;
    }

    public void setFeatured(Boolean featured) {
        isFeatured = featured;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    //    @DynamoDbPartitionKey
//    public String getArticleId() {
//        return articleId;
//    }

    // @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_sku_lookup"})
    public String getSku() {
        return sku;
    }

    // @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_catalog_lookup"})
    public String getCatalogId() {
        return catalogId;
    }

    @Override
    public String toString() {
        return "Article{" +
                "partitionKey='" + partitionKey + '\'' +
                ", sortKey='" + sortKey + '\'' +
                ", catalogId='" + catalogId + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", inventory=" + inventory +
                ", isFeatured=" + isFeatured +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", sku='" + sku + '\'' +
                ", state='" + state + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
