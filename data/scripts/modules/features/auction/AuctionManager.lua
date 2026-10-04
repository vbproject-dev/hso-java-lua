--[[
--
--Filename: AuctionManager.lua
--
--Created Date: Saturday, October 3rd 2026, 3:43:49 am
--
--Author: VIBE
--
--]]

local Service     = require("core.JavaClass").Service
local AuctionItem = require("modules.features.auction.AuctionItem")
local Cmd         = require("core.Cmd")

local MailManager = require("modules.features.mail.MailManager")
local Mail        = require("modules.features.mail.Mail")

local MAX_ITEM    = 10
local TAX         = 10


local STATUS = {
    ONSALE = 0,
    SOLD = 1,
    EXPIRED = 2,
    CANCEL = 3,
}

local AuctionManager = {
    auctionItems = ArrayList.new(),
}

function AuctionManager.load()
    -- Load AuctionItem
    local result, err = loadTable("auction")
    if result then
        result:forEach(function(data)
            local auction = AuctionItem.new(data)

            if auction.status == STATUS.ONSALE and auction:isExpired() then
                MailManager.send({
                    player_id = auction.playerId,
                    sender = "Black Market",
                    message = string.format("Item lelang %s telah kadaluarsa.", auction.itemName),
                    items = {
                        {
                            item_id = auction.itemId,
                            amount = 1,
                            name = auction.itemName,
                            category = auction.category,
                            tier = auction.itemInfo.tier,
                            tierStar = auction.itemInfo.tierStar,
                            options = auction.itemInfo.options
                        }
                    },
                    type = Mail.TYPE.AUCTION
                })

                -- Delete from auction
                local ok, err = deleteTable("auction", { id = auction.id })
                if not ok then
                    log("[AuctionManager] Failed to delete auction %d %s", auction.id, err)
                end
                return
            end

            AuctionManager.auctionItems:add(auction)
        end)
    else
        log("[AuctionManager] error: %s", err)
    end
end

function AuctionManager.getItems(playerId)
    return AuctionManager.auctionItems:filter(function(data)
        return data.playerId == playerId
    end)
end

function AuctionManager.hasSlot(session)
    return AuctionManager.auctionItems:filter(function(item)
        return item.playerId == session.p.objectId and item.status == 0
    end)
end

function AuctionManager.hasItemOnSale(session)
    return AuctionManager.auctionItems:filter(function(item)
        return item.playerId == session.p.objectId and item.status == STATUS.ONSALE
    end):size() > 0
end

function AuctionManager.openAuction(session)
    -- Only filter items that are on sale
    -- 0 = ON SALE
    -- 1 = SOLD OUT
    -- 2 = Expired
    -- 3 = CANCEL

    local items = AuctionManager.auctionItems:filter(function(data)
        return data.status == 0
    end)

    local packet = Java.new("client.io.Message", Cmd.NPC_INFO)
    packet:writer():writeUTF("Auction")
    packet:writer():writeByte(1)
    packet:writer():writeShort(items:size())
    items:forEach(function(auction)
        local item = auction.itemObject
        packet:writer():writeShort(auction.id)
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
    session.state:put("auction", true)
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
            color = item.color,
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

function AuctionManager.cancel(session, auctionId)
    local auction = AuctionManager.auctionItems:findFirst(function(item)
        return item.id == auctionId and item.playerId == session.p.objectId and item.status == STATUS.ONSALE
    end)

    if not auction then
        Service.notice(session, "Item lelang tidak ditemukan")
        return false
    end

    MailManager.send({
        player_id = session.p.objectId,
        sender = "Black Market",
        message = string.format("Item lelang %s telah dibatalkan.", auction.itemName),
        items = {
            {
                item_id = auction.itemId,
                amount = 1,
                name = auction.itemName,
                category = auction.category,
                tier = auction.itemInfo.tier,
                tierStar = auction.itemInfo.tierStar,
                options = auction.itemInfo.options
            }
        },
        type = Mail.TYPE.AUCTION
    })

    auction.status = STATUS.CANCEL

    local ok, err = deleteTable("auction", { id = auction.id })
    if not ok then
        log("[AuctionManager] Failed to delete auction %d: %s", auction.id, err)
        return false
    end

    AuctionManager.auctionItems:remove(auction)

    Service.notice(session, "Penjualan berhasil dibatalkan")
    return true
end

function AuctionManager.cancelAll(session)
    local auctions = AuctionManager.auctionItems:filter(function(item)
        return item.playerId == session.p.objectId
            and item.status == STATUS.ONSALE
    end)

    if auctions:size() == 0 then
        Service.notice(session, "Tidak ada item lelang")
        return false
    end

    auctions:forEach(function(auction)
        MailManager.send({
            player_id = session.p.objectId,
            sender = "Black Market",
            message = string.format("Item lelang %s telah dibatalkan.", auction.itemName),
            items = {
                {
                    item_id = auction.itemId,
                    amount = 1,
                    name = auction.itemName,
                    category = auction.category,
                    tier = auction.itemInfo.tier,
                    tierStar = auction.itemInfo.tierStar,
                    options = auction.itemInfo.options
                }
            },
            type = Mail.TYPE.AUCTION
        })

        auction.status = STATUS.CANCEL

        local ok, err = deleteTable("auction", { id = auction.id })
        if not ok then
            log("[AuctionManager] Failed to delete auction %d: %s", auction.id, err)
            return
        end

        AuctionManager.auctionItems:remove(auction)
    end)

    Service.notice(session, "Semua penjualan berhasil dibatalkan")
    return true
end

function AuctionManager.buy(session, auctionId)
    local auction = AuctionManager.auctionItems:findFirst(function(item)
        return item.id == auctionId
            and item.status == STATUS.ONSALE
    end)

    if not auction then
        Service.notice(session, "Item tidak ditemukan")
        return false
    end

    if auction.playerId == session.p.objectId then
        Service.notice(session, "Kamu tidak dapat membeli item sendiri")
        return false
    end


    local price = auction.price

    if session.p:getGem() < price then
        Service.notice(session, "Permata tidak cukup")
        return false
    end

    session.p:updateGem(-price)

    -- Give item to buyer

    session.p.item:add_item_bag3(auction.itemObject)
    session.p.item:updateBag()

    -- Send money to seller through mail
    local tax = math.floor(price * TAX / 100)
    local received = price - tax

    MailManager.send({
        player_id = auction.playerId,
        sender = "Black Market",
        message = string.format("Item %s telah terjual.", auction.itemName),
        gem = received,
        type = Mail.TYPE.AUCTION
    })

    auction.status = STATUS.SOLD

    local ok, err = deleteTable("auction", { id = auction.id })

    if not ok then
        log("[AuctionManager] Failed to delete auction %d: %s", auction.id, err)
        return false
    end

    AuctionManager.auctionItems:remove(auction)

    AuctionManager.openAuction(session)

    Service.notice(session, "Pembelian berhasil")
    return true
end

return AuctionManager
