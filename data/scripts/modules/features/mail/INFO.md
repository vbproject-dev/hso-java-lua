# MailManager Usage

## Send Mail

```lua
local MailManager = require("modules.features.mail.MailManager")

MailManager.send({
    player_id = 762,
    sender = "System",
    message = "Congratulations!",
    gold = 10000,
    gem = 100,
    items = {}
})
```

## Send Mail With Item

```lua
MailManager.send({
    player_id = 762,
    sender = "System",
    message = "Congratulations!",
    items = {
        {
            item_id = 1001,
            category = 3,
            quantity = 1
        }
    }
})
```

## Mail API

**POST** `/api/mail`

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


