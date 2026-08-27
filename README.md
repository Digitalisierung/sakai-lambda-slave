catalog-service angelegt (24.08.2026)

Template-Validierung mit `sam validate`

Event generieren: `sam local generate-event apigateway aws-proxy > list-catalogs-event.json`

Im Infrastructure Projekt wird REST Api erstellt und konfiguriert, nicht HTTP Api.

# sakai-lambda-slave
Backend service for sakai-ng-master.

### JAR direkt in Lambda hochladen.

```bash
aws lambda update-function-code \
--function-name ${LAMBDA_FUNCTION_NAME} \
--s3-bucket ${S3_LAMBDA_ART_BUCKET} \
--s3-key asset-service-lambda.jar \
--profile sakai-cdk-lab
```

### Testen (lokal ausführen)

Um `Lambda Function` lokal auszuführen benötigt man zwei Dateien:

* Die Datei `env.json` definiert Umgebungsvariablen. Man kann dort für jede Lambda Funktion ihre eigene (separate)
  Umgebungsvariablen definieren oder gemeinsame Umgebungsvariable für alle Funktionen gemeinsamt. In diesem Projekt hat
  jede Lambda-Funktion ihre eigene Umgebung.
* Datei `event*.json` simuliert Request-Objekt und enthält Daten, welche dann den Attributen (Eigenschaften) der Klasse
  `APIGatewayProxyRequestEvent` entsprechen. Bzw. `event*.json` bildet die Klasse `APIGatewayProxyRequestEvent` ab.
  Befüllt Request-Objekt mit Daten. Für jede Lambda-Funktion wird eine separate `event*.json` Datei erstellt werden.

Dateien `env.json` und `*event.json` liegen im Ordner `<modul-name>/events` .
Es ergibt sich folgende Struktur (`asset-service` als Beispiel):

```shell script
asset-service/
├── env.json                          # Handler-spezifische Umgebungsvariablen, alle in einer Datei
├── events/
│   ├── list-articles-event.json      # Event für ListArticlesHandler
│   ├── get-article-event.json        # Event für GetArticleHandler
│   └── create-article-event.json     # Weitere Events...
└── template.yaml
```

Wenn man eine neue Funktion erstellt hat, muss man diese in `template.yaml` defenieren aber auch Umgebungsvariable in
`env.jsom` deklarieren und `*event.json` Datei erzeugen.

Bevor man eine Funktion aufruft, muss zuerst gebaut werden:

```bash
sam build <handler-name>
```

Dieser Befehl muss im gleichen Ordner aufgerufen werden, wo auch `template,yaml` liegt.

**Aufruf der Funktion (lokale Ausführung)**

```bash
sam local invoke ListArticlesHandler \
--event events/list-articles-event.json \
--env-vars env.json
```

für `ListArticlesHandler` oder:

```bash
sam local invoke GetArticleHandler \
--event events/get-article-event.json \
--env-vars env.json
```

für `GetArticleHandler` .

### Debuggen.

Zuerst `Remote JVM Debug` in IntelliJ IDEA einrichten. Das geht über `Edit configurations` und dann Plus Zeichen `+`

Wenn fertig, dann fölgenden Befehl ausführen.

```bash
sam local invoke ListArticlesHandler --event events/list-articles-event.json --env-vars env.json --debug-port 5858 --docker-network host
```

Darauf achten, dass die Portsnummer in `Remote JVM Debug` und `--debug-port` gleich sind.

### Event generieren.

Die Event-Datein kann man entweder manuell erstellen oder mit `sam local generate-event` ertzeugen. Der genau Befehl
lautet wie folgt:

```bash
sam local generate-event apigateway aws-proxy > <modul-name>/events/event.json
```

und danach in einem Editor anpassen (Path, Method und etc).

Konkret für `articles` :

```bash
sam local generate-event apigateway aws-proxy --method GET --path /articles --body "" > asset-service/events/list-articles-event.json
```

All diese Befehle müssen natürlich der Datei `template.yaml` abgeleitet werden. Path und Query und andere Parameter
müssen mit denen in `tempate.yaml` übereinstimmen.

```bash
sam local generate-event apigateway aws-proxy --method GET --path /articles/{id} --body "" > ./events/get-article-event.json
```
