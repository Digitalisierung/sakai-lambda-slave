package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sakai.inventory.domain.service.BulkDeleteArticlesService;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.List;

public class BulkDeleteArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BulkDeleteArticlesService deleteArticlesService;

    public BulkDeleteArticlesHandler() {
        super();
        this.deleteArticlesService = null;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String body = requestEvent.getBody();
        try {
            List<String> deleteIds = JsonUtil.parseFromJsonToObject(body, new TypeReference<>() {
            });
            List<String> unprocessedIds = deleteArticlesService.bulkDeleteArticles(deleteIds);
            return ResponseUtil.createApiResponse(HttpStatusCode.NO_CONTENT, "", ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            return ExceptionHandler.handleException(e);
        }
    }
}
