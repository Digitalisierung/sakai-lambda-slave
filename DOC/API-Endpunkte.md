# REST-API Referenz

## Basis-URL

```
https://<api-id>.execute-api.eu-central-1.amazonaws.com/Prod
```

---

## Artikel-Endpunkte

### BE-06 — Artikelliste abrufen

```
GET /articles
```

**Query-Parameter (alle optional):**

| Parameter | Typ | Beschreibung |
|---|---|---|
| `name` | String | Teiltext-Suche im Artikelnamen |
| `state` | String | Exakter Status: `AVAILABLE`, `SOLD`, `UNDER_EVALUATION`, `RESERVED` |
| `catalogId` | String | Filtert nach zugewiesener Katalog-UUID |
| `sku` | String | Exakte SKU-Suche |

**Beispiele:**
```
GET /articles
GET /articles?state=AVAILABLE
GET /articles?name=Vase&state=AVAILABLE
GET /articles?catalogId=50fcb578-3a66-4bc5-82fe-c5bc10735d00
```

**Response 200:**
```json
[
  {
    "articleId": "199ac913-e653-4919-be0e-53e97db1ee05",
    "name": "Antike Vase aus der Ming-Dynastie",
    "sku": "SKU-7001",
    "description": "Eine gut erhaltene Porzellanvase...",
    "price": "0",
    "inventory": 1,
    "imageUrl": "https://stock-images.s3.eu-central-1.amazonaws.com/ming_vase.jpg",
    "catalogId": "CAT-ANTIQUES",
    "isActive": true,
    "isFeatured": true,
    "dynamicFields": {},
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-03-10T14:22:00Z"
  }
]
```

---

### BE-07 — Einzelnen Artikel abrufen

```
GET /articles/{id}
```

| Parameter | Beschreibung |
|---|---|
| `id` | UUID des Artikels (letztes Segment des DynamoDB-sortKey) |

**Response 200:** Einzelnes Artikel-Objekt (siehe BE-06)  
**Response 404:** `{"message": "Artikel nicht gefunden."}`

---

### BE-08 — Neuen Artikel anlegen

```
POST /articles
```

**Request-Body:**
```json
{
  "name": "Neue Vase",
  "sku": "SKU-9999",
  "description": "Beschreibung des Artikels",
  "price": 1500,
  "stock": 1,
  "imageUrl": "https://example.com/bild.jpg",
  "catalogId": "50fcb578-3a66-4bc5-82fe-c5bc10735d00",
  "isFeatured": false
}
```

**Pflichtfelder:** `name`, `sku`

**Response 201:** Erstelltes Artikel-Objekt mit generierter UUID  
**Response 400:** Validierungsfehler (Pflichtfeld fehlt)

---

### BE-09 — Artikel aktualisieren (Partial Update)

```
PUT /articles/{id}
```

**Request-Body** (alle Felder optional — nur gesetzte Felder werden überschrieben):
```json
{
  "name": "Geänderter Name",
  "state": "SOLD",
  "price": 2000
}
```

**Response 200:** Aktualisiertes Artikel-Objekt  
**Response 404:** Artikel nicht gefunden

---

### BE-10 — Artikel löschen

```
DELETE /articles/{id}
```

**Response 204:** Erfolgreich gelöscht (kein Body)  
**Response 404:** Artikel nicht gefunden

---

## Katalog-Endpunkte

### BE-11 — Katalogliste abrufen

```
GET /catalogs
```

**Response 200:**
```json
[
  {
    "catalogId": "50fcb578-3a66-4bc5-82fe-c5bc10735d00",
    "name": "Glass",
    "description": "",
    "color": "#fcd123",
    "productCount": 3,
    "createdAt": "2026-02-05T14:30:01Z",
    "updatedAt": "2026-02-05T14:30:01Z"
  }
]
```

---

### BE-12 — Einzelnen Katalog abrufen

```
GET /catalogs/{id}
```

**Response 200:** Einzelnes Katalog-Objekt  
**Response 404:** `{"message": "Katalog nicht gefunden."}`

---

### BE-13 — Neuen Katalog erstellen

```
POST /catalogs
```

**Request-Body:**
```json
{
  "name": "Neuer Katalog",
  "description": "Beschreibung",
  "color": "#FF5733"
}
```

**Pflichtfelder:** `name`, `color`  
**Validierung:** `color` muss ein gültiger Hex-Farbcode sein (`#RGB` oder `#RRGGBB`)

**Response 201:** Erstelltes Katalog-Objekt  
**Response 400:** Validierungsfehler

---

### BE-14 — Katalog aktualisieren (Partial Update)

```
PUT /catalogs/{id}
```

**Request-Body** (alle Felder optional):
```json
{
  "name": "Geänderter Name",
  "color": "#00FF00"
}
```

**Response 200:** Aktualisiertes Katalog-Objekt  
**Response 400:** Ungültiger Farbcode  
**Response 404:** Katalog nicht gefunden

---

### BE-15 — Katalog löschen

```
DELETE /catalogs/{id}
```

**Response 204:** Erfolgreich gelöscht  
**Response 404:** Katalog nicht gefunden

---

## Relation-Endpunkte

### BE-16 — Artikel einem Katalog zuweisen

```
POST /catalogs/{id}/articles
```

**Request-Body:**
```json
{
  "articleId": "199ac913-e653-4919-be0e-53e97db1ee05"
}
```

**Effekt:**
- Feld `catalogId` am Artikel-Item wird auf die Katalog-UUID gesetzt
- `productCount` am Katalog-Item wird um 1 erhöht

**Response 200:** `{"message": "Artikel erfolgreich dem Katalog zugewiesen."}`  
**Response 404:** Katalog oder Artikel nicht gefunden

---

### BE-17 — Artikel aus Katalog entfernen

```
DELETE /catalogs/{id}/articles/{articleId}
```

**Effekt:**
- Feld `catalogId` am Artikel-Item wird geleert
- `productCount` am Katalog-Item wird um 1 verringert (minimum 0)

**Response 200:** `{"message": "Artikel erfolgreich aus dem Katalog entfernt."}`  
**Response 400:** Artikel ist diesem Katalog nicht zugewiesen  
**Response 404:** Katalog oder Artikel nicht gefunden

---

## Allgemeine HTTP-Status-Codes

| Code | Bedeutung |
|---|---|
| 200 | Anfrage erfolgreich |
| 201 | Ressource erfolgreich erstellt |
| 204 | Ressource erfolgreich gelöscht (kein Body) |
| 400 | Ungültige Anfrage (Pflichtfeld fehlt, Validierungsfehler) |
| 404 | Ressource nicht gefunden |
| 500 | Interner Serverfehler |

---

## Standard-Response-Header

Alle Antworten enthalten folgende Header:

```
Content-Type: application/json
X-Custom-Header: application/json
```
