package builder;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import mapper.DynDbMapper;

import java.util.Map;

public class RequestBuilder {
    private static final String TABLE_NAME;

    static {
        TABLE_NAME = System.getenv("TABLE_NAME");
    }

    public PutItemResponse buildItemResponse(Map jsonMap){
        Map<String, AttributeValue> dynDbMap = DynDbMapper.MAPPER.convertToDynDbMap(jsonMap);

        final PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(dynDbMap)
                .build();

        final DynamoDbClient ddbClient = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .build();

        try (ddbClient) {
            return ddbClient.putItem(putItemRequest);
        } catch (Exception e) {
            return null;
        }
    }
}
