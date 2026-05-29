# Architektur — sakai-lambda-slave

## Projektbeschreibung

`sakai-lambda-slave` ist das Backend-Service für `sakai-ng-master`. Es ist als serverlose Anwendung auf AWS Lambda implementiert und stellt eine REST-API für die Verwaltung von Inventarartikeln und Katalogen bereit.

---

## Technologiestack

| Bereich | Technologie | Version |
|---|---|---|
| Programmiersprache | Java | 21 (Amazon Corretto) |
| Build-Tool | Maven | 3.9.x |
| Laufzeitumgebung | AWS Lambda | Java 21 Runtime |
| API-Gateway | AWS API Gateway | REST API (Regional) |
| Datenbank | AWS DynamoDB | Single-Table Design |
| HTTP-Client | AWS CRT HTTP Client | aws-crt-client |
| DynamoDB-Client | AWS SDK Enhanced Client | 2.41.x |
| JSON-Verarbeitung | Jackson Databind | 2.17.x |
| Validierung | Hibernate Validator | 9.1.x |
| Logging | Log4j2 + Lambda Appender | 1.6.x |
| CI/CD | AWS CodeBuild | buildspec.yaml |
| IaC (Lambda/API) | AWS SAM | template.yaml |
| IaC (DynamoDB) | AWS CDK | extern verwaltet |

---

## Systemarchitektur

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                              │
│                   (sakai-ng-master / Browser)               │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    AWS API Gateway                          │
│              (REST API – Regional, Prod Stage)              │
│                                                             │
│  GET  /articles              GET  /catalogs                 │
│  GET  /articles/{id}         GET  /catalogs/{id}            │
│  POST /articles              POST /catalogs                 │
│  PUT  /articles/{id}         PUT  /catalogs/{id}            │
│  DELETE /articles/{id}       DELETE /catalogs/{id}          │
│                                                             │
│  POST   /catalogs/{id}/articles                             │
│  DELETE /catalogs/{id}/articles/{articleId}                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ Invoke
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   AWS Lambda Funktionen                     │
│                                                             │
│  ┌─────────────────────┐  ┌─────────────────────────────┐  │
│  │   Article Handler   │  │     Catalog Handler         │  │
│  │  ListArticles       │  │  ListCatalogs               │  │
│  │  GetArticle         │  │  GetCatalog                 │  │
│  │  CreateArticle      │  │  CreateCatalog              │  │
│  │  UpdateArticle      │  │  UpdateCatalog              │  │
│  │  DeleteArticle      │  │  DeleteCatalog              │  │
│  └─────────────────────┘  └─────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Relation Handler                          │   │
│  │  AssignArticleToCatalog                             │   │
│  │  RemoveArticleFromCatalog                           │   │
│  └─────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │ AWS SDK
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    AWS DynamoDB                             │
│             Tabelle: InventoryTable-Dev                     │
│          (Single-Table Design, PAY_PER_REQUEST)             │
└─────────────────────────────────────────────────────────────┘
```

---

## Paketstruktur

```
asset-service/src/main/java/
│
├── com/sakai/inventory/api/
│   ├── dto/                        # Request- und Response-Objekte (Java Records)
│   │   ├── ArticleDTO.java         # API-Response für Artikel
│   │   ├── CatalogDTO.java         # API-Response für Kataloge
│   │   ├── DynamicFieldsDTO.java   # Dynamische Felder in der Response
│   │   ├── CreateArticleRequest.java
│   │   ├── UpdateArticleRequest.java
│   │   ├── CreateCatalogRequest.java
│   │   └── UpdateCatalogRequest.java
│   │
│   ├── handler/                    # Lambda-Handler (ein Handler = ein Endpunkt)
│   │   ├── ListArticlesHandler.java
│   │   ├── GetArticleHandler.java
│   │   ├── CreateArticleHandler.java
│   │   ├── UpdateArticleHandler.java
│   │   ├── DeleteArticleHandler.java
│   │   ├── ListCatalogsHandler.java
│   │   ├── GetCatalogHandler.java
│   │   ├── CreateCatalogHandler.java
│   │   ├── UpdateCatalogHandler.java
│   │   ├── DeleteCatalogHandler.java
│   │   ├── AssignArticleToCatalogHandler.java
│   │   └── RemoveArticleFromCatalogHandler.java
│   │
│   ├── infrastructure/             # Technische Infrastruktur
│   │   ├── DynamoDbClientFactory.java   # Zentraler DynamoDB-Client (Singleton)
│   │   └── KeyHelper.java               # DynamoDB Key-Hilfsmethoden
│   │
│   └── model/                      # DynamoDB-Datenmodelle
│       ├── Article.java
│       ├── Catalog.java
│       └── DynamicFieldValue.java
│
├── builder/
│   ├── GetItemBuilder.java         # Legacy-Builder (Low-Level API)
│   └── RequestBuilder.java         # Legacy-Builder (nicht fertig implementiert)
│
├── dao/
│   └── CreateKhachiDao.java        # Legacy-DAO
│
├── mapper/
│   └── DynDbMapper.java            # Legacy-Mapper-Interface
│
└── utility/
    ├── Utility.java                # HTTP-Response-Hilfsmethoden, ObjectMapper
    └── EnvironmentLoader.java      # Laden von Umgebungsvariablen
```

---

## IAM-Berechtigungskonzept

Es gibt zwei IAM-Rollen:

| Rolle | Verwendung | DynamoDB-Berechtigungen |
|---|---|---|
| `ReadOnlyRole` | GET-Handler (List, Get) | DescribeTable, Query, Scan, GetItem |
| `ReadWriteRole` | Write-Handler (Create, Update, Delete, Assign) | + PutItem, UpdateItem, DeleteItem |

---

## Umgebungsvariablen

| Variable | Wert | Beschreibung |
|---|---|---|
| `TABLE_NAME` | `InventoryTable-Dev` | Name der DynamoDB-Tabelle |
| `REGION` | `eu-central-1` | AWS-Region |

---

## CI/CD Pipeline

Der Build läuft über AWS CodeBuild (`buildspec.yaml`):

1. **Install** — Java 21 (Amazon Corretto) einrichten
2. **Pre-Build** — Java-Version und Maven prüfen
3. **Build** — `mvn clean package -Dmaven.test.skip=true` im `asset-service/`-Verzeichnis
4. **Post-Build** — JAR in S3 hochladen, SSM-Parameter setzen
5. **Artifact** — `asset-service-lambda.jar` wird als Build-Artefakt gespeichert
