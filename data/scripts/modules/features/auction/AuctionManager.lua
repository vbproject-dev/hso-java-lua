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

local MAX_ITEM       = 10

local AuctionManager = {}

function AuctionManager.getItems(playerId)
    return GameData.auction:filter(function(data)
        return data.player_id == playerId
    end)
end

function AuctionManager.registerItem(session, item, price)
    local items = AuctionManager.getItems(session.p.objectId)
    if items:size() > MAX_ITEM then
        Service.notice(session, "Kamu telah mencapai batas maksimal")
        return false
    end

    local options = {}
    item.op:forEach(function(opt)
        table.insert(options, { id = opt.id, value = opt.param })
    end)

    -- Create auction data
    local itemData = {
        player_id = session.p.objectId,
        item_id = item.id,
        item_category = 3,
        price = price,
        status = 0,
        created_at = os.date("%Y-%m-%d %H:%M:%S"),
        item_info = {
            tier = item.tier,
            tierStar = item.tierStar,
            options = options,
        }
    }

    GameData.auction:add(itemData)

    local dbData = Utils.copyTable(itemData)
    dbData.item_info = JSON.fromTable(itemData.item_info)

    -- Insert to auction table
    local ok, err = insertTable("auction", dbData)
    if not ok then
        log("[AuctionManager] error: %s", err)
        return false
    end

    return true
end

return AuctionManager
