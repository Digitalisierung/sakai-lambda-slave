## DynamoDB Schema Design – Inventory ans Assets Management.

Für Schema Desing wird Single Table Pattern angewendet. Es werden sowohl Artikle (Article) als auch Kataloge (Im
Frontend: eine Samlung von Artiklen bzw. Organisationseinheit für Artikle) in einer Tabelle gespeichert.

### Tabelle: `ArticlesAndCategory` .

| Attribut          | Type    | Attribut von     | Optional | Beschreibung                                   |
|-------------------|---------|------------------|----------|------------------------------------------------|
| partitionKey (PK) | String  | Article, Catalog | nein     | Partition key. Primärschlüssel (PK)            |
| sortKey      (PK) | String  | Article, Catalog | nein     | Sort Key. Primärschlüssel (PK)                 |
| name              | String  | Article, Catalog | nein     | Artikel- oder Katalogname. Freitext.           |
| description       | String  | Article, Catalog | ja       | Freitext.                                      |
| price             | Number  | Article          | nein     |                                                |
| imageUrl          | String  | Article          | ja       |                                                |
| status            | String  | Article          | nein     | Werte: "active", "inactive"                    |
| isFeatured        | Boolean | Article          | nein     |                                                |
| dynamicFields     | Map     | Article          | ja       | Benutzerdefinierte Attribute.                  |
| createdAt         | String  | Article, Catalog | nein     | ISO 8601 Timestamp                             |
| updatedAt         | String  | Article, Catalog | nein     | ISO 8601 Timestamp                             |
| color             | String  | Catalog          | ja       | Farbe für ein Katalog (Catalog). Hexadezimal.  |
| inventory         | Number  | Article          | nein     | Insgesamt auf Lager.                           |
| sku               | String  | Article          | nein     | Stock Keeping Unit.                            |
| state             | String  | Article          | nein     | Verfügberkeit und/oder Zustand eines Artikels. |

**Single-Table-Design**
Singe-Table Design spart Kosten, vereinfacht Rechteverwaltung und erlaubt es, zusammengehörige Daten mit einem einzigen
Request zu laden.

**Primäschlüssel:**
Primärschlüssel (PK) wird aus `partitionKey` und `sortKey` zusammengestzt. Um Atrikel von Kategorien zu unterscheiden,
wir `partitionKey` von Artikel "ARTKL" enthalten und `partitionKey` von Kategorie "CAT". `partitionKey` kann weitere
Attribute enthalten, um Suche und Filterung zu ermöglichen. Attribute werden mit "#" von einander getrennt. Beispiel für
`partitionKey` von einem Artikel: "ARTCL#SKU-001", wo "SKU-001" SKU von Artekel ist. Das macht die Suche nach einem
Artikel mit SKU "SKU-001" möglich ohne gesammte Tabelle zu Scannen.

### Global Secondary Indexes (GSI).

**GSI_SKU:**
Vorerst aufgehoben: Wird in der aktuellen Version nicht benötigt.

**GSI_NameLookup (Namensuche):**
Um die Suche nach Produktnamen zu ermöglichen, ohne dabei die gesamte Tabelle zu Scannen, wird ein Global Secondary
Index auf Felder "name" (partitionKey) und "sku" (sortKey) gesetzt.

**Suche mit GSI_NameLookup**.

- Artikel nach Namen zu suchen: Mit Query, wo `partitionKey`=name und `sortKey` bleibt leer.
- Artikel nach Namen suchen, aber Suchergebnise mit sku abgrenzen: Mit Query, wo `partitionKey`=name und `sortKey`=sku
  ist.

---

### Beispieltabelle mit fiktiven Daten.

| partitionKey      | sortKey                          | color   | createdAt            | description   | imageUrl            | inv. | isFeatured | name         | price | SKU     | state     | updatedAt  |
|-------------------|----------------------------------|---------|----------------------|---------------|---------------------|------|------------|--------------|-------|---------|-----------|------------|
| CATALOGS          | CATALOGS#CAT-125#METADATA        | #fade55 | 2026-02-05T14:30:00Z |               |                     |      |            | phones       |       |         |           | 2026-02-01 |
| CATALOGS          | CATALOGS#CAT-123#METADATA        | #fcdsss | 2026-02-05T14:30:01Z | DELL Laptops. |                     |      |            | Laptops      |       |         |           | 2026-02-01 |
| ARTICLES          | ARTICLES#SKU-120#<UUID>          |         | 2026-02-05T14:30:00Z |               | http://s3.img01.png | 1    | true       | iPhone 15    | 0     | SKU-120 | IN_USE    |            |
| ARTICLES          | ARTICLES#SKU-120#<UUID>          |         | 2026-02-05T14:31:00Z |               | http://s3.img01.png | 1    | true       | iPhone 15    | 0     | SKU-120 | AVAILABLE |            |
| ARTICLES          | ARTICLES#SKU-030#<UUID>          |         | 2026-02-05T14:30:01Z |               | http://s3.img03.png | 1    | false      | Samsung S24  | 0     | SKU-030 | IN_USE    | 2026-02-07 |
| ARTICLES          | ARTICLES#SKU-123#<UUID>          |         | 2026-02-06T14:31:01Z | Gebraucht.    | http://s3.img04.png | 1    | false      | IPhone 15 SE | 0     | SKU-123 | AVAILABLE | 2026-02-07 |
| CATALOGS#ARTICLES | CATALOGS#CAT-125#ARTICLES#<UUID> |         | 2026-02-05T14:30:00Z |               |                     |      |            |              |       |         |           |            |
| CATALOGS#ARTICLES | CATALOGS#CAT-125#ARTICLES#<UUID> |         | 2026-02-05T14:30:01Z |               |                     |      |            |              |       |         |           |            |

---

### Suchen.

Einzelne Items werden nach PK gesucht und einzelne (bestimmte) Artikel können zusätzlich noch nach deren SKU gesucht
werden.

- [x] alle Artikel finden: Mit Query, wo `partitionKey`=ARTICLES und `sortKey`=nichts (keiner).
- [x] alle Artikel mit einem bestimmten SKU finden: Mit Query, wo `partitionKey`=ARTICLES und `sortKey`= Begins with "
  ARTICLES#SKU-125".
- [x] alle Artikel in Kategorie (Catalog) finden: Mit Query, wo `partitionKey`=CATALOGS#ARTICLES und `sortKey`= Begins
  with "CATALOGS#CAT-125".
- [x] alle Kategorien (Catalogs) finden bzw. auflisten: Mit Query, wo `partitionKey`=CATALOGS und `sortKey`=nichts (
  keiner).

---

### Wertebereiche für `state`

| Wert        | Bedeutung                                                                                                  |
|-------------|------------------------------------------------------------------------------------------------------------|
| AVAILABLE   | Der Artikel (Inventar) ist für die Anwendung bereit.                                                       |
| IN_USE      | Der Artikel (Inventar) wird gerade genutzt, im Einsatz oder in Ausleihe.                                   |
| RESERVED    | Der Artikel (Inventar) ist für einen bestimmten Zwekck reserviert, wurde aber noch nicht entnommen.        |
| MAINTENANCE | Wartung, Reparatur, Qualitätsprüfung, Bestandasaufnahme (Buchhaltung).                                     |
| RETIRED     | Der Artikel (Inventar) wird aus dem aktiven Bestand ausgeschrieben (verloren, defekt, verkauft, veraltet). |
