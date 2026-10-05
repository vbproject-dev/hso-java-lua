--[[
--
--Filename: Mail.lua
--
--Created Date: Saturday, October 3rd 2026, 10:25:05 pm
--
--Author: VIBE
--
--]]

local Mail = class("Mail")

Mail.TYPE = {
    SYSTEM = "SYSTEM",
    COMPENSATION = "COMPENSATION",
    AUCTION = "AUCTION",
    EVENT = "EVENT",
    GM = "GM",
    TOPUP = "TOPUP"
}

Mail.STATUS = {
    UNREAD = "UNREAD",
    READ = "READ",
    CLAIMED = "CLAIMED"
}

function Mail:ctor(data)
    self.id = data.id or -1
    self.playerId = data.player_id
    self.sender = data.sender or "Inbox"
    self.message = data.message or ""
    self.gold = data.gold or 0
    self.gem = data.gem or 0
    self.items = type(data.items) == "table" and data.items or JSON.toTable(data.items or "[]")
    self.type = data.type or Mail.TYPE.SYSTEM
    self.status = data.status or Mail.STATUS.UNREAD
    self.createdAt = data.created_at or os.date("%Y-%m-%d %H:%M:%S")
    self.expiresAt = data.expires_at or os.date("%Y-%m-%d %H:%M:%S", os.time() + (7 * 86400))
end

function Mail:hasItems()
    return #self.items > 0
end

function Mail:hasGold()
    return self.gold > 0
end

function Mail:hasGem()
    return self.gem > 0
end

function Mail:toDatabase()
    return {
        player_id = self.playerId,
        sender = self.sender,
        message = self.message,
        gold = self.gold,
        gem = self.gem,
        items = JSON.fromTable(self.items),
        type = self.type,
        status = self.status,
        created_at = self.createdAt,
        expires_at = self.expiresAt
    }
end

return Mail
