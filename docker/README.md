## LocalStack für lokale Entwicklung.

**LocalStack** ist ein Cloud-Service-Emulator, welcher als Docker-Container auf dem lokalen Rechner läuft. Er simuliert
die APIs von Amazon Web Services (AWS), sodass Cloud-Anwendungen lokal entwickelt und getestet werden können,
ohne sich tatsächlich mit den echten AWS-Servern verbinden zu müssen. Vorteil: man braucht kein AWS-Konto, es entstehen
keine Kosten für die Nutzung von AWS-Infrastruktur, man braucht keine Internetverbindung - Cloud-Anwendung kann lokal
und offline getestet werden.

## 🚀 1: LocalStack starten.

In den Ordner mit `docker-compose.yaml` Datei navigieren und von dort LocalStack Container starten:

```bash
docker compose up -d
```

Prüfen ob LocalStack läuft:

```bash
aws --endpoint-url=http://localhost:4566 sts get-caller-identity
```

---

## 🗃️ 2: DynamoDB-Tabelle auf LocalStack erstellen.

DynamoDB Tabelle genau wie in **template.yaml** definiert erstellen:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
    --table-name InventoryTable \
    --attribute-definitions \
        AttributeName=partitionKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
        AttributeName=SKU,AttributeType=S \
    --key-schema \
        AttributeName=partitionKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{"IndexName": "SKU-index", "KeySchema": [{"AttributeName": "SKU", "KeyType": "HASH"}], "Projection": {"ProjectionType": "ALL"}}]'

```

Erstellte Tabelle prüfen:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb list-tables
```

```bash
aws --endpoint-url=http://localhost:4566 dynamodb describe-table --table-name InventoryTable
```

---

## 📊 3: Testdaten in DynamoDB einfügen

Beispiel-Katalog hinzufügen:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb put-item \
    --table-name InventoryTable \
    --item '{
        "partitionKey": {"S": "CAT#CAT-01"},
        "sortKey": {"S": "METADATA"},
        "color": {"S": "#fade55"},
        "name": {"S": "MacBooks Pro"},
        "description": {"S": ""},
        "createdAt": {"S": "1999.99"},
        "updatedAt": {"S": "25"}
    }'
```

Beispiel Artikel:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb put-item \
    --table-name InventoryTable \
    --item '{
  "partitionKey": {"S": "ARTCL#ART-12"},
  "sortKey": {"S": "ARTCL#ART-12"},
  "createdAt": {"S": "2026-02-05T14:30:00Z"},
  "description": {"S": ""},
  "dynamicFields": {"M": {}},
  "imageUrl": {"S": "http://s3"},
  "inventory": {"N": "1"},
  "isFeatured": {"BOOL": true},
  "name": {"S": "iPhone 15"},
  "price": {"S": ""},
  "SKU": {"S": "SKU-12-0"},
  "state": {"S": "AVAILABLE"},
  "updatedAt": {"S": ""}
}'
```

Weitere Beispieldaten:

```bash
aws --endpoint-url=http://kocalhost:4566 dynamodb put-item \
    --table-name InventoryTable \
    --item '{
    "partitionKey": {"S": ARTCL#ART-001"},
    "sortKey": {"S": ARTCL#ART-001"},
    "createdAt": {"S": "2024-01-15T10:30:00Z"},
    "description": {"S": "Eine gut erhaltene Porzellanvase mit blau-weißen Mustern, ca. 16. Jahrhundert."},
    "dynamicFields": {"M": {}}
    "imageUrl": {"S": "https://inventory-images.s3.eu-central-1.amazonaws.com/ming_vase.jpg"},
    "inventory": {"N": "1"},
    "isFeatured": {"BOOL": true},
    "name": {"S": "Antike Vase aus der Ming-Dynastie"},
    "price": {"S": "Nicht bewertet"},
    "SKU": {"S": "SKU-7001"},
    "state": {"S": "AVAILABLE"},
    "updatedAt": {"S": "2024-03-10T14:22:00Z"},
    }'
```

Artikel im Katalog:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb put-item \
    --table-name InventoryTable \
    --item '{
  "partitionKey": {"S": "CAT#CAT-01"},
  "sortKey": {"S": "ARTCL#ART-12"},
  "catalogId": {"S": "CAT-01"},
  "createdAt": {"S": "2026-02-05T14:30:00Z"},
  "description": {"S": ""},
  "dynamicFields": {"M": {}},
  "imageUrl": {"S": "http://s3"},
  "inventory": {"N": "1"},
  "isFeatured": {"BOOL": true},
  "name": {"S": "iPhone 15"},
  "price": {"S": ""},
  "SKU": {"S": "SKU-12-0"},
  "state": {"S": "AVAILABLE"},
  "updatedAt": {"S": ""}
}'
```

Alle Daten anzeigen:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb scan --table-name InventoryTable
```

---

## 🔧 5: Lambda-Funktion mit SAM lokal testen.

### A) Projekt-Struktur vorbereiten.

```
asset-service/
├── template.yaml
├── template-local.yaml
├── src/
│   └── com/sakai/inventory/api/handler/
│       └── GetCatalogsHandler.java
├── events/
│   └── test-event.json
├── env.json
└── docker-compose.yaml
```

**`events/test-event.json`:**

```json
{
  "httpMethod": "GET",
  "path": "/catalogs",
  "queryStringParameters": null
}
```

**`env.json`:**

```json
{
  "GetCatalogsHandler": {
    "DYNAMODB_ENDPOINT": "http://host.docker.internal:4566",
    "AWS_ACCESS_KEY_ID": "test",
    "AWS_SECRET_ACCESS_KEY": "test",
    "AWS_REGION": "eu-central-1"
  }
}
```

### B) SAM lokal ausführen.

```bash
# 1. SAM-App bauen (für Java)
sam build --use-container

# 2. Lambda-Funktion lokal testen
sam local invoke GetCatalogsHandler \
    --event events/test-event.json \
    --env-vars env.json \
    --docker-network host  # Für Mac/Windows: --docker-network bridge

# Alternative: Mit Debugging
sam local invoke GetCatalogsHandler \
    --event events/test-event.json \
    --env-vars env.json \
    --debug-port 5858 \
    --docker-network host
```

---

## 🌐 6: Komplette API mit SAM lokal starten.

```bash
# 1. Vollständige API lokal starten (API Gateway + Lambda)
sam local start-api \
    --port 3000 \
    --env-vars env.json \
    --docker-network host \
    --warm-containers LAZY

# 2. API testen (in neuem Terminal)
curl http://localhost:3000/catalogs
```

---

## 🐛 Debugging.

### A) Debugging mit SAM und IDE:

```bash
# 1. SAM mit Debug-Port starten
sam local invoke GetCatalogsHandler \
    --event events/test-event.json \
    --env-vars env.json \
    --debug-port 5858 \
    --docker-network host

# 2. In IDE (z.B. IntelliJ):
# - Run → Edit Configurations → "+" → Remote JVM Debug
# - Host: localhost, Port: 5858
# - Start Debug Session
```

### B) Logs anzeigen:

```bash
# LocalStack Logs
docker logs localstack -f

# Lambda Logs (wenn auf LocalStack deployed)
aws --endpoint-url=http://localhost:4566 logs describe-log-groups
```

---

## 🔄 (optional) Komplettes Deployment auf LocalStack.

Wenn du alles auf LocalStack haben willst (nicht nur SAM lokal):

```bash
# 1. CloudFormation Stack auf LocalStack deployen
aws --endpoint-url=http://localhost:4566 cloudformation create-stack \
    --stack-name asset-service \
    --template-body file://template-local.yaml \
    --capabilities CAPABILITY_IAM

# 2. Status prüfen
aws --endpoint-url=http://localhost:4566 cloudformation describe-stacks \
    --stack-name asset-service

# 3. Lambda auf LocalStack erstellen (wenn du den Code hast)
aws --endpoint-url=http://localhost:4566 lambda create-function \
    --function-name GetCatalogsHandler \
    --runtime java21 \
    --handler com.sakai.inventory.api.handler.ListArticlesHandler::handleRequest \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --environment Variables="{DYNAMODB_ENDPOINT=http://localhost:4566,AWS_ACCESS_KEY_ID=test,AWS_SECRET_ACCESS_KEY=test,AWS_REGION=eu-central-1}" \
    --code S3Bucket="hot-reload",S3Key="/tmp/localstack/zipfile.jar"  # Vereinfacht

# 4. Lambda testen
aws --endpoint-url=http://localhost:4566 lambda invoke \
    --function-name GetCatalogsHandler \
    --payload '{}' \
    response.json

cat response.json
```

---

## 📝 **Zusammenfassung der besten Vorgehensweise:**

### **Für einfache Entwicklung und Debugging:**

1. **LocalStack für DynamoDB** (Tabellen + Daten)
2. **SAM für Lambda lokal** (Ausführung + Debugging)
3. **SAM Local API für API Gateway** (lokale Endpoints)

### **Kurzanleitung:**

```
# 1. Starte LocalStack
docker compose up -d

# 2. Erstelle DynamoDB Tabelle
aws --endpoint-url=http://localhost:4566 dynamodb create-table ... 

# 3. Füge Testdaten ein
aws --endpoint-url=http://localhost:4566 dynamodb put-item ...

# 4. Baue Lambda-Funktion
sam build --use-container

# 5. Teste Lambda lokal
sam local invoke GetCatalogsHandler --event events/test-event.json --env-vars env.json

# 6. Oder starte komplette API
sam local start-api --port 3000 --env-vars env.json
```

### **Tipps:**

- **Für Docker-Netzwerk auf Mac/Windows:** Nutze `host.docker.internal` statt `localhost` in deinem Java-Code
- **Umgebungsvariablen:** Definiere alle AWS Credentials in `env.json`
- **Debugging:** Nutze `--debug-port` mit SAM und verbinde deine IDE
