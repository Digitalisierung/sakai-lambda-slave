package builder;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Map;

/**
 * Builder-Klasse für DynamoDB PutItem-Anfragen.
 * Kapselt die Erstellung und Ausführung von Schreiboperationen auf DynamoDB.
 */
public class RequestBuilder {
    private static final String TABLE_NAME;

    static {
        TABLE_NAME = System.getenv("TABLE_NAME");
    }

    /**
     * Erstellt und sendet eine PutItem-Anfrage an DynamoDB, um ein neues Item zu speichern.
     * Die übergebene Map wird in DynamoDB-Attribute konvertiert und in die Tabelle geschrieben.
     *
     * @param jsonMap die zu speichernden Daten als Map
     * @return die DynamoDB-Antwort des PutItem-Aufrufs
     * @throws UnsupportedOperationException solange das Mapping nicht implementiert ist
     */
    public PutItemResponse buildItemResponse(Map jsonMap){
        // TODO: DynDbMapper muss wieder eingebunden werden, damit .item(...) befüllt werden kann.
        // Solange item() fehlt, lehnt AWS die Anfrage mit ValidationException ab.
        throw new UnsupportedOperationException("buildItemResponse ist nicht funktionsfähig: item()-Mapping fehlt.");
    }
}
