package utility;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Hilfsklasse mit gemeinsam genutzten Methoden für alle Lambda-Handler.
 * Stellt einen zentralen Jackson-ObjectMapper sowie Hilfsmethoden
 * für HTTP-Response-Erstellung und Standard-Header bereit.
 */
public class Utility {
    public static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
    }

    /**
     * Erstellt eine Map mit den Standard-HTTP-Headern für alle API-Antworten.
     * Enthält "Content-Type: application/json" sowie den Custom-Header.
     *
     * @return Map mit den Standard-Headern
     */
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        return headers;
    }

    /**
     * Erstellt ein vollständiges APIGatewayProxyResponseEvent-Objekt
     * mit dem angegebenen HTTP-Statuscode, Body und Headern.
     *
     * @param statusCode der HTTP-Statuscode (z.B. 200, 201, 404, 500)
     * @param body       der Response-Body als JSON-String
     * @param headers    die HTTP-Antwort-Header
     * @return das fertig befüllte Response-Objekt für API Gateway
     */
    public static APIGatewayProxyResponseEvent getApiResponse(int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent resposeEvent = new APIGatewayProxyResponseEvent();
        resposeEvent.setBody(body);
        resposeEvent.setHeaders(headers);
        resposeEvent.setStatusCode(statusCode);
        return resposeEvent;
    }
}
