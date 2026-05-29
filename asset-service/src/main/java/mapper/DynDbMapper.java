package mapper;

//import org.mapstruct.factory.Mappers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

// @Mapper(componentModel = "cdi", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
/**
 * Mapper-Interface zur Konvertierung von Java-Objekten in DynamoDB-Attribute.
 * Wandelt eine generische Map in eine Map aus DynamoDB-AttributeValue-Objekten um,
 * die direkt für PutItem- und UpdateItem-Anfragen verwendet werden kann.
 */
public interface DynDbMapper {
    //DynDbMapper MAPPER = Mappers.getMapper(DynDbMapper.class);

    // Map<String, AttributeValue> concertToDynDbMap(Concert concert);

    /**
     * Konvertiert eine generische Java-Map (String → Object) in eine DynamoDB-AttributeValue-Map.
     * Die Schlüssel bleiben erhalten; die Werte werden anhand ihres Java-Typs in den
     * passenden DynamoDB-Typ umgewandelt (siehe {@link #map(Object)}).
     *
     * @param object die Eingabe-Map mit beliebigen Java-Werten
     * @return eine Map aus DynamoDB-AttributeValue-Objekten
     */
    Map<String, AttributeValue> convertToDynDbMap(Map<String, Object> object);

    /**
     * Konvertiert einen einzelnen Java-Wert in ein DynamoDB-AttributeValue-Objekt.
     * Die Typzuordnung erfolgt nach folgender Logik:
     * <ul>
     *   <li>String  → AttributeValue mit Typ S (String), trimmed</li>
     *   <li>Number  → AttributeValue mit Typ N (Number)</li>
     *   <li>Boolean → AttributeValue mit Typ BOOL</li>
     *   <li>Map     → AttributeValue mit Typ M (Map)</li>
     *   <li>Sonstige Typen → AttributeValue mit Typ NULL</li>
     * </ul>
     *
     * @param value der zu konvertierende Java-Wert
     * @return das entsprechende DynamoDB-AttributeValue-Objekt
     */
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
