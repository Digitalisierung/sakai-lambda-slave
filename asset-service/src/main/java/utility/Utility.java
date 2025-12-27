package utility;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Utility {
    public static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
    }

    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        return headers;
    }

    public static APIGatewayProxyResponseEvent getApiResponse(int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent resposeEvent = new APIGatewayProxyResponseEvent();
        resposeEvent.setBody(body);
        resposeEvent.setHeaders(headers);
        resposeEvent.setStatusCode(statusCode);
        return resposeEvent;
    }
}
