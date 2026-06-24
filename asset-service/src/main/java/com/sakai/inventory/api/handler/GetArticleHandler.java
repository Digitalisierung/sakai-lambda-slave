package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import utility.Utility;

import java.util.Map;

public class GetArticleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleHandler.class);

    public GetArticleHandler() {
        super();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiRequest, Context context) {
        //APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        Map<String, String> pathParameters = apiRequest.getPathParameters();
        String id = pathParameters.get("id");
        LOGGER.info("id {}", id);

        //String requestBody = apiRequest.getBody();
        //LOGGER.info("Request-Body: {}", requestBody);

        // TODO: body validieren, ob JSON: wenn ja -> document

        //EnhancedDocument document = EnhancedDocument.fromJson(requestBody);

        //String id = document.getString("id");

        DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
                .build();

        TableSchema<EnhancedDocument> tableSchema = TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "partitionKey", AttributeValueType.B)
                .addIndexSortKey(TableMetadata.primaryIndexName(), "sortKey", AttributeValueType.B)
                .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                .build();

        DynamoDbTable<EnhancedDocument> table = ddbClient.table(System.getenv("TABLE_NAME"), tableSchema);

        Key key = Key.builder()
                .partitionValue("ARTICLES")
                .sortValue("ARTICLES#SKU-030#" + id)
                .build();

        EnhancedDocument item = table.getItem(key);

        Map<String, String> headers = Utility.getHeaders();

        if (item == null) {
            LOGGER.error("Item not found or null.");
            return Utility.getApiResponse(404, "{\"error\": \"Item not found\"}", headers);
        }

        String jsonBody = item.toJson();


        return Utility.getApiResponse(200, jsonBody, headers);
    }
}
