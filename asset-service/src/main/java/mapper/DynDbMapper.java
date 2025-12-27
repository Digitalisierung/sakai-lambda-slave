package mapper;

import org.mapstruct.factory.Mappers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

// @Mapper(componentModel = "cdi", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DynDbMapper {
    DynDbMapper MAPPER = Mappers.getMapper(DynDbMapper.class);

    Map<String, AttributeValue> convertToDynDbMap(Map<String, Object> object);

    default AttributeValue map(Object value) {
        if (value instanceof String s){
            return AttributeValue.builder().s(s.trim()).build();
        } else if(value instanceof Number n) {
            return AttributeValue.builder().n(n.toString()).build();
        } else if (value instanceof Boolean b) {
            return AttributeValue.builder().bool(b).build();
        } else if (value instanceof Map m) {
            // vielleicht besser? Map<String, AttributeValue> amp =  concertToDynDbMap(m);
            return AttributeValue.builder().m((Map<String, AttributeValue>) m).build();
        } else {
            return AttributeValue.builder().nul(true).build();
        }
    }
}
