package handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.mapstruct.ap.shaded.freemarker.core.Environment;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import dao.CreateKhachiDao;
import mapper.DynDbMapper;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import utility.Utility;

import java.util.List;
import java.util.Map;

import static mapper.DynDbMapper.MAPPER;

public class CreateNewKhachiHandler implements RequestHandler<APIGatewayProxyRequestEvent, String> {
    private CreateKhachiDao transactionDao;
    public String handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String body = input.getBody();
        try {
            Map map = Utility.objectMapper.readValue(body, Map.class);
            PutItemRequest request = PutItemRequest.builder().build();
            Map<String, AttributeValue> responseItems = this.transactionDao.createNewKhachi(request);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return "OK";
    }
}
