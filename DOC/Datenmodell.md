# Datenmodell — DynamoDB Single-Table Design

## Übersicht

Das Projekt verwendet ein **Single-Table Design** in DynamoDB. Alle Entitäten (Artikel, Kataloge) werden in einer einzigen Tabelle gespeichert und durch das Muster der Partition- und Sortierschlüssel voneinander unterschieden.

**Tabellenname:** `InventoryTable-Dev`  
**Verwaltet durch:** AWS CDK (extern, nicht im SAM-Template)

---

## Tabellenstruktur

| Attribut | Typ | Beschreibung |
|---|---|---|
| `partitionKey` | String (S) | Primärschlüssel — bestimmt den Typ der Entität |
| `sortKey` | String (S) | Sortierschlüssel — eindeutiger Identifikator je Entität |

**Billing Mode:** PAY_PER_REQUEST  
**Point-in-Time Recovery:** deaktiviert (Dev-Umgebung)

---

## Key-Schema der Entitäten

### Kataloge

```
partitionKey = "CATALOGS"
sortKey      = "CATALOGS#<NAME_GROSS>#METADATA#<uuid>"
```

**Beispiele aus den Testdaten:**
```
partitionKey: "CATALOGS"
sortKey:      "CATALOGS#GLASS#METADATA#50fcb578-3a66-4bc5-82fe-c5bc10735d00"

partitionKey: "CATALOGS"
sortKey:      "CATALOGS#PAINTINGS#METADATA#58d686f0-7251-4601-b4bc-239943bfe9bf"
```

> **Hinweis:** Die UUID (letztes Segment nach dem letzten `#`) ist die **API-seitige ID** des Katalogs.
> Sie wird bei Abfragen via `KeyHelper.extractId(sortKey)` extrahiert.
> Das Attribut `catalogId` wird **nicht** als eigenes DynamoDB-Attribut gespeichert.

---

### Artikel

```
partitionKey = "ARTICLES"
sortKey      = "ARTICLES#<uuid>"           ← neue Artikel (via API angelegt)
              oder
sortKey      = "ARTICLES#<ART-ID>#<SKU>#<uuid>"  ← bestehende Testdaten
```

**Beispiele aus den Testdaten:**
```
partitionKey: "ARTICLES"
sortKey:      "ARTICLES#ART-001#SKU-7001#199ac913-e653-4919-be0e-53e97db1ee05"

partitionKey: "ARTICLES"
sortKey:      "ARTICLES#ART-002#SKU-7002#03872d62-b047-418b-842e-362ca616653b"
```

> **Hinweis:** Die UUID (letztes Segment nach dem letzten `#`) ist die **API-seitige ID** des Artikels.
> Neue Artikel werden mit dem Format `ARTICLES#<uuid>` angelegt.
> Beide Formate sind kompatibel, da die Suche über `contains(sortKey, uuid)` arbeitet.

---

## Attribute der Entitäten

### Artikel-Attribute

| Attribut | DynamoDB-Typ | Pflicht | Beschreibung |
|---|---|---|---|
| `partitionKey` | S | ✅ | Immer `"ARTICLES"` |
| `sortKey` | S | ✅ | `"ARTICLES#<uuid>"` oder `"ARTICLES#<id>#<sku>#<uuid>"` |
| `name` | S | ✅ | Anzeigename des Artikels |
| `sku` | S | ✅ | Stock Keeping Unit (eindeutige Lagerkennung) |
| `description` | S | — | Beschreibung des Artikels |
| `price` | N | — | Preis als ganzzahliger Wert |
| `stock` | N | — | Lagerbestand (Anzahl Einheiten) |
| `imageUrl` | S | — | URL zum Artikelbild (S3) |
| `state` | S | — | Status: `AVAILABLE`, `SOLD`, `UNDER_EVALUATION`, `RESERVED` |
| `isFeatured` | BOOL | — | Hervorhebungsstatus |
| `catalogId` | S | — | UUID des zugewiesenen Katalogs (leer = kein Katalog) |
| `createdAt` | S | ✅ | Erstellungszeitstempel (ISO-8601) |
| `updatedAt` | S | ✅ | Letzter Änderungszeitstempel (ISO-8601) |

---

### Katalog-Attribute

| Attribut | DynamoDB-Typ | Pflicht | Beschreibung |
|---|---|---|---|
| `partitionKey` | S | ✅ | Immer `"CATALOGS"` |
| `sortKey` | S | ✅ | `"CATALOGS#<NAME>#METADATA#<uuid>"` |
| `name` | S | ✅ | Anzeigename des Katalogs |
| `color` | S | ✅ | Hex-Farbcode (z.B. `#fcd123`) |
| `description` | S | — | Beschreibung des Katalogs |
| `productCount` | N | — | Anzahl zugewiesener Artikel (wird automatisch gepflegt) |
| `createdAt` | S | ✅ | Erstellungszeitstempel (ISO-8601) |
| `updatedAt` | S | ✅ | Letzter Änderungszeitstempel (ISO-8601) |

> **Hinweis:** `catalogId` wird **nicht** als eigenes Attribut in DynamoDB gespeichert
> (`@DynamoDbIgnore`). Sie wird zur Laufzeit aus dem `sortKey` extrahiert.

---

## Abfragemuster (Access Patterns)

| Operation | DynamoDB-Operation | Bedingung |
|---|---|---|
| Alle Artikel abrufen | Query | `partitionKey = "ARTICLES"` |
| Artikel filtern | Query + FilterExpression | PK = `"ARTICLES"` + Filter auf name/state/catalogId/sku |
| Einzelnen Artikel abrufen | Query + contains-Filter | `PK = "ARTICLES"` + `contains(sortKey, uuid)` |
| Artikel erstellen | PutItem | — |
| Artikel aktualisieren | UpdateItem (ignoreNulls=true) | vollständiger Key aus Query |
| Artikel löschen | DeleteItem | vollständiger Key aus Query |
| Alle Kataloge abrufen | Query | `partitionKey = "CATALOGS"` |
| Einzelnen Katalog abrufen | Query + contains-Filter | `PK = "CATALOGS"` + `contains(sortKey, uuid)` |

---

## Bekannte Einschränkungen

1. **Suche nach Artikel-ID:** Da der vollständige `sortKey` beim API-Aufruf unbekannt ist, wird für GET/UPDATE/DELETE eine Query mit `contains(sortKey, uuid)` als FilterExpression durchgeführt. Bei sehr großen Datenmengen (>10.000 Artikel) ist dies nicht optimal — ein GSI auf einem dedizierten `articleId`-Attribut wäre effizienter.

2. **Kein SKU-Index:** Der ursprünglich geplante GSI `SKU-index` ist in der CDK-verwalteten Tabelle nicht vorhanden. SKU-Suche läuft daher als FilterExpression.

3. **productCount:** Der Zähler wird durch die Handler `AssignArticleToCatalog` und `RemoveArticleFromCatalog` gepflegt, aber nicht atomar (kein DynamoDB-Transaction). Bei gleichzeitigen Zugriffen könnte der Wert kurzzeitig inkonsistent sein.
