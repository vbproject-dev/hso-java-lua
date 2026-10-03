--[[
--
--Filename: AuctionItem.lua
--
--Created Date: Saturday, October 3rd 2026, 6:16:22 am
--
--Author: VIBE
--
--]]

local AuctionItem = class("AuctionItem")

function AuctionItem:ctor(data)
    self.playerId = data.player_id
    self.itemId = data.item_id
    self.itemName = data.item_name
    self.price = data.price
    self.category = data.item_category or 3
    self.price = data.price or 0
    self.status = data.status or 0
    self.createdAt = data.created_at
    self.itemInfo = data.item_info or {}
    self.days = data.days or 7

    -- Java Object
    local eq = Java.callStatic("template.Item3", "fromTemplate", self.itemId)
    if eq then
        eq.tier = self.itemInfo.tier
        eq.tierStar = self.itemInfo.tierStar
        eq.op:clear()
        for __, opt in ipairs(self.itemInfo.options) do
            eq.op:add(Java.new("template.Option", opt.id, opt.value))
        end

        self.itemObject = eq
    end
end

function AuctionItem:toDatabase()
    return {
        player_id = self.playerId,
        item_id = self.itemId,
        item_name = self.itemName,
        item_category = self.category,
        price = self.price,
        days = self.days,
        item_info = JSON.fromTable(self.itemInfo),
        status = self.status,
        created_at = self.createdAt
    }
end

return AuctionItem
