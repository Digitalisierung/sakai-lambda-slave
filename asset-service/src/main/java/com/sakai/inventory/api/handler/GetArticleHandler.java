package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import dao.CreateKhachiDao;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import utility.Utility;

import java.util.Map;

public class GetArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private CreateKhachiDao transactionDao;

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String body = input.getBody();
        String str = "Hello from Lambda!";
        try {
            Map map = Utility.objectMapper.readValue(body, Map.class);
            PutItemRequest request = PutItemRequest.builder().build();
            Map<String, AttributeValue> responseItems = this.transactionDao.createNewKhachi(request);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return response;
    }
}
