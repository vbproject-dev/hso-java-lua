## Item API

**GET** `/api/item`

### Query Parameters

| Parameter | Required | Description |
|---|---|---|
| `page` | No | Page number (default: `1`) |
| `limit` | No | Items per page (default: `20`, maximum: `100`) |
| `category` | Yes | Item category: `3` = Equipment, `4` = Potion, `7` = Material |

### Request Example

```http
GET /api/item?page=1&limit=1&category=3
```

### Category 3 — Equipment

```json
{
  "items": [
    {
      "clazz": 0,
      "color": 0,
      "data": [[0, 54], [40, 120]],
      "iconid": 0,
      "id": 0,
      "level": 1,
      "name": "Pedang batu",
      "part": 0,
      "type": 8
    }
  ],
  "pagination": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "limit": 1,
    "page": 1
  }
}
```

### Category 4 — Potion

```http
GET /api/item?page=1&limit=1&category=4
```

```json
{
  "items": [
    {
      "canTrade": 1,
      "content": "Memulihkan 100.000 HP",
      "icon": 1,
      "id": 0,
      "moneytype": 0,
      "name": "HP Kecil",
      "price": 10000,
      "sell": 1,
      "typepotion": 0,
      "value": 100000
    }
  ],
  "pagination": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "limit": 1,
    "page": 1
  }
}
```

### Category 7 — Material

```http
GET /api/item?page=1&limit=1&category=7
```

```json
{
  "items": [
    {
      "content": "Bahan upgrade perlengkapan",
      "id": 0,
      "imgid": 0,
      "name": "Besi tua",
      "price": 10000,
      "pricetype": 0,
      "sell": 0,
      "setcolorname": 0,
      "trade": 0,
      "type": 12,
      "value": 0
    }
  ],
  "pagination": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "limit": 1,
    "page": 1
  }
}
```

### Pagination Fields

| Field | Type | Description |
|---|---|---|
| `page` | Integer | Current page |
| `limit` | Integer | Maximum number of items per page |
| `hasNextPage` | Boolean | Indicates whether another page may exist |
| `hasPreviousPage` | Boolean | Indicates whether a previous page exists |

### Error Response

```json
{
  "error": "Invalid item category"
}
```