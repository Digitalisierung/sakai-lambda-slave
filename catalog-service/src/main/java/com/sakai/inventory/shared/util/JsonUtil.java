package com.sakai.inventory.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
        super();
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Convert object to JSON string.
     *
     * @param object
     * @return String
     * @throws JsonProcessingException
     */
    public static String convertToJson(Object object) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * Parse JSON string to object.
     *
     * @param json
     * @param valueType
     * @return <T>
     * @throws JsonProcessingException
     */
    public static <T> T parseFromJsonToObject(String json, Class<T> valueType) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, valueType);
    }

    /**
     * Parse JSON string to object with TypeReference (for generics).
     *
     * @param json
     * @param <T>  typeReference
     * @return <T>
     * @throws JsonProcessingException
     */
    public static <T> T parseFromJsonToObject(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, typeReference);
    }
}
