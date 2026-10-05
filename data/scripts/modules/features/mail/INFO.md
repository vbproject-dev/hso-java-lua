## Mail API

**POST** `/api/mail`

| Header      | Required | Description                                     |
| ----------- | -------- | ----------------------------------------------- |
| `X-API-Key` | Yes      | Secret API key used to authenticate the request |

### Request Body

```json
{
  "player_id": 762,
  "sender": "GM",
  "message": "Isi Pesan",
  "gold": 10000,
  "gem": 100,
  "items": [
    {"item_id": 10, "category": 3, "quantity":1, "color": 4, "tier": 0, "tierStar": 0, "options": [{"id": 4, "value": 100}]},
    {"item_id": 10, "category": 4, "quantity":200},
    {"item_id": 10, "category": 7, "quantity":200}
    ]
}
