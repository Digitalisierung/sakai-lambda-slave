package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * DynamoDB-Datenmodell für einen dynamischen Feldwert eines Artikels.
 * Dynamische Felder ermöglichen flexible, artikelspezifische Zusatzattribute
 * (z.B. Provenienz, Ausstellungshistorie, Material).
 */
@DynamoDbBean
public class DynamicFieldValue {
    private String name;
    private String type; // "TEXT", "NUMBER", "BOOLEAN", "DATE", etc.
    private String value; // Eigentlicher Inhalt.

    /**
     * Standardkonstruktor — wird vom DynamoDB Enhanced Client für die Deserialisierung benötigt.
     */
    public DynamicFieldValue() {
    }

    /**
     * Gibt den Namen des dynamischen Feldes zurück (z.B. "provenance", "exhibitions").
     */
    public String getName() {
        return name;
    }

    /**
     * Setzt den Namen des dynamischen Feldes.
     *
     * @param name der Feldname (z.B. "provenance")
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gibt den Datentyp des Feldes zurück.
     * Mögliche Werte: "TEXT", "NUMBER", "BOOLEAN", "DATE".
     */
    public String getType() {
        return type;
    }

    /**
     * Setzt den Datentyp des dynamischen Feldes.
     *
     * @param type der Typ des Wertes (z.B. "TEXT", "NUMBER")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gibt den eigentlichen Inhalt des dynamischen Feldes als String zurück.
     * Der Wert wird unabhängig vom Typ immer als String gespeichert.
     */
    public String getValue() {
        return value;
    }

    /**
     * Setzt den Inhalt des dynamischen Feldes.
     *
     * @param value der Feldinhalt als String
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gibt eine lesbare Darstellung des dynamischen Feldwertes zurück — nützlich für Logging.
     */
    @Override
    public String toString() {
        return "DynamicFieldValue::[name=" + name
                + "type=" + type
                + "]";
    }
}
