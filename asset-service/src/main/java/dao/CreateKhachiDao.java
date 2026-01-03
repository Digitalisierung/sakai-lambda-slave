package dao;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Map;

public class CreateKhachiDao {
    private final DynamoDbClient client;

    public CreateKhachiDao() {
        this.client = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(null)
                .build();
        //this.client = DynamoDbClient.create();
    }

    public Map<String, AttributeValue> createNewKhachi(PutItemRequest request) {
        PutItemResponse putItemResponse = client.putItem(request);

         return putItemResponse.attributes();
    }
}
