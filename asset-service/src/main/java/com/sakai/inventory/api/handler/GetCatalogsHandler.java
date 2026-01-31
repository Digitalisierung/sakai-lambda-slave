package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class GetCatalogsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(GetCatalogsHandler.class);

    public GetCatalogsHandler() {
        LOGGER.info("GetCatalogsHandler constructor");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent s, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        String body = String.format("{\"message\": \"Lambda works successfully\"}");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        response.setHeaders(headers);
        response.setBody(body);
        response.setStatusCode(200);

        LOGGER.info("GetCatalogsHandler request successful");
        LOGGER.info("GetCatalogsHandler response: {}", response.getBody());

        return response;
    }
}
