package com.sakai.inventory.shared.exception;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.shared.util.ResponseUtil;
import software.amazon.awssdk.http.HttpStatusCode;

public class ExceptionHandler {
    private ExceptionHandler() {
        super();
    }

    public static APIGatewayProxyResponseEvent handleException(Exception e) {

        return switch (e) {
            case NotFoundException notFoundException ->
                    ResponseUtil.createErrorResponse(HttpStatusCode.NOT_FOUND, notFoundException.getMessage());
            case software.amazon.awssdk.thirdparty.jackson.core.JsonParseException jsonParseException ->
                    ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, jsonParseException.getMessage());
            case com.fasterxml.jackson.core.JsonParseException jsonParseExcept ->
                    ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, jsonParseExcept.getMessage());
            case software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException jsonProcessException ->
                    ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, jsonProcessException.getMessage());
            case com.fasterxml.jackson.core.JsonProcessingException jsonProcessingException ->
                    ResponseUtil.createErrorResponse(HttpStatusCode.BAD_REQUEST, jsonProcessingException.getMessage());
            default -> ResponseUtil.createErrorResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage());
        };
    }
}
