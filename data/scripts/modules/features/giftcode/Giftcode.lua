local GiftCode = class("GiftCode")

function GiftCode:ctor(data)
    self.id = data.id
    self.code = data.code
    self.description = data.description
    self.limit = data.limit or 1
    self.used = data.used or 0
    self.expired = data.expired or os.date("%Y-%m-%d %H:%M:%S", os.time() + 86400)
    self.gold = data.gold or 0
    self.gem = data.gem or 0
    self.items = data.items or {}
end

function GiftCode:isExpired()
    return os.time() >= (os.time({
        year = tonumber(self.expired:sub(1, 4)),
        month = tonumber(self.expired:sub(6, 7)),
        day = tonumber(self.expired:sub(9, 10)),
        hour = tonumber(self.expired:sub(12, 13)),
        min = tonumber(self.expired:sub(15, 16)),
        sec = tonumber(self.expired:sub(18, 19))
    }) or 0)
end

function GiftCode:toTable()
    return {
        id = self.id,
        code = self.code,
        description = self.description,
        limit = self.limit,
        used = self.used,
        expired = self.expired,
        gold = self.gold,
        gem = self.gem,
        items = self.items
    }
end

return GiftCode
