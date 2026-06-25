package com.sakai.inventory.shared.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public final class Utility {
    public static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
    }

    private Utility() {
        super();
    }

    /**
     * Get default (minimal) headers for response.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        return headers;
    }

    /**
     * Get expanded headers for api response.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> createExpandedHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        return headers;
    }

    /**
     * Create api response with custom headers.
     *
     * @param statusCode
     * @param body
     * @param headers
     * @return APIGatewayProxyResponseEvent
     */
    public static APIGatewayProxyResponseEvent createApiResponse(int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setBody(body);
        responseEvent.setHeaders(headers);
        responseEvent.setStatusCode(statusCode);
        return responseEvent;
    }

    /**
     * Create error api response.
     *
     * @param statusCode
     * @param message
     * @return APIGatewayProxyResponseEvent
     */
    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        String body = String.format("{\"error\": \"%s\"}", message);
        return createApiResponse(statusCode, body, createHeaders());
    }
}
