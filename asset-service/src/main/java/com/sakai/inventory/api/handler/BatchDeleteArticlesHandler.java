package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.api.dto.BatchDeleteRequestDTO;
import com.sakai.inventory.api.dto.BatchDeleteResponseDTO;
import com.sakai.inventory.domain.service.BatchDeleteArticleService;
import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.repository.ArticleRepository;
import com.sakai.inventory.infrastructure.repository.DynamoDbArticleRepository;
import com.sakai.inventory.shared.exception.ExceptionHandler;
import com.sakai.inventory.shared.util.JsonUtil;
import com.sakai.inventory.shared.util.ResponseUtil;
import software.amazon.awssdk.http.HttpStatusCode;

import java.util.List;

public class BatchDeleteArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BatchDeleteArticleService articleService;

    public BatchDeleteArticlesHandler() {
        super();
        this.articleService = new BatchDeleteArticleService(createArticleRepository());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            BatchDeleteRequestDTO request = JsonUtil.parseFromJsonToObject(requestEvent.getBody(), BatchDeleteRequestDTO.class);
            List<String> unprocessed = articleService.batchDeleteArticles(request.articleIds());

            int statusCode = unprocessed.size() == request.articleIds().size()
                    ? HttpStatusCode.BAD_REQUEST
                    : HttpStatusCode.OK;

            String body = JsonUtil.convertToJson(new BatchDeleteResponseDTO(unprocessed));
            return ResponseUtil.createApiResponse(statusCode, body, ResponseUtil.createExpandedHeader());
        } catch (Exception e) {
            return ExceptionHandler.handleException(e);
        }
    }

    private ArticleRepository createArticleRepository() {
        return new DynamoDbArticleRepository(
                DynamoDbFactory.createEnhancedClient(),
                DynamoDbFactory.getTableName(),
                DynamoDbFactory.createTableSchema()
        );
    }
}
