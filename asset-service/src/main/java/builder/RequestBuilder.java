package builder;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Map;

public class RequestBuilder {
    private static final String TABLE_NAME;

    static {
        TABLE_NAME = System.getenv("TABLE_NAME");
    }

    public PutItemResponse buildItemResponse(Map jsonMap){
        // TODO: DynDbMapper muss wieder eingebunden werden, damit .item(...) befüllt werden kann.
        // Solange item() fehlt, lehnt AWS die Anfrage mit ValidationException ab.
        throw new UnsupportedOperationException("buildItemResponse ist nicht funktionsfähig: item()-Mapping fehlt.");
    }
}
