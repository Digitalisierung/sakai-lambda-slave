package com.sakai.inventory.shared.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public final class ResponseUtil {

    private ResponseUtil() {
        super();
    }

    /**
     * Get default (minimal) headers for response.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> createHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /**
     * Get expanded headers for api response.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> createExpandedHeader() {
        Map<String, String> headers = ResponseUtil.createHeader();
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
        String body = String.format("{\"message\": \"%s\"}", message);
        return createApiResponse(statusCode, body, createHeader());
    }
}
