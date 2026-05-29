package dao;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Map;

/**
 * Data Access Object (DAO) für Schreiboperationen auf DynamoDB.
 * Kapselt den DynamoDB-Client und stellt Methoden zum Anlegen neuer Einträge bereit.
 */
public class CreateKhachiDao {
    private final DynamoDbClient client;

    /**
     * Erstellt eine neue Instanz des CreateKhachiDao und initialisiert den DynamoDB-Client
     * für die Region EU_CENTRAL_1.
     */
    public CreateKhachiDao() {
        this.client = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(null)
                .build();
        //this.client = DynamoDbClient.create();
    }

    /**
     * Führt eine PutItem-Operation auf DynamoDB aus und gibt die Attribute
     * des vorherigen Items zurück (falls vorhanden).
     *
     * @param request das vollständig befüllte PutItemRequest-Objekt
     * @return Map mit den Attributen des zuvor gespeicherten Items (kann leer sein)
     */
    public Map<String, AttributeValue> createNewKhachi(PutItemRequest request) {
        PutItemResponse putItemResponse = client.putItem(request);

         return putItemResponse.attributes();
    }
}
