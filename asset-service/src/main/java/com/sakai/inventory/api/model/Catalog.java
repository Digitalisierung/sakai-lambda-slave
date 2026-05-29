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
 *   sortKey      = "CATALOGS#&lt;NAME&gt;#METADATA#&lt;uuid&gt;"
 *
 * Die catalogId wird NICHT als eigenes DynamoDB-Attribut gespeichert.
 * Sie wird aus dem letzten Segment des sortKey extrahiert (KeyHelper.extractId).
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

    /**
     * Standardkonstruktor — wird vom DynamoDB Enhanced Client für die Deserialisierung benötigt.
     */
    public Catalog() {
    }

    /**
     * Gibt den DynamoDB-Partitionsschlüssel zurück.
     * Für Kataloge ist dieser Wert immer "CATALOGS".
     */
    @DynamoDbPartitionKey
    @DynamoDbAttribute("partitionKey")
    public String getPartitionKey() {
        return partitionKey;
    }

    /**
     * Setzt den DynamoDB-Partitionsschlüssel.
     *
     * @param partitionKey der Partitionsschlüssel (z.B. "CATALOGS")
     */
    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    /**
     * Gibt den DynamoDB-Sortierschlüssel zurück.
     * Format: "CATALOGS#&lt;NAME&gt;#METADATA#&lt;uuid&gt;"
     */
    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() {
        return sortKey;
    }

    /**
     * Setzt den DynamoDB-Sortierschlüssel.
     *
     * @param sortKey der Sortierschlüssel (z.B. "CATALOGS#GLASS#METADATA#abc-123")
     */
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

    /**
     * Setzt die Katalog-ID (wird nicht in DynamoDB persistiert).
     * Wird intern verwendet, wenn die ID im Anwendungscode weitergegeben werden muss.
     *
     * @param catalogId die UUID des Katalogs
     */
    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    /**
     * Gibt den Anzeigenamen des Katalogs zurück.
     */
    public String getName() {
        return name;
    }

    /**
     * Setzt den Anzeigenamen des Katalogs.
     *
     * @param name der Katalogname (z.B. "Glass", "Paintings")
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gibt die Beschreibung des Katalogs zurück.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setzt die Beschreibung des Katalogs.
     *
     * @param description die Katalogbeschreibung
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gibt den Farbcode des Katalogs zurück (Hex-Format, z.B. "#fcd123").
     */
    public String getColor() {
        return color;
    }

    /**
     * Setzt den Farbcode des Katalogs.
     *
     * @param color Hex-Farbcode (z.B. "#FF5733" oder "#F53")
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Gibt die Anzahl der Artikel zurück, die diesem Katalog zugewiesen sind.
     * Wird bei Zuweisung/Entfernung von Artikeln aktualisiert.
     */
    public Integer getProductCount() {
        return productCount;
    }

    /**
     * Setzt die Anzahl der dem Katalog zugewiesenen Artikel.
     *
     * @param productCount die aktuelle Artikelanzahl (≥ 0)
     */
    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    /**
     * Gibt den Zeitstempel der Erstellung des Katalogs zurück (ISO-8601-Format).
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Setzt den Erstellungszeitstempel des Katalogs.
     *
     * @param createdAt Zeitstempel im ISO-8601-Format (z.B. "2026-02-05T14:30:01Z")
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gibt den Zeitstempel der letzten Aktualisierung des Katalogs zurück (ISO-8601-Format).
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

    /**
     * Gibt eine lesbare Darstellung des Katalogobjekts zurück — nützlich für Logging und Debugging.
     */
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
