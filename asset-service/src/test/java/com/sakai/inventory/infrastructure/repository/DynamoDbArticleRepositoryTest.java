package com.sakai.inventory.infrastructure.repository;

import com.sakai.inventory.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbArticleRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<EnhancedDocument> articleTable;

    private TableSchema<EnhancedDocument> tableSchema;

    @Mock
    private BatchWriteResult batchWriteResult;

    @Mock
    private DynamoDbIndex<EnhancedDocument> articleIndex;

    @Mock
    private SdkIterable<Page<EnhancedDocument>> pages;

    @Mock
    private Page<EnhancedDocument> page;

    private DynamoDbArticleRepository repository;

    @BeforeEach
    void setUp() {
        tableSchema = TableSchema.documentSchemaBuilder()
                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "partitionKey", AttributeValueType.S)
                .addIndexSortKey(TableMetadata.primaryIndexName(), "sortKey", AttributeValueType.S)
                .build();
        when(enhancedClient.table("articles", tableSchema)).thenReturn(articleTable);
        lenient().when(articleTable.tableSchema()).thenReturn(tableSchema);
        repository = new DynamoDbArticleRepository(enhancedClient, "articles", tableSchema);
    }

    @Test
    void findByIdReturnsArticleWithIdAndWithoutStorageFields() {
        EnhancedDocument storedDocument = EnhancedDocument.fromJson("""
                {"partitionKey":"ACC#default__CAT#catalog-1","sortKey":"ITEM#article-1","entityType":"ARTICLES","version":2,"title":"Chair"}
                """);
        when(articleTable.getItem(any(Key.class))).thenReturn(storedDocument);

        Optional<EnhancedDocument> result = repository.findById("article-1");

        assertThat(result).isPresent();
        assertThat(result.get().getString("id")).isEqualTo("article-1");
        assertThat(result.get().getString("title")).isEqualTo("Chair");
        assertThat(result.get().toMap()).doesNotContainKeys("partitionKey", "sortKey", "entityType", "version");
    }

    @Test
    void findByIdReturnsEmptyWhenArticleDoesNotExist() {
        when(articleTable.getItem(any(Key.class))).thenReturn(null);

        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void batchSaveAddsStorageKeysAndReturnsUnprocessedItems() {
        EnhancedDocument article = EnhancedDocument.fromJson("""
                {"catalogId":"catalog-1","title":"Chair"}
                """);
        when(enhancedClient.batchWriteItem(any(BatchWriteItemEnhancedRequest.class))).thenReturn(batchWriteResult);
        when(batchWriteResult.unprocessedPutItemsForTable(articleTable)).thenReturn(List.of());
        when(batchWriteResult.consumedCapacity()).thenReturn(List.of());

        List<EnhancedDocument> result = repository.batchSave(List.of(article));

        assertThat(result).isEmpty();
        ArgumentCaptor<BatchWriteItemEnhancedRequest> requestCaptor = ArgumentCaptor.forClass(BatchWriteItemEnhancedRequest.class);
        verify(enhancedClient).batchWriteItem(requestCaptor.capture());
        assertThat(requestCaptor.getValue().writeBatches()).hasSize(1);
    }

    @Test
    void updateThrowsNotFoundWhenArticleDoesNotExist() {
        when(articleTable.getItem(any(Key.class))).thenReturn(null);
        Map<String, Object> changes = new java.util.HashMap<>(Map.of("title", "Desk"));

        assertThatThrownBy(() -> repository.update("catalog-1", "article-1", changes))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("article-1");
        assertThat(changes).containsEntry("partitionKey", "ACC#default__CAT#catalog-1")
                .containsEntry("sortKey", "ITEM#article-1");
    }

    @Test
    void deleteDoesNothingWhenArticleIsNotFound() {
        when(articleTable.index("GSI_entityType")).thenReturn(articleIndex);
        when(articleIndex.query(any(QueryEnhancedRequest.class))).thenReturn(pages);
        when(pages.stream()).thenReturn(Stream.empty());

        repository.delete("missing");

        verify(articleTable, never()).deleteItem(any(Key.class));
    }

    @Test
    void found_an_Id_and_delivers_exactly_one_item() {
        String id = UUID.randomUUID().toString();
        EnhancedDocument document = EnhancedDocument.fromAttributeValueMap(Map.of(
                "partitionKey", AttributeValue.builder().s("ACC#default__CAT#default").build(),
                "sortKey", AttributeValue.builder().s("ITEM#" + id).build(),
                "entityType", AttributeValue.builder().s("ARTICLES").build(),
                "version", AttributeValue.builder().n("0").build(),
                "name", AttributeValue.builder().s("Mac Book Pro 14").build(),
                "sku", AttributeValue.builder().s("MBP-14-001").build(),
                "createdAt", AttributeValue.builder().s("2026-11-06T19:17:00.777Z").build(),
                "updatedAt", AttributeValue.builder().s("2026-11-06T19:17:00.777Z").build()
        ));

        when(articleTable.index("GSI_entityType")).thenReturn(articleIndex);
        when(articleIndex.query(any(QueryEnhancedRequest.class))).thenReturn(pages);
        when(pages.stream()).thenReturn(Stream.of(page));
        when(page.items()).thenReturn(List.of(document));


        List<String> ids = List.of(id);
        List<Map<String, Object>> erg = repository.bulkFindViaGsi(ids);

        assertThat(erg).hasSize(1);
        assertThat(erg.getFirst())
                .containsEntry("id", id)
                .containsEntry("name", "Mac Book Pro 14")
                .containsEntry("sku", "MBP-14-001")
                .containsEntry("updatedAt", "2026-11-06T19:17:00.777Z")
                .containsEntry("createdAt", "2026-11-06T19:17:00.777Z");
    }
}