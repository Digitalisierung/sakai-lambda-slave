package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Automatically generated tests for GetCatalogsHandler.")
class ListArticlesHandlerTest {

    private ListArticlesHandler handler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new ListArticlesHandler();
    }

    @Test
    @DisplayName("Test GetCatalogsHandler. It should return success response.")
    void handleRequest_ShouldReturnSuccessResponse() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotBlank();

        Map<String, String> headers = response.getHeaders();
        assertThat(headers).isNotNull();
        assertThat(headers).containsEntry("Content-Type", "application/json");
        assertThat(headers).containsEntry("X-Custom-Header", "application/json");
    }
}
