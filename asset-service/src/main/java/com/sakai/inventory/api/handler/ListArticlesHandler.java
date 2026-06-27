package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sakai.inventory.domain.service.ListArticlesService;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.Map;

public class ListArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListArticlesHandler.class);

    private ListArticlesService articlesService;

    public ListArticlesHandler() {
        super();
        articlesService = new ListArticlesService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiRequest, Context context) {
        Map<String, String> queryStringParameters = apiRequest.getQueryStringParameters();

        LOGGER.info("isNull? {}", queryStringParameters == null);
        LOGGER.info("Query String Parameters size{}", queryStringParameters != null ? queryStringParameters.size() : 0);
        LOGGER.info("Query Parameter limit: {}", queryStringParameters != null ? queryStringParameters.get("limit") : "NoN");
        LOGGER.info("Query Parameter nextToken: {}", queryStringParameters != null ? queryStringParameters.get("nextToken") : "NoN");

        try {
            String json = JsonUtil.convertToJson(apiRequest);
            LOGGER.info("APIGatewayProxyRequestEvent");
            LOGGER.info("{}", json);
            return ResponseUtil.createApiResponse(HttpStatusCode.OK, json, ResponseUtil.createHeaders());
        } catch (JsonProcessingException e) {
            return ResponseUtil.createErrorResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, "Parsing exception");
        }
    }
}
