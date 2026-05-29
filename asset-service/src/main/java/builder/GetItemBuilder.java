package builder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder-Klasse für DynamoDB GetItem-Anfragen.
 * Kapselt die Erstellung von GetItemRequest-Objekten für den Lesezugriff auf DynamoDB.
 */
public class GetItemBuilder {
    private static final String TABLE_NAME;

    static {
        TABLE_NAME = System.getenv("jre-dev-khachi-table");
    }

    /**
     * Erstellt eine GetItemRequest-Anfrage zum Abrufen eines einzelnen DynamoDB-Items
     * anhand der zusammengesetzten Schlüssel aus companyId und assetId.
     *
     * @param companyId die ID des Unternehmens (Partitionsschlüssel)
     * @param assetId   die ID des Assets (Sortierschlüssel)
     * @return das fertig befüllte GetItemRequest-Objekt
     */
    public GetItemRequest createGetItemRequest(String companyId, String assetId) {
        Map<String, AttributeValue> keyMap = new HashMap<>();

        return GetItemRequest.builder()
                .key(keyMap)
                .tableName(TABLE_NAME)
                .build();
    }
}
