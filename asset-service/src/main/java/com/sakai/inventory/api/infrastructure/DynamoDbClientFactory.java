package com.sakai.inventory.api.infrastructure;

import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

/**
 * Zentrale Factory für DynamoDB-Clients.
 * Wird von allen Handlern verwendet, um Code-Duplikation zu vermeiden.
 * Der Client wird einmalig pro Lambda-Instanz erstellt (statisch) und wiederverwendet.
 */
public class DynamoDbClientFactory {

    private static final DynamoDbClient DB_CLIENT = DynamoDbClient.builder()
            .httpClientBuilder(AwsCrtHttpClient.builder())
            .region(Region.of(System.getenv().getOrDefault("REGION", "eu-central-1")))
            .build();

    private static final DynamoDbEnhancedClient ENHANCED_CLIENT = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(DB_CLIENT)
            .build();

    private DynamoDbClientFactory() {
    }

    public static DynamoDbClient getClient() {
        return DB_CLIENT;
    }

    public static DynamoDbEnhancedClient getEnhancedClient() {
        return ENHANCED_CLIENT;
    }
}
