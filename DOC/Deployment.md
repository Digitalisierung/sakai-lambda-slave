# Deployment & lokale Entwicklung

## Voraussetzungen

| Tool | Version | Zweck |
|---|---|---|
| Java (Amazon Corretto) | 21 | Kompilieren und Ausführen |
| Maven | 3.9.x | Build-Tool |
| AWS CLI | 2.x | AWS-Zugriff |
| AWS SAM CLI | 1.x | Lokales Testen und Deployment |
| Docker | aktuell | SAM local (Lambda-Simulation) |
| LocalStack | aktuell | Lokale DynamoDB-Simulation |

---

## 1. Projekt bauen

```bash
cd asset-service/
mvn clean package
```

Das Shade-Plugin erstellt ein Fat-JAR unter:
```
asset-service/target/asset-service-lambda.jar
```

Build ohne Tests:
```bash
mvn clean package -Dmaven.test.skip=true
```

---

## 2. Lokale Entwicklung mit LocalStack

### LocalStack starten

```bash
cd docker/
docker compose up -d
```

Prüfen ob LocalStack läuft:
```bash
aws --endpoint-url=http://localhost:4566 sts get-caller-identity
```

### DynamoDB-Tabelle lokal erstellen

```bash
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
    --table-name InventoryTable-Dev \
    --attribute-definitions \
        AttributeName=partitionKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
    --key-schema \
        AttributeName=partitionKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST
```

### Testdaten einspielen

Die Testdaten liegen in `asset-service/events/exhibition-items.json` (BatchWriteItem-Format):

```bash
aws --endpoint-url=http://localhost:4566 dynamodb batch-write-item \
    --request-items file://asset-service/events/exhibition-items.json
```

---

## 3. Lambda lokal testen mit SAM

### Projekt bauen (für SAM)

```bash
cd asset-service/
sam build --use-container
```

### Einzelne Lambda-Funktion aufrufen

```bash
sam local invoke ListArticlesHandler \
    --event events/event.json \
    --env-vars env.json \
    --docker-network host
```

### Lokale API starten

```bash
sam local start-api \
    --port 3000 \
    --env-vars env.json \
    --docker-network host \
    --warm-containers LAZY
```

Endpunkte sind dann erreichbar unter:
```
http://localhost:3000/articles
http://localhost:3000/catalogs
```

### env.json für lokale Entwicklung

Datei `asset-service/env.json` (nicht im Git-Repository, wird ignoriert):
```json
{
  "ListArticlesHandler": {
    "TABLE_NAME": "InventoryTable-Dev",
    "REGION": "eu-central-1",
    "AWS_ACCESS_KEY_ID": "test",
    "AWS_SECRET_ACCESS_KEY": "test"
  }
}
```

---

## 4. Deployment auf AWS

### Manuell per AWS CLI

```bash
cd asset-service/
mvn clean package -Dmaven.test.skip=true

aws lambda update-function-code \
    --function-name ${LAMBDA_FUNCTION_NAME} \
    --s3-bucket ${S3_LAMBDA_ART_BUCKET} \
    --s3-key asset-service-lambda.jar \
    --profile sakai-cdk-lab
```

### Über CI/CD (AWS CodeBuild)

Der Build wird automatisch über `buildspec.yaml` im Projektroot ausgeführt:

1. JAR wird gebaut (`mvn clean package`)
2. JAR wird in S3 hochgeladen (`s3://${S3_LAMBDA_ART_BUCKET}/asset-service-lambda.jar`)
3. SSM-Parameter werden gesetzt:
   - `/sakai/${STAGE_NAME}/lambda/artifact-bucket-name`
   - `/sakai/${STAGE_NAME}/lambda/artifact-key`

---

## 5. Umgebungsvariablen

| Variable | Beschreibung | Beispielwert |
|---|---|---|
| `TABLE_NAME` | DynamoDB-Tabellenname | `InventoryTable-Dev` |
| `REGION` | AWS-Region | `eu-central-1` |
| `S3_LAMBDA_ART_BUCKET` | S3-Bucket für das JAR (CI/CD) | `jar-artifact-bucket-...` |
| `STAGE_NAME` | Deployment-Stage (CI/CD) | `Dev` |
| `LAMBDA_FUNCTION_NAME` | Lambda-Funktionsname (manuelles Deploy) | `ListArticlesHandler` |

---

## 6. Logs abrufen

```bash
# Lambda-Logs in CloudWatch anzeigen
aws logs tail /aws/lambda/ListArticlesHandler --follow --profile sakai-cdk-lab

# LocalStack-Logs
docker logs localstack -f
```
