package com.sakai.inventory.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class DynamicFieldValue {
    private String name;
    private String type; // "TEXT", "NUMBER", "BOOLEAN", "DATE", etc.
    private String value; // Eigentlicher Inhalt.

    public DynamicFieldValue() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DynamicFieldValue::[name=" + name
                + "type=" + type
                + "]";
    }
}
