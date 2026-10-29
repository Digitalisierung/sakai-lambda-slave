# KHACHI KAMRI KHAN – Asset Inventory Management System.

Cloudbasiertes Inventar- und Asset-Management-System zur Verwaltung von Inventar-Artikeln, Beständen und Gegenständen.

Dieses Repository enthält das Backend-Teil der Anwendung und eine CI/CD-Pipeline für automatisierte Deployments.

Die Anwendung ist Teil eines größeren Systems mit Angular-Frontend und CDK-Infrastruktur, welche als separate Projekte
geführt werden.

Die Anwendung ist als serverloses Java-Backend auf Basis von AWS Lambda, Amazon API Gateway und DynamoDB aufgebaut.

---

## Inhaltsverzeichnis.

- [Überblick](#überblick)
- [Aktueller Funktionsumfang](#aktueller-funktionsumfang)
- [Geplante Funktionen](#geplante-funktionen)
- [Architektur](#architektur)
- [Projektstruktur](#projektstruktur)
- [Module](#module)
- [Technologie-Stack](#technologie-stack)
- [API-Übersicht](#api-übersicht)
- [Datenmodell](#datenmodell)
- [DynamoDB-Zugriffsmuster](#dynamodb-zugriffsmuster)
- [Build](#build)
- [Lokale Entwicklung](#lokale-entwicklung)
- [LocalStack](#localstack)
- [AWS SAM](#aws-sam)
- [Deployment](#deployment)
- [CI/CD](#cicd)
- [Umgebungsvariablen](#umgebungsvariablen)
- [Debugging](#debugging)
- [Tests](#tests)
- [Roadmap](#roadmap)
- [Hinweise](#hinweise)

---

## Überblick.

**KHACHI KAMRI KHAN** ist ein cloudbasiertes System zur Verwaltung von Inventar, Assets und Beständen.

Das Backend stellt REST-API-Endpunkte bereit für:

- Verwaltung von Inventar-Artikeln.
- Verwaltung von Katalogen.
- Katalogisierung von Artikeln.
- Bestandsverwaltung.
- Verfügbarkeits- und Statusinformationen.
- Hervorhebung ausgewählter Artikel.
- Erweiterung von Artikeln durch dynamische Felder.

Das Projekt ist als Maven-Multi-Modul-Anwendung umgesetzt. Die fachlichen Backend-Bereiche werden als Lambda-fähige
JAR-Dateien gebaut und über mehrere AWS-Lambda-Funktionen hinter einem API-Gateway bereitgestellt. (Microservice
Architektur)

Die Persistenz erfolgt über DynamoDB. Das Datenmodell ist auf cloudnative Zugriffsmuster ausgelegt und verwendet
Partition Keys, Sort Keys und Global Secondary Indexes.

---

## Aktueller Funktionsumfang.

Die aktuelle Version unterstützt:

- Erstellen von Inventar-Artikeln.
- Aktualisieren von Inventar-Artikeln.
- Löschen von Inventar-Artikeln.
- Auflisten von Inventar-Artikeln.
- Abrufen einzelner Artikel per ID.
- Erstellen von Katalogen.
- Aktualisieren von Katalogen.
- Auflisten von Katalogen.
- Abrufen einzelner Kataloge per ID.
- Auflisten von Artikeln innerhalb eines Katalogs.
- Verwaltung von Lagerbestand / Bestand.
- Verfügbarkeit und Statusmodell für Artikel.
- Featured-Artikel.
- dynamische Zusatzfelder für Artikel.
- serverlose Backend-Architektur.
- lokale Entwicklung mit LocalStack.
- lokale Lambda- und API-Ausführung mit AWS SAM.

---

## Geplante Funktionen.

Für kommende Versionen sind geplant:

- Benutzer-Login.
- Rollen- und Rechteverwaltung.
- geschützte API-Endpunkte.
- Import von Inventar-Daten.
- Export von Artikeln, Katalogen und Berichten.
- Bild-Upload und Bildverwaltung.
- Änderungshistorie.
- Audit-Trail.
- erweiterte Filter- und Suchfunktionen.
- weitere Integrationen mit dem Angular-Frontend.

---

## Architektur.

Das Backend folgt einer serverlosen Architektur:

```
text
Angular-Frontend
|
v
Amazon API Gateway
|
v
AWS Lambda
|
v
Amazon DynamoDB
```

Build- und Deployment-seitig ist das Projekt für automatisierte Artefakt-Erzeugung und Bereitstellung vorbereitet:

```
text
Entwicklung / CI
|
v
Maven Build
|
v
Lambda Fat JARs
|
v
JAR in S3 Artefakt-Bucket kopieren
|
v
SSM Parameter Store setzen
|
v
AWS Lambda Deployment
```

Wichtige Architekturmerkmale:

- Java 21.
- AWS Lambda als Compute Layer.
- API Gateway als REST-Schnittstelle.
- DynamoDB als NoSQL-Datenbank.
- AWS SAM für serverlose Ressourcen.
- AWS CDK für Infrastruktur- und Pipeline-Code.
- Maven Multi-Modul-Struktur.
- getrennte Module für Backend Services.
- fachliche Trennung der Backen-Services.
- lokale Entwicklung mit Docker, LocalStack und AWS SAM.

---

## Projektstruktur.

```text
.
├── asset-service/
│   ├── src/
│   ├── pom.xml
│   └── template.yaml
│
├── catalog-service/
│   ├── src/
│   ├── pom.xml
│   ├── README.md
│   └── template.yaml
│
├── lambda-pipeline/
│   ├── src/
│   ├── pom.xml
│   └── cdk.json
│
├── docker/
│   └── docker-compose.yaml
│
├── docs/
│
├── buildspec.yaml
├── pom.xml
├── template.yaml
├── mvnw
└── mvnw.cmd
```

---

## Module.

### Root-Projekt:

Das Root-Projekt ist ein Maven-Parent-Projekt. Es definiert gemeinsame Versionen, Dependency Management und die
Zusammensetzung der Module.

Wichtige Module:

- `asset-service`
- `catalog-service`
- `lambda-pipeline`
- `docs`

Das Modul `docs` enthält Dokumentationsinhalte und ist für die Laufzeit nicht erforderlich.

---

### `asset-service`:

Das Modul `asset-service` enthält die fachliche Logik für Inventar-Artikel und Assets.

Zuständigkeiten:

- Artikel erstellen.
- Artikel aktualisieren.
- Artikel löschen.
- Artikel abrufen.
- Artikel auflisten.
- Artikel einem Katalog zuordnen.
- Artikel innerhalb eines Katalogs auflisten.
- dynamische Artikelfelder verwalten.
- Bestands- und Verfügbarkeitsinformationen abbilden.

Typische Paketstruktur:

```text
asset-service/src/main/java/com/sakai/inventory/
├── api/
│   ├── dto/
│   └── handler/
├── domain/
│   ├── model/
│   └── service/
├── infrastructure/
│   ├── factory/
│   ├── mapper/
│   └── repository/
└── shared/
├── exception/
└── util/
```

Das Modul wird als Lambda-fähiges Fat JAR gebaut:

```text
asset-service/target/asset-service-lambda.jar
```

---

### `catalog-service`:

Das Modul `catalog-service` enthält die fachliche Logik für Kataloge.

Zuständigkeiten:

- Katalog erstellen.
- Katalog aktualisieren.
- Katalog abrufen.
- Kataloge auflisten.
- Katalog-Metadaten verwalten.

Das Modul nutzt unter anderem Validierung und Mapping und wird ebenfalls als Lambda-fähiges Fat JAR gebaut:

```text
catalog-service/target/catalog-service-lambda.jar
```

---

### `lambda-pipeline`:

Das Modul `lambda-pipeline` enthält Infrastruktur-Code auf Basis von AWS CDK v2.

Zuständigkeiten:

- Infrastruktur für Lambda-Artefakte.
- Artefakt-Bucket.
- Pipeline-Ressourcen.
- Stage-spezifische Konfiguration.
- SSM-Parameter-Integration.
- Build- und Deployment-Unterstützung.

Dieses Modul ist nicht für Request-Verarbeitung zuständig, sondern für Infrastruktur und Deployment-Automatisierung.

---

## Technologie-Stack.

### Laufzeit und Sprache:

- Java 21
- Maven
- AWS Lambda Java Runtime.

### AWS:

- AWS Lambda
- Amazon API Gateway.
- Amazon DynamoDB.
- Amazon S3.
- AWS Systems Manager Parameter Store.
- AWS IAM.
- AWS CloudFormation / SAM.
- AWS CDK.
- AWS CodeBuild-kompatibler Build.

### Bibliotheken:

- AWS Lambda Java Core
- AWS Lambda Java Events
- AWS SDK for Java v2
- DynamoDB Enhanced Client
- Jackson
- SLF4J
- Hibernate Validator
- MapStruct
- JUnit
- Mockito
- AssertJ
- JaCoCo

### Lokale Entwicklung:

- Docker
- LocalStack
- AWS SAM CLI
- AWS CLI
- optional: `awslocal`

---

## API-Übersicht.

Die REST-API wird über API Gateway bereitgestellt.

### Artikel-API

| Methode | Pfad                      | Beschreibung                         |
|---------|---------------------------|--------------------------------------|
| `GET`   | `/articles`               | Artikel auflisten                    |
| `GET`   | `/articles/{id}`          | Artikel per ID abrufen               |
| `POST`  | `/articles`               | Artikel erstellen / Batch-Erstellung |
| `PUT`   | `/articles/{id}`          | Artikel aktualisieren                |
| `GET`   | `/catalogs/{id}/articles` | Artikel eines Katalogs auflisten     |

### Katalog-API

| Methode | Pfad             | Beschreibung           |
|---------|------------------|------------------------|
| `GET`   | `/catalogs`      | Kataloge auflisten     |
| `GET`   | `/catalogs/{id}` | Katalog per ID abrufen |
| `POST`  | `/catalogs`      | Katalog erstellen      |
| `PUT`   | `/catalogs/{id}` | Katalog aktualisieren  |

---

## Datenmodell.

Das System basiert fachlich auf zwei zentralen Konzepten:

1. Artikel / Asset
2. Katalog

### Artikel / Asset

Ein Artikel repräsentiert einen Inventargegenstand oder ein Asset.

Typische Felder:

- ID
- Name
- SKU
- Beschreibung
- Bestand / Menge
- Bild-URL
- Aktiv-Status
- Featured-Status
- Katalog-ID
- Status / Zustand
- Erstellzeitpunkt
- Änderungszeitpunkt
- dynamische Zusatzfelder

### Katalog

Ein Katalog gruppiert und organisiert Artikel.

Typische Felder:

- ID
- Name
- Beschreibung
- Farbe oder Darstellungs-Metadaten
- Erstellzeitpunkt
- Änderungszeitpunkt

### Dynamische Felder

Artikel können dynamische Felder enthalten. Dadurch kann ein Artikel zusätzliche, kontextspezifische Informationen
speichern, ohne dass für jeden neuen Asset-Typ das feste Datenmodell erweitert werden muss.

Beispiele:

- Seriennummer
- Standort
- Hersteller
- Garantieende
- Zustand
- verantwortliche Person
- Gerätetyp
- interne Inventarnummer

---

## DynamoDB-Zugriffsmuster.

Die Anwendung verwendet DynamoDB als primären Datenspeicher.

Die Haupttabelle speichert Artikel- und Katalogdaten mit einem Partition-Key-/Sort-Key-Modell.

Wichtige Schlüssel und Attribute:

```text
partitionKey
sortKey
entityType
catalogId
```

Global Secondary Indexes (GSI) unterstützen zusätzliche Abfragen:

```text
GSI_entityType
GSI_ItemsInCatalogs
```

Unterstützte Zugriffsmuster:

- Artikel per ID abrufen.
- Artikel auflisten.
- Kataloge auflisten.
- Katalog per ID abrufen.
- Artikel eines Katalogs abrufen.
- Artikel aktualisieren.
- Katalog aktualisieren.
- Artikel gesammelt schreiben.
- Katalog löschen.
- Artikel einzeln oder gesammelt löschen.

---

## Build.

Gilt nicht für `lambda-pipeline` Modul.

### Voraussetzungen:

- Java 21
- Maven 3.8+
- Docker
- AWS SAM CLI
- AWS CLI
- optional: LocalStack CLI / `awslocal` / `samlocal`

---

### Alle Module bauen:

Vom Root-Verzeichnis aus:

```bash
./mvnw clean package
```

Oder mit lokal installiertem Maven:

```bash
mvn clean package
```

Build ohne Tests:

```bash
./mvnw clean package -DskipTests
```

Tests werden zwar compiliert, aber die Ausführung von Tests wird übersprungen.

Alternativ:

```bash
./mvnw clean package -Dmaven.test.skip=true 
```

Tests werden weder compiliert noch ausgeführt.

Erwartete Build-Artefakte:

```text
asset-service/target/asset-service-lambda.jar
catalog-service/target/catalog-service-lambda.jar
```

---

## Lokale Entwicklung.

Empfohlener Ablauf:

1. Dateien `event.json` und `env.json` mit Environment Werten vorbereiten.
2. Projekt mit SAM bauen.
3. SAM-Anwendung lokal starten oder einzelne Lambda-Funktionen ausführen.
4. API-Endpunkte mit dem Event aus `event.json` testen.
5. Empfohlen: DynamoDB lokal über LocalStack verwenden. (Standard wird die entfernte DB verwendet).

---

## LocalStack.

LocalStack simuliert AWS-Dienste lokal in Docker.

### LocalStack starten:

Im Verzeichnis mit `docker-compose.yaml`:

```bash
docker compose up -d
```

ausführen und dann den Status prüfen:

```bash
aws --endpoint-url=http://localhost:4566 sts get-caller-identity
```

Oder mit `awslocal` (falls installiert):

```bash
awslocal sts get-caller-identity
```

---

### Verwendete LocalStack-Services:

Für die lokale Entwicklung mit LocalStack können folgende Dienste kostenlos genutzt werden:

- API Gateway
- API Gateway v2
- CloudFormation
- CloudWatch
- DynamoDB
- DynamoDB Streams
- EventBridge
- IAM
- Lambda
- Logs
- S3
- Secrets Manager
- SNS
- SQS
- SSM
- STS

Weitere Dienste sind in der kostenpflichtigen Version vorhanden.

---

## AWS SAM.

Das Projekt enthält SAM-Templates zur lokalen und cloudbasierten Ausführung.

### Mit SAM Anwendung bauen:

```bash
sam build --use-container
```

Oder gezielt für eine Funktion:

```bash
sam build <HandlerName>
```

Beispiel:

```bash
sam build ListArticlesHandler
```

Der Befehl muss in dem Verzeichnis ausgeführt werden, in dem sich das jeweilige `template.yaml` befindet.

---

### Lokale API starten.

```bash
sam local start-api --port 3000 --docker-network host
```

Je nach Betriebssystem und Docker-Netzwerk kann statt `localhost` auch `host.docker.internal` notwendig sein.

---

### Beispielaufrufe:

Kataloge auflisten:

```bash
curl http://localhost:3000/catalogs
```

Artikel auflisten:

```bash
curl http://localhost:3000/articles
```

Artikel abrufen:

```bash
curl http://localhost:3000/articles/ART-001
```

Katalog abrufen:

```bash
curl http://localhost:3000/catalogs/CAT-001
```

Artikel eines Katalogs auflisten:

```bash
curl http://localhost:3000/catalogs/CAT-001/articles
```

---

## Lokale Lambda-Ausführung.

Für die lokale Ausführung einzelner Lambda-Funktionen werden typischerweise zwei Dateien benötigt:

1. `env.json`
2. `*-event.json`

### `env.json`

Diese Datei enthält Umgebungsvariablen für Lambda-Funktionen.

Beispiel:

```json
{
"Parameters": {
  "REGION": "eu-central-1", 
  "TABLE_NAME": "InventoryCatalogs", 
  "DYNAMODB_ENDPOINT": "http://host.docker.internal:4566", 
  "AWS_ACCESS_KEY_ID": "test", 
  "AWS_SECRET_ACCESS_KEY": "test", 
  "AWS_REGION": "eu-central-1"
  }
}
```

### Event-Dateien

Eine Event-Datei simuliert den Request, der normalerweise vom API Gateway an die Lambda-Funktion übergeben wird.

Beispielstruktur:

```text
asset-service/
├── env.json
├── events/
│   ├── list-articles-event.json
│   ├── get-article-event.json
│   └── create-article-event.json
└── template.yaml
```

### Funktion lokal ausführen:

Beispiel für `ListArticlesHandler`:
```bash
sam local invoke ListArticlesHandler \
--event events/list-articles-event.json \
--env-vars env.json
```

Beispiel für `GetArticleHandler`:
```bash
sam local invoke GetArticleHandler \
--event events/get-article-event.json \
--env-vars env.json
```

---

## Events generieren:

Event-Dateien können manuell erstellt oder über SAM generiert werden.

Allgemein:

```bash
sam local generate-event apigateway aws-proxy > events/event.json
```

Beispiel für `GET /articles`:

```bash
sam local generate-event apigateway aws-proxy \
--method GET \
--path /articles \
--body "" \
> asset-service/events/list-articles-event.json
```

Beispiel für `GET /articles/{id}`:

```bash
sam local generate-event apigateway aws-proxy \
--method GET \
--path /articles/ART-001 \
--body "" \
> asset-service/events/get-article-event.json
```

Wichtig: Methode, Pfad, Query-Parameter und Body müssen zur Definition im jeweiligen `template.yaml` passen.

---

## Beispiel-Payloads.

### Artikel erstellen:

```json
{
"name": "MacBook Pro 14",
"sku": "MBP-14-001",
"description": "Entwickler-Notebook",
"stock": 1,
"imageUrl": "https://example.com/images/macbook-pro-14.png",
"active": true,
"isFeatured": true,
"catalogId": "CAT-IT-EQUIPMENT",
"createdAt": "2026-01-01T10:00:00Z"
}
```

### Artikel aktualisieren:

```json
{
"name": "MacBook Pro 14",
"sku": "MBP-14-001",
"description": "Aktualisiertes Entwickler-Notebook",
"stock": 1,
"imageUrl": "https://example.com/images/macbook-pro-14.png",
"active": true,
"isFeatured": false,
"catalogId": "CAT-IT-EQUIPMENT"
}
```

### Katalog erstellen:

```json
{
"name": "IT-Equipment",
"description": "Laptops, Monitore, Tastaturen und weiteres IT-Inventar",
"color": "#3B82F6"
}
```

---

## Deployment.

Das Projekt kann über AWS SAM oder über eine separate CI/CD-Pipeline deployed werden.

### Deployment mit SAM

Geführtes Deployment:

```bash
sam deploy --guided
```

Mögliche Werte:

```text
Stack Name: khachi-kamri-khan-backend
AWS Region: eu-central-1
Confirm changes before deploy: Y
Allow SAM CLI IAM role creation: Y
Save arguments to samconfig.toml: Y
```

Folge-Deployments:

```bash
sam deploy
```

---

## Direktes Aktualisieren einer Lambda-Funktion.

Wenn das Artefakt bereits lokal gebaut wurde, kann eine Lambda-Funktion direkt aktualisiert werden.

Beispiel mit lokalem JAR:

```bash
aws lambda update-function-code \
--function-name <LAMBDA_FUNCTION_NAME> \
--zip-file fileb://asset-service/target/asset-service-lambda.jar \
--profile <AWS_PROFILE>
```

Beispiel mit S3-Artefakt:

```bash
aws lambda update-function-code \
--function-name <LAMBDA_FUNCTION_NAME> \
--s3-bucket <S3_LAMBDA_ART_BUCKET> \
--s3-key asset-service-lambda.jar \
--profile <AWS_PROFILE>
```

---

## CI/CD.

Das Repository enthält eine `buildspec.yaml` für AWS-CodeBuild-kompatible Builds.

Der Build-Prozess:

1. richtet Java 21 / Amazon Corretto 21 ein
2. baut das Maven-Multi-Modul-Projekt
3. erzeugt Lambda-Fat-JARs
4. prüft, ob erwartete Handler-Klassen im JAR vorhanden sind
5. lädt Lambda-Artefakte in einen S3-Bucket hoch
6. speichert Artefaktinformationen im AWS Systems Manager Parameter Store

Erzeugte Artefakte:

```text
asset-service-lambda.jar
catalog-service-lambda.jar
```

Typische SSM-Parameter:

```text
/sakai/{stage}/lambda/artifact-bucket-name
/sakai/{stage}/lambda/asset-service/artifact-key
/sakai/{stage}/lambda/catalog-service/artifact-key
```

Benötigte Pipeline-Umgebungsvariablen:

```text
S3_LAMBDA_ART_BUCKET
STAGE_NAME
```

---

## Umgebungsvariablen.

Typische Lambda-Umgebungsvariablen:

```text
REGION
TABLE_NAME
```

Für lokale Entwicklung zusätzlich:

```text
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=eu-central-1
DYNAMODB_ENDPOINT=http://host.docker.internal:4566
```

---

## Debugging.

### Remote Debugging mit AWS SAM:

Beispiel:

```bash
sam local invoke ListArticlesHandler \
--event events/list-articles-event.json \
--env-vars env.json \
--debug-port 5858 \
--docker-network host
```

Danach in der IDE eine Remote-JVM-Debug-Konfiguration einrichten.

Für IntelliJ IDEA:

```text
Run -> Edit Configurations -> Add New Configuration -> Remote JVM Debug
```

Werte:

```text
Host: localhost
Port: 5858
```

Wichtig: Der Port in der IDE muss mit `--debug-port` übereinstimmen.

---

## Logs.

### LocalStack-Logs anzeigen.

```bash
docker logs localstack -f
```

### CloudWatch-Logs über LocalStack anzeigen.

```bash
aws --endpoint-url=http://localhost:4566 logs describe-log-groups
```

Oder:

```bash
awslocal logs describe-log-groups
```

---

## Tests.

Alle Tests ausführen:

```bash
./mvnw test
```

Build inklusive Test- und Coverage-Phase:

```bash
./mvnw clean package
```

JaCoCo ist in den Service-Modulen für Coverage-Reports konfiguriert.

Typischer Report-Pfad:

```text
target/site/jacoco/index.html
```

---

## Entwicklungsnotizen.

### Maven Wrapper verwenden

Linux/macOS oder Windoes:

```bash
./mvnw clean package
```

## Sicherheit.

Authentifizierung und Autorisierung sind für eine spätere Version geplant.

Geplante sicherheitsrelevante Funktionen:

- Benutzer-Login.
- rollenbasierte Berechtigungen.
- geschützte API-Endpunkte.
- Audit- und Historienfunktionen.

Die aktuelle Version konzentriert sich auf die Kernfunktionen für Inventar- und Katalogverwaltung.

---

## Fachlicher Kontext.

**KHACHI KAMRI KHAN – Asset Inventory Management System** ist vorgesehen für:

- Büros
- kleine Unternehmen
- Selbständige
- Vereine
- kleine Shops
- IT-Equipment-Verwaltung
- allgemeines Inventar- und Bestandsmanagement

Das Backend in diesem Repository ist Teil einer größeren cloudbasierten Web-Anwendung.

---

## Lizenz.

Aktuell ist keine Lizenz definiert.

Vor einer öffentlichen Open-Source-Veröffentlichung oder produktiven Weitergabe sollte eine passende Lizenz ergänzt
werden.

---

## Maintainer.

Das Projekt wird im Rahmen der Entwicklung von **KHACHI KAMRI KHAN – Asset Inventory Management System** gepflegt.
