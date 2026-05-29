package com.sakai.inventory.api.infrastructure;

import com.sakai.inventory.api.model.Article;
import com.sakai.inventory.api.model.Catalog;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

/**
 * Hilfsklasse für DynamoDB-Key-Operationen.
 *
 * Hintergrund (Single-Table Design):
 *   Articles sortKey-Format:  ARTICLES#<businessId>#<sku>#<uuid>
 *   Catalogs sortKey-Format:  CATALOGS#<NAME>#METADATA#<uuid>
 *
 * Da der vollständige sortKey beim API-Aufruf (GET /articles/{id}) nicht bekannt ist,
 * wird die UUID als letztes Segment im sortKey gespeichert und per Query+Filter gesucht.
 */
public class KeyHelper {

    private KeyHelper() {
    }

    /**
     * Extrahiert die UUID (letztes Segment nach dem letzten '#') aus einem sortKey.
     * Beispiel: "ARTICLES#ART-001#SKU-7001#abc-123" → "abc-123"
     */
    public static String extractId(String sortKey) {
        if (sortKey == null || !sortKey.contains("#")) {
            return sortKey;
        }
        return sortKey.substring(sortKey.lastIndexOf('#') + 1);
    }

    /**
     * Sucht einen Artikel anhand seiner UUID.
     * Da der vollständige sortKey unbekannt ist, wird per Query (PK=ARTICLES)
     * mit einem FilterExpression auf contains(sortKey, uuid) gesucht.
     *
     * Hinweis: Liest alle Artikel der Partition und filtert clientseitig.
     * Für große Datenmengen empfiehlt sich ein GSI auf einem dedizierten articleId-Attribut.
     */
    public static Article findArticleById(DynamoDbTable<Article> table, String id) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue("ARTICLES").build()
                ))
                .filterExpression(Expression.builder()
                        .expression("contains(#sk, :id)")
                        .expressionNames(Map.of("#sk", "sortKey"))
                        .expressionValues(Map.of(":id", AttributeValue.fromS(id)))
                        .build())
                .build();

        List<Article> results = table.query(request).items().stream().toList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Sucht einen Katalog anhand seiner UUID.
     * Gleiche Logik wie findArticleById.
     */
    public static Catalog findCatalogById(DynamoDbTable<Catalog> table, String id) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue("CATALOGS").build()
                ))
                .filterExpression(Expression.builder()
                        .expression("contains(#sk, :id)")
                        .expressionNames(Map.of("#sk", "sortKey"))
                        .expressionValues(Map.of(":id", AttributeValue.fromS(id)))
                        .build())
                .build();

        List<Catalog> results = table.query(request).items().stream().toList();
        return results.isEmpty() ? null : results.get(0);
    }
}
