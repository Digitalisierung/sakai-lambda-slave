package com.sakai.inventory.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sakai.inventory.api.dto.ArticleDTO;
import com.sakai.inventory.api.model.Article;
import com.sakai.inventory.api.model.DynamicFieldValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utility.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ListArticlesHandler class implements the AWS Lambda RequestHandler interface to process a
 * request and provide a response for listing articles. It fetches, maps, and returns article data in JSON format.
 */
public class ListArticlesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ListArticlesHandler.class);

    public ListArticlesHandler() {
        LOGGER.info("GetCatalogsHandler constructor");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent s, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        // String body = String.format("{\"message\": \"Lambda works successfully\"}");
        List<Article> articles = fetchArticles();
        List<ArticleDTO> articleDTOs = mapArticles(articles);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        response.setHeaders(headers);
        try {
            response.setBody(Utility.objectMapper.writeValueAsString(articleDTOs));
            response.setStatusCode(200);
            LOGGER.info("GetCatalogsHandler request successful");
        } catch (JsonProcessingException e) {
            response.setStatusCode(500);
            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"message\": \"" + e.getMessage() + "\"}");
            LOGGER.error(e.getMessage(), e);
        }


        return response;
    }

    private List<ArticleDTO> mapArticles(List<Article> articles) {
        List<ArticleDTO> articleDTOS = new ArrayList<>();

        for (Article article : articles) {
            articleDTOS.add(new ArticleDTO(
                    article.getArticleId(),
                    article.getName(),
                    article.getSku(),
                    article.getDescription(),
                    article.getPrice(),
                    article.getInventory(),
                    article.getImageUrl(),
                    article.getCatalogId(),
                    true,
                    true,
                    new HashMap<>(),
                    article.getCreatedAt(),
                    article.getUpdatedAt()));
        }

        return articleDTOS;
    }

    private List<Article> fetchArticles() {

        DynamicFieldValue dynamicFieldValue1 = new DynamicFieldValue();
        dynamicFieldValue1.setName("provenance");
        dynamicFieldValue1.setType("text");
        dynamicFieldValue1.setValue("Sammlung Dr. Schmidt, Auktion Haus Müller 2019");

        DynamicFieldValue dynamicFieldValue2 = new DynamicFieldValue();
        dynamicFieldValue2.setName("exhibitions");
        dynamicFieldValue2.setValue("Biennale Venedig 2023");
        dynamicFieldValue2.setType("text");

        Map<String, DynamicFieldValue> dynamicField1 = new HashMap<>();
        dynamicField1.put("artist", dynamicFieldValue1);
        Map<String, DynamicFieldValue> dynamicField2 = new HashMap<>();
        dynamicField2.put("exhibitions", dynamicFieldValue2);

        return List.of(
                createArticle(
                        "ART-001",
                        "SKU-7001",
                        "Antike Vase aus der Ming-Dynastie",
                        "Eine gut erhaltene Porzellanvase mit blau-weißen Mustern, ca. 16. Jahrhundert.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/ming_vase.jpg",
                        "AVAILABLE",
                        true,
                        "CAT-ANTIQUES",
                        "2024-01-15T10:30:00Z",
                        "2024-03-10T14:22:00Z",
                        new HashMap<>()
                ),
                // Artikel 2 (OHNE dynamische Felder)
                createArticle(
                        "ART-002",
                        "SKU-7002",
                        "Barock-Gemälde 'Landschaft mit Fluss'",
                        "Öl auf Leinwand, 18. Jahrhundert, Rahmen original.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/barock_painting.jpg",
                        "UNDER_EVALUATION",
                        false,
                        "CAT-PAINTINGS",
                        "2024-02-01T09:15:00Z",
                        "2024-02-28T16:45:00Z",
                        new HashMap<>()
                ),
                // Artikel 3 (MIT dynamischen Feldern - Provenienz und Material)
                createArticle(
                        "ART-003",
                        "SKU-7003",
                        "Renaissance-Bronzeskulptur 'Reiter'",
                        "Kleine Bronzestatue, italienische Arbeit, 17. Jahrhundert.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/bronze_rider.jpg",
                        "AVAILABLE",
                        true,
                        "CAT-SCULPTURES",
                        "2023-11-20T11:00:00Z",
                        "2024-01-05T12:30:00Z",
                        dynamicField1
                ),
                // Artikel 4 (OHNE dynamische Felder)
                createArticle(
                        "ART-004",
                        "SKU-7004",
                        "Art Deco Sekretär aus Nussbaum",
                        "Möbelstück mit Intarsien, Frankreich um 1925.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/artdeco_desk.jpg",
                        "SOLD",
                        false,
                        "CAT-FURNITURE",
                        "2023-10-05T14:20:00Z",
                        "2024-03-15T10:10:00Z",
                        new HashMap<>()
                ),
                // Artikel 5 (OHNE dynamische Felder)
                createArticle(
                        "ART-005",
                        "SKU-7005",
                        "Samurai-Rüstung (Gusoku)",
                        "Komplette Rüstung aus Eisen, Seide und Leder, Edo-Periode.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/samurai_armor.jpg",
                        "AVAILABLE",
                        true,
                        "CAT-ARMOR",
                        "2024-03-01T08:45:00Z",
                        "2024-03-01T08:45:00Z",
                        new HashMap<>()
                ),
                // Artikel 6 (OHNE dynamische Felder)
                createArticle(
                        "ART-006",
                        "SKU-7006",
                        "Persischer Teppich 'Isfahan'",
                        "Handgeknüpfter Seidenteppich, 2.3 x 1.6 m, Anfang 20. Jh.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/isfahan_carpet.jpg",
                        "UNDER_EVALUATION",
                        false,
                        "CAT-TEXTILES",
                        "2024-02-10T13:10:00Z",
                        "2024-02-25T09:30:00Z",
                        new HashMap<>()
                ),
                // Artikel 7 (MIT dynamischen Feldern - Künstler und Technik)
                createArticle(
                        "ART-007",
                        "SKU-7007",
                        "Moderne Skulptur 'Equilibrium'",
                        "Abstrakte Stahlskulptur des zeitgenössischen Künstlers M. Chen.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/equilibrium_sculpture.jpg",
                        "AVAILABLE",
                        true,
                        "CAT-MODERN",
                        "2024-01-30T16:00:00Z",
                        "2024-03-12T11:20:00Z",
                        dynamicField2
                ),
                // Artikel 8 (OHNE dynamische Felder)
                createArticle(
                        "ART-008",
                        "SKU-7008",
                        "Klassische Violine 'Stradivarius-Kopie'",
                        "Geige deutscher Arbeit um 1900, ausgezeichneter Klang.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/violin_strad_copy.jpg",
                        "AVAILABLE",
                        false,
                        "CAT-MUSIC",
                        "2023-12-15T10:45:00Z",
                        "2024-02-20T15:15:00Z",
                        new HashMap<>()
                ),
                // Artikel 9 (OHNE dynamische Felder)
                createArticle(
                        "ART-009",
                        "SKU-7009",
                        "Ägyptische Uschebti-Figur",
                        "Fayence-Statuette, Spätzeit, für Grabbeigabe bestimmt.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/ushabti_figure.jpg",
                        "RESERVED",
                        false,
                        "CAT-ANTIQUITIES",
                        "2024-02-28T12:30:00Z",
                        "2024-03-14T17:00:00Z",
                        new HashMap<>()
                ),
                // Artikel 10 (OHNE dynamische Felder)
                createArticle(
                        "ART-010",
                        "SKU-7010",
                        "Jugendstil-Vase von Émile Gallé",
                        "Glasvase mit eingeschliffenen Pflanzendekoren, Frankreich um 1900.",
                        "Nicht bewertet",
                        1L,
                        "https://inventory-images.s3.eu-central-1.amazonaws.com/galle_vase.jpg",
                        "AVAILABLE",
                        true,
                        "CAT-GLASS",
                        "2024-01-10T09:30:00Z",
                        "2024-03-05T14:05:00Z",
                        new HashMap<>()
                )
        );
    }

    private Article createArticle(String articleId, String sku, String name, String description, String price,
                                  Long inventory, String imageUrl, String state, Boolean isFeatured, String catalogId,
                                  String createdAt, String updatedAt, Map<String, DynamicFieldValue> dynamicFields) {

        Article article = new Article(name, sku);
        article.setArticleId(articleId);
        article.setDescription(description);
        article.setPrice(price);
        article.setInventory(inventory);
        article.setImageUrl(imageUrl);
        article.setState(state);
        article.setFeatured(isFeatured);
        article.setCatalogId(catalogId);
        article.setCreatedAt(createdAt);
        article.setUpdatedAt(updatedAt);
        article.setDynamicFields(dynamicFields);

        return article;
    }
}
