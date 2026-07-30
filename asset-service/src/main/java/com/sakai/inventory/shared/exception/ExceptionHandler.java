package com.sakai.inventory.shared.exception;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.http.HttpStatusCode;

public class ExceptionHandler {
    private ExceptionHandler() {
        super();
    }

    public static APIGatewayProxyResponseEvent handleException(Exception e) {
        APIGatewayProxyResponseEvent responseEvent;

        if (e instanceof NotFoundException notFoundException) {
            responseEvent = handleException(notFoundException);
        } else if (e instanceof software.amazon.awssdk.thirdparty.jackson.core.JsonParseException jsonParseException) {
            responseEvent = handleException(jsonParseException);
        } else if (e instanceof com.fasterxml.jackson.core.JsonParseException jsonParseException) {
            responseEvent = handleException(jsonParseException);
        } else if (e instanceof software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException jsonProcessingException) {
            responseEvent = handleException(jsonProcessingException);
        } else if (e instanceof com.fasterxml.jackson.core.JsonProcessingException jsonProcessingException) {
            responseEvent = handleException(jsonProcessingException);
        } else {
            responseEvent = new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        return responseEvent;
    }

    public static APIGatewayProxyResponseEvent handleException(NotFoundException e) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.NOT_FOUND);
    }

    public static APIGatewayProxyResponseEvent handleException(software.amazon.awssdk.thirdparty.jackson.core.JsonParseException e) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.BAD_REQUEST);
    }

    public static APIGatewayProxyResponseEvent handleException(com.fasterxml.jackson.core.JsonParseException e) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.BAD_REQUEST);
    }

    public static APIGatewayProxyResponseEvent handleException(software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException e) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.BAD_REQUEST);
    }

    public static APIGatewayProxyResponseEvent handleException(com.fasterxml.jackson.core.JsonProcessingException e) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatusCode.BAD_REQUEST);
    }
}
