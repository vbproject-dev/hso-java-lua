--[[
--
--Filename: AuctionManager.lua
--
--Created Date: Saturday, October 3rd 2026, 3:43:49 am
--
--Author: VIBE
--
--]]
local Service        = require("core.JavaClass").Service
local Utils          = require("core.Utils")
local GameData       = require("data.GameData")
local AuctionItem    = require("modules.features.auction.AuctionItem")
local Cmd            = require("core.Cmd")

local MAX_ITEM       = 10
local TAX            = 10

local AuctionManager = {
    auctionItems = ArrayList.new()
}

function AuctionManager.load()
    GameData.auctions:forEach(function(data)
        AuctionManager.auctionItems:add(AuctionItem.new(data))
    end)
end

function AuctionManager.getItems(playerId)
    return AuctionManager.auctionItems:filter(function(data)
        return data.player_id == playerId
    end)
end

function AuctionManager.hasSlot(session)
    return AuctionManager.getItems(session.p.objectId):size() < MAX_ITEM
end

function AuctionManager.openAuction(session)
    local items = AuctionManager.auctionItems
    local packet = Java.new("client.io.Message", Cmd.NPC_INFO)
    packet:writer():writeUTF("Auction")
    packet:writer():writeByte(1)
    packet:writer():writeShort(items:size())
    items:forEach(function(auction)
        local item = auction.itemObject
        packet:writer():writeShort(item.id)
        packet:writer():writeUTF(item.name)
        packet:writer():writeByte(item.clazz)
        packet:writer():writeByte(item.type)
        packet:writer():writeShort(item.icon)
        packet:writer():writeLong(auction.price)
        packet:writer():writeShort(item.level)
        packet:writer():writeByte(item.color)
        packet:writer():writeByte(item.op:size())
        item.op:forEach(function(opt)
            packet:writer():writeByte(opt:getId())
            packet:writer():writeInt(opt:getParam(item.tier))
        end)
        packet:writer():writeByte(1)
    end)

    session:addmsg(packet)
end

function AuctionManager.registerItem(session, item, price)
    local options = {}
    item.op:forEach(function(opt)
        table.insert(options, { id = opt.id, value = opt.param })
    end)

    -- Create auction
    local itemData = AuctionItem.new({
        player_id = session.p.objectId,
        item_id = item.id,
        item_name = item.name,
        item_category = 3,
        price = price,
        days = 7,
        status = 0,
        created_at = os.date("%Y-%m-%d %H:%M:%S"),
        item_info = {
            tier = item.tier,
            tierStar = item.tierStar,
            options = options,
        }
    })

    AuctionManager.auctionItems:add(itemData)

    -- Insert to auction table
    local ok, err = insertTable("auction", itemData:toDatabase())
    if not ok then
        log("[AuctionManager] error: %s", err)
        return false
    end

    return true
end

return AuctionManager
