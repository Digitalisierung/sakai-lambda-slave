package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sakai.inventory.shared.util.EnvironmentLoader;
import com.sakai.inventory.shared.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class GetArticleHandlerBackup implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticleHandlerBackup.class);

    public GetArticleHandlerBackup() {
        super();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiRequest, Context context) {
        Map<String, String> pathParameters = apiRequest.getPathParameters();
        String id = pathParameters.get("id");
        LOGGER.info("id {}", id);

        Map<String, String> headers = Utility.createHeaders();

        try (final DynamoDbClient client = createDynamoDbClient()) {
            DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(client)
                    .build();

            TableSchema<EnhancedDocument> tableSchema = createTableSchema();

            DynamoDbTable<EnhancedDocument> table = ddbClient.table(EnvironmentLoader.getEnv("TABLE_NAME"), tableSchema);

            Key key = buildArticleKey(id);

            EnhancedDocument item = table.getItem(key);

            if (item == null) {
                LOGGER.error("Item not found or null.");
                return Utility.createApiResponse(404, "{\"error\": \"Item not found\"}", headers);
            }

            String jsonBody = item.toJson();
            return Utility.createApiResponse(200, jsonBody, headers);
        } catch (Exception e) {
            LOGGER.error("Internal server error");
            return Utility.createApiResponse(500, "{\"error\": \"Internal server error\"}", headers);
        }

    }

    private DynamoDbClient createDynamoDbClient() {
        return DynamoDbClient.builder()
                .httpClient(AwsCrtHttpClient.create())
                .build();
    }

    private TableSchema<EnhancedDocument> createTableSchema() {
        return TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "partitionKey", AttributeValueType.S)
                .addIndexSortKey(TableMetadata.primaryIndexName(), "sortKey", AttributeValueType.S)
                .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                .build();
    }

    private Key buildArticleKey(String id) {
        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue("ARTICLES#SKU-030#" + id)
                .build();
    }
}
