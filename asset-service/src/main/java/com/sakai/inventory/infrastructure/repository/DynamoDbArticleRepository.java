package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.infrastructure.factory.DynamoDbFactory;
import com.sakai.inventory.infrastructure.factory.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.*;

import static software.amazon.awssdk.services.dynamodb.model.ReturnValue.ALL_NEW;

public class DynamoDbArticleRepository implements ArticleRepository<EnhancedDocument> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbArticleRepository.class);

    private final DynamoDbClient dynamoDbClient = DynamoDbFactory.createDynamoDbClient();
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<EnhancedDocument> articleTable;

    public DynamoDbArticleRepository(final DynamoDbEnhancedClient enhancedClient, final String tableName, TableSchema<EnhancedDocument> tableSchema) {
        this.enhancedClient = enhancedClient;
        this.articleTable = enhancedClient.table(tableName, tableSchema);
        LOGGER.debug("DynamoDbArticleRepository initialized. Table name {}", tableName);
    }

    /**
     * Saves a list of EnhancedDocuments in a batch.
     *
     * @param documents list of EnhancedDocuments to save (max 25).
     * @return List<EnhancedDocument> List of unprocessed EnhancedDocuments (empty if all succeeded).
     */
    @Override
    public List<EnhancedDocument> batchSave(List<EnhancedDocument> documents) {
        LOGGER.info("Batch saving {} documents.", documents.size());

        List<EnhancedDocument> supplementedDocuments = supplementDocumentsWithId(documents);

        WriteBatch.Builder<EnhancedDocument> writeBatchBuilder = WriteBatch.builder(EnhancedDocument.class)
                .mappedTableResource(articleTable);

        for (EnhancedDocument doc : supplementedDocuments) {
            writeBatchBuilder.addPutItem(doc);
        }

        BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatchBuilder.build())
                .build();

        var result = enhancedClient.batchWriteItem(batchRequest);

        List<EnhancedDocument> unprocessed = result.unprocessedPutItemsForTable(articleTable);
        LOGGER.info("Unprocessed items: {}", unprocessed.size());

        // Gesamtkosten anzeigen, wenn mindestens einer geschrieben war.
        if (unprocessed.size() != documents.size()) {
            double wcu = 0.;
            for (var cc : result.consumedCapacity()) {
                wcu += cc.capacityUnits();
            }
            LOGGER.info("Write Capacity Units total: {}", wcu);
        }

        return unprocessed;
    }

    @Override
    public Optional<EnhancedDocument> findById(String id) {
        LOGGER.debug("Finding article by id {}", id);

        Key key = buildArticleKey(id);

        EnhancedDocument rohDocument = articleTable.getItem(key);
        EnhancedDocument document = replaceIdInDocument(rohDocument);

        return Optional.ofNullable(document);
    }

    @Override
    public Optional<EnhancedDocument> findByIdViaGsi(String id) {
        LOGGER.debug("Querying single article by id='{}' using GSI_entityType.", id);
        QueryConditional queryConditional = QueryConditional.keyEqualTo(buildArticleKey(id));

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        SdkIterable<Page<EnhancedDocument>> sdkIterable = articleTable.index("GSI_entityType")
                .query(queryRequest);

        return sdkIterable.stream()
                .flatMap(page -> page.items().stream())
                .map(this::replaceIdInDocument)
                .findFirst();
    }

    @Override
    public PaginatedResult<EnhancedDocument> findAll(int limit, Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying (all) articles using GSI_entityType. Limit {}, start key {}", limit, exclusiveStartKey != null);

        Key key = buildArticleKey();

        QueryConditional query = QueryConditional.keyEqualTo(key);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(query)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .build();

        SdkIterable<Page<EnhancedDocument>> sdkIterable = articleTable.index("GSI_entityType")
                .query(queryRequest);

        Optional<Page<EnhancedDocument>> pageOptional = sdkIterable.stream()
                .findFirst();

        final List<EnhancedDocument> articles = new ArrayList<>();
        final Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        pageOptional.ifPresent(page -> {
            articles.addAll(page.items());
            if (page.lastEvaluatedKey() != null) {
                lastEvaluatedKey.putAll(page.lastEvaluatedKey());
            }
        });

        List<EnhancedDocument> replacedList = articles.stream()
                .map(this::replaceIdInDocument)
                .toList();

        LOGGER.debug("Found {} articles.", replacedList.size());

        return new PaginatedResult<>(replacedList, lastEvaluatedKey);
    }

    @Override
    public PaginatedResult<EnhancedDocument> findCatalogArticles(final String id, final int limit, final Map<String, AttributeValue> exclusiveStartKey) {
        LOGGER.debug("Paginated querying articles for catalog id '{}'. Limit {}, start key {}", id, limit, exclusiveStartKey != null);

        Key key = Key.builder()
                .partitionValue("ACC#default__CAT#" + id)
                .sortValue("ITEM#")
                .build();

        QueryConditional query = QueryConditional.sortBeginsWith(key);

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(query)
                .exclusiveStartKey(exclusiveStartKey)
                .limit(limit)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build();

        Iterator<Page<EnhancedDocument>> pageIterator = articleTable.query(queryRequest).iterator();

        if (!pageIterator.hasNext()) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        Page<EnhancedDocument> documentPage = pageIterator.next();

        List<EnhancedDocument> articles = documentPage.items().stream()
                .map(this::replaceIdInDocument)
                .toList();

        return new PaginatedResult<>(articles, documentPage.lastEvaluatedKey());
    }

    @Override
    public Map<String, Object> updateAll(String catalogId, String articleId, Map<String, Object> document) {
        String partitionKey = "ACC#default__CAT#" + catalogId;
        String sortKey = "ITEM#" + articleId;

        document.put("partitionKey", partitionKey);
        document.put("sortKey", sortKey);

        EnhancedDocument enhancedDocument = convertObjectMapToDocument(document);

        Expression conditionExpression = Expression.builder()
                .expression("attribute_exists(partitionKey) AND attribute_exists(sortKey)")
                .build();

        UpdateItemEnhancedRequest<EnhancedDocument> request = UpdateItemEnhancedRequest.builder(EnhancedDocument.class)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .conditionExpression(conditionExpression)
                .item(enhancedDocument)
                .build();

        UpdateItemEnhancedResponse<EnhancedDocument> response = articleTable.updateItemWithResponse(request);
        EnhancedDocument responseDocument = response.attributes();
        return convertDocumentToObjectMap(responseDocument);
    }

    @Override
    public EnhancedDocument update(String catalogId, String articleId, Map<String, AttributeValue> fields) {
        String partitionKey = "ACC#default__CAT#" + catalogId;
        String sortKey = "ITEM#" + articleId;

        LOGGER.debug("Updating article. partitionKey='{}', sortKey='{}'", partitionKey, sortKey);

        // Build SET expression dynamically from provided fields
        StringBuilder setExpression = new StringBuilder("SET ");
        Map<String, String> expressionNames = new HashMap<>();
        Map<String, AttributeValue> expressionValues = new HashMap<>();

        int i = 0;
        for (Map.Entry<String, AttributeValue> entry : fields.entrySet()) {
            String nameToken = "#f" + i;
            String valueToken = ":v" + i;
            if (i > 0) setExpression.append(", ");
            setExpression.append(nameToken).append(" = ").append(valueToken);
            expressionNames.put(nameToken, entry.getKey());
            expressionValues.put(valueToken, entry.getValue());
            i++;
        }

        // conditionExpression: item must already exist
        Map<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put("partitionKey", AttributeValue.builder().s(partitionKey).build());
        keyMap.put("sortKey", AttributeValue.builder().s(sortKey).build());

        LOGGER.debug("Update article request. Table name: {}", DynamoDbFactory.getTableName());
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(articleTable.tableName())
                .key(keyMap)
                .updateExpression(setExpression.toString())
                .expressionAttributeNames(expressionNames)
                .expressionAttributeValues(expressionValues)
                .conditionExpression("attribute_exists(partitionKey) AND attribute_exists(sortKey)")
                .returnValues(ALL_NEW)
                .build();


        var response = dynamoDbClient.updateItem(updateRequest);
        return replaceIdInDocument(response.attributes());
    }

    private Map<String, Object> convertDocumentToObjectMap(EnhancedDocument document) {
        Map<String, Object> converted = new HashMap<>();
        Map<String, AttributeValue> map = document.toMap();
        for (Map.Entry<String, AttributeValue> entry : map.entrySet()) {
            String key = entry.getKey();
            AttributeValue value = entry.getValue();
            AttributeValue.Type type = value.type();
            var object = switch (type) {
                case B -> value.b();
                case BOOL -> value.bool();
                case S -> value.s();
                case N -> value.n();
                case L -> value.l();
                case M -> value.m();
                default -> null;
            };
            converted.put(key, object);
        }
        return converted;
    }

    private EnhancedDocument convertObjectMapToDocument(Map<String, Object> map) {
        Map<String, AttributeValue> valueMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (value) {
                case String sVal -> valueMap.put(key, AttributeValue.builder()
                        .s(sVal)
                        .build());
                case Integer iVal -> valueMap.put(key, AttributeValue.builder()
                        .n(String.valueOf(iVal))
                        .build());
                case Double dVal -> valueMap.put(key, AttributeValue.builder()
                        .n(String.valueOf(dVal))
                        .build());
                case Boolean bVal -> valueMap.put(key, AttributeValue.builder()
                        .bool(bVal)
                        .build());
                default -> {
                }
            }
        }

        return EnhancedDocument.fromAttributeValueMap(valueMap);
    }

    private Key buildArticleKey(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Article id must not be null or empty.");
        }

        String sortKey = "ITEM#" + id;

        return Key.builder()
                .partitionValue("ARTICLES")
                .sortValue(sortKey)
                .build();
    }

    private Key buildArticleKey() {
        return Key.builder()
                .partitionValue("ARTICLES")
                .build();
    }

    private EnhancedDocument replaceIdInDocument(EnhancedDocument document) {
        String partitionKey = document.getString("partitionKey");
        String pk = extractIdFromPartitionKey(partitionKey);

        String sortKey = document.getString("sortKey");
        String sk = extractIdFromSortKey(sortKey);

        Map<String, AttributeValue> map = document.toMap();
        map.replace("partitionKey", AttributeValue.builder().s(pk).build());
        map.replace("sortKey", AttributeValue.builder().s(sk).build());

        return EnhancedDocument.fromAttributeValueMap(map);
    }

    private EnhancedDocument replaceIdInDocument(Map<String, AttributeValue> documentMap) {
        if (documentMap == null || documentMap.isEmpty()) {
            return EnhancedDocument.fromAttributeValueMap(Map.of());
        }

        String partitionKey = documentMap.get("partitionKey").s();
        String pk = extractIdFromPartitionKey(partitionKey);

        String sortKey = documentMap.get("sortKey").s();
        String sk = extractIdFromSortKey(sortKey);

        documentMap.replace("partitionKey", AttributeValue.builder().s(pk).build());
        documentMap.replace("sortKey", AttributeValue.builder().s(sk).build());

        return EnhancedDocument.fromAttributeValueMap(documentMap);
    }

    private String extractIdFromPartitionKey(String partitionKey) {
        if (partitionKey == null || partitionKey.isBlank()) {
            return null;
        }

        String[] split = partitionKey.split("__");
        if (split.length < 2) {
            return null;
        }

        return extractIdFromSortKey(split[1]);
    }

    private String extractIdFromSortKey(String sortKey) {
        if (sortKey == null || sortKey.isBlank()) {
            return null;
        }

        String[] split = sortKey.split("#");
        if (split.length < 2) {
            return null;
        }

        return split[1];
    }

    /**
     * Adds partitionKey, sortKey, and entityType to each document in the list.
     *
     * @param documents list of documents without partition- and sortKey
     * @return List<EnhancedDocument> list of documents with partition- and sortKey plus entityType
     */
    private List<EnhancedDocument> supplementDocumentsWithId(List<EnhancedDocument> documents) {
        List<EnhancedDocument> healthyList = new ArrayList<>();

        for (EnhancedDocument doc : documents) {
            String uuid = UUID.randomUUID().toString();
            String catalogId = extractCatalogId(doc.getString("catalogId"));

            String partitionKey = "ACC#default__CAT#" + catalogId;
            String sortKey = "ITEM#" + uuid;

            Map<String, AttributeValue> map = doc.toMap();
            map.put("partitionKey", AttributeValue.builder().s(partitionKey).build());
            map.put("sortKey", AttributeValue.builder().s(sortKey).build());
            map.put("entityType", AttributeValue.builder().s("ARTICLES").build());

            EnhancedDocument healthyDoc = EnhancedDocument.fromAttributeValueMap(map);
            healthyList.add(healthyDoc);
        }

        return healthyList;
    }

    private String extractCatalogId(String catalogId) {
        return (catalogId != null && !catalogId.isBlank()) ? catalogId : "default";
    }
}
