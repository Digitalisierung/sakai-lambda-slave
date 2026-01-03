package builder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;

public class GetItemBuilder {
    private static final String TABLE_NAME;

    static {
        TABLE_NAME = System.getenv("jre-dev-khachi-table");
    }

    public GetItemRequest createGetItemRequest(String companyId, String assetId) {
        Map<String, AttributeValue> keyMap = new HashMap<>();

        return GetItemRequest.builder()
                .key(keyMap)
                .tableName(TABLE_NAME)
                .build();
    }
}
