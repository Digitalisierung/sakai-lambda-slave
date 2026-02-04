package com.sakai.inventory.api.model;

public class DynamicFieldValue {
    private String name;
    private String type; // "TEXT", "NUMBER", "BOOLEAN", "DATE", etc.
    private Object value; // Eigentlicher Inhalt.

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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DynamicFieldValue::[name=" + name
                + "type=" + type
                + "]";
    }
}
