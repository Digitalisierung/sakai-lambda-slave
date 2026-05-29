package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * DynamoDB-Datenmodell für einen Artikel.
 *
 * Single-Table Design:
 *   partitionKey = "ARTICLES"
 *   sortKey      = "ARTICLES#<uuid>"
 *
 * Die UUID im sortKey ist gleichzeitig die API-seitige ID des Artikels
 * und wird von KeyHelper.extractId(sortKey) abgeleitet.
 */
@DynamoDbBean
public class Article {

    private String partitionKey;
    private String sortKey;
    private String sku;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private String imageUrl;
    private String state;
    private Boolean isFeatured;
    private String catalogId;
    private String createdAt;
    private String updatedAt;

    /**
     * Standardkonstruktor — wird vom DynamoDB Enhanced Client für die Deserialisierung benötigt.
     */
    public Article() {
        super();
    }

    /**
     * Gibt den DynamoDB-Partitionsschlüssel zurück.
     * Für Artikel ist dieser Wert immer "ARTICLES".
     */
    @DynamoDbPartitionKey
    @DynamoDbAttribute("partitionKey")
    public String getPartitionKey() {
        return partitionKey;
    }

    /**
     * Setzt den DynamoDB-Partitionsschlüssel.
     *
     * @param partitionKey der Partitionsschlüssel (z.B. "ARTICLES")
     */
    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    /**
     * Gibt den DynamoDB-Sortierschlüssel zurück.
     * Format: "ARTICLES#&lt;uuid&gt;"
     */
    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() {
        return sortKey;
    }

    /**
     * Setzt den DynamoDB-Sortierschlüssel.
     *
     * @param sortKey der Sortierschlüssel (z.B. "ARTICLES#abc-123")
     */
    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    /**
     * Gibt die ID des Katalogs zurück, dem dieser Artikel zugewiesen ist.
     * Leer oder null, wenn der Artikel keinem Katalog zugeordnet ist.
     */
    // @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_catalog_lookup"})
    public String getCatalogId() {
        return catalogId;
    }

    /**
     * Setzt die Katalog-ID, der dieser Artikel zugewiesen ist.
     *
     * @param catalogId die UUID des Katalogs
     */
    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    /**
     * Gibt den Zeitstempel der Erstellung des Artikels zurück (ISO-8601-Format).
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Setzt den Erstellungszeitstempel des Artikels.
     *
     * @param createdAt Zeitstempel im ISO-8601-Format (z.B. "2024-01-15T10:30:00Z")
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gibt die Beschreibung des Artikels zurück.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setzt die Beschreibung des Artikels.
     *
     * @param description die Artikelbeschreibung
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gibt die URL zum Bild des Artikels zurück.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Setzt die Bild-URL des Artikels.
     *
     * @param imageUrl vollständige URL zum Artikelbild (z.B. S3-URL)
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gibt den aktuellen Lagerbestand des Artikels zurück.
     */
    public Integer getStock() {
        return stock;
    }

    /**
     * Setzt den Lagerbestand des Artikels.
     *
     * @param stock die Anzahl verfügbarer Einheiten (≥ 0)
     */
    public void setStock(Integer stock) {
        this.stock = stock;
    }

    /**
     * Gibt zurück, ob der Artikel als hervorgehoben markiert ist.
     */
    public Boolean getFeatured() {
        return isFeatured;
    }

    /**
     * Setzt den Hervorhebungsstatus des Artikels.
     *
     * @param featured true, wenn der Artikel als "Featured" angezeigt werden soll
     */
    public void setFeatured(Boolean featured) {
        isFeatured = featured;
    }

    /**
     * Gibt den Namen des Artikels zurück.
     */
    public String getName() {
        return name;
    }

    /**
     * Setzt den Namen des Artikels.
     *
     * @param name der Anzeigename des Artikels
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gibt den Preis des Artikels in ganzzahligen Einheiten zurück (z.B. Cent oder kleinste Währungseinheit).
     */
    public Integer getPrice() {
        return price;
    }

    /**
     * Setzt den Preis des Artikels.
     *
     * @param price der Preis in ganzzahligen Einheiten
     */
    public void setPrice(Integer price) {
        this.price = price;
    }

    /**
     * Gibt die SKU (Stock Keeping Unit) des Artikels zurück — eindeutige Lagerkennung.
     */
    // @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_sku_lookup"})
    public String getSku() {
        return sku;
    }

    /**
     * Setzt die SKU (Stock Keeping Unit) des Artikels.
     *
     * @param sku die eindeutige Lagerkennung (z.B. "SKU-7001")
     */
    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * Gibt den aktuellen Status des Artikels zurück.
     * Mögliche Werte: AVAILABLE, SOLD, UNDER_EVALUATION, RESERVED.
     */
    public String getState() {
        return state;
    }

    /**
     * Setzt den Status des Artikels.
     *
     * @param state der Statuswert (z.B. "AVAILABLE", "SOLD")
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gibt den Zeitstempel der letzten Aktualisierung des Artikels zurück (ISO-8601-Format).
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Setzt den Zeitstempel der letzten Aktualisierung.
     *
     * @param updatedAt Zeitstempel im ISO-8601-Format
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    //    @DynamoDbPartitionKey
//    public String getArticleId() {
//        return articleId;
//    }

    /**
     * Gibt eine lesbare Darstellung des Artikelobjekts zurück — nützlich für Logging und Debugging.
     */
    @Override
    public String toString() {
        return "Article{" +
                "partitionKey='" + partitionKey + '\'' +
                ", sortKey='" + sortKey + '\'' +
                ", catalogId='" + catalogId + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", stock=" + stock +
                ", isFeatured=" + isFeatured +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", sku='" + sku + '\'' +
                ", state='" + state + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
