package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.domain.service.GetArticleService;
import com.sakai.inventory.shared.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleHandler.class);

    private final GetArticleService getArticleService;

    public GetArticleHandler() {
        super();
        LOGGER.info("Initializing GetArticleService");
        getArticleService = new GetArticleService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LOGGER.info("Processing GET /articles/{id} request.");

        Map<String, String> pathParameters = requestEvent.getPathParameters();
        if (pathParameters == null || !pathParameters.containsKey("id")) {
            LOGGER.error("Missing 'id' path parameters.");
            return Utility.createErrorResponse(400, "Missing article ID");
        }

        try {
            String articleId = pathParameters.get("id");
            String article = getArticleService.findArticleById(articleId);
            LOGGER.info("Fetching article by id: {}", articleId);
            LOGGER.info("{}", article);

            return Utility.createApiResponse(200, article, Utility.createHeaders());
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
            return Utility.createErrorResponse(404, "Article not found");
        }
    }
}
