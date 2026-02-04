package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.Map;

@DynamoDbBean
public class Article {
    private String articleId;
    private String sku;
    private String name;
    private String description;
    // TODO: muss später entfernt werden. Keine Preise, da kein Handel. Den Wert bestimmt Gutachten.
    private String price;
    private Long inventory;
    private String imageUrl;
    private String state;
    private Boolean isFeatured;
    private String catalogId; // Fremdschlüssel-Referenz.

    private String createdAt;
    private String updatedAt;

    // Die "Magie" für dynamische Felder.
    private Map<String, DynamicFieldValue> dynamicFields;

    public Article(String name, String sku) {
        this.name = name;
        this.sku = sku;
    }

    @DynamoDbPartitionKey
    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_sku_lookup"})
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Long getInventory() {
        return inventory;
    }

    public void setInventory(Long inventory) {
        this.inventory = inventory;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getFeatured() {
        return isFeatured;
    }

    public void setFeatured(Boolean featured) {
        isFeatured = featured;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_catalog_lookup"})
    public String getCatalogId() {
        return catalogId;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, DynamicFieldValue> getDynamicFields() {
        return dynamicFields;
    }

    public void setDynamicFields(Map<String, DynamicFieldValue> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    @Override
    public String toString() {
        return "Article::[articleId=" + articleId
                + ", sku=" + sku
                + ", name='" + name
                + "', price=" + price
                + ", inventory=" + inventory
                + ", state=" + state
                + ", isFeatured=" + isFeatured
                + ", catalogId=" + catalogId
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + ", dynamicFields (Size) =" + dynamicFields.size()
                + "]";
    }
}
