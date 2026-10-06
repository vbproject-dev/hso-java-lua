--[[
--
--Filename: AuctionManager.lua
--
--Created Date: Saturday, October 3rd 2026, 3:43:49 am
--
--Author: VIBE
--
--]]

local Service       = require("core.JavaClass").Service
local AuctionItem   = require("modules.features.auction.AuctionItem")
local Cmd           = require("core.Cmd")
local MemberManager = require("modules.features.membership.MemberManager")


local MailManager = require("modules.features.mail.MailManager")
local Mail        = require("modules.features.mail.Mail")





local AuctionManager  = {
    auctionItems = ArrayList.new(),

    -- Configurations
    MAX_ITEM = 5,
    TAX = 4,
    BLACKLIST = {},
    MIN_LEVEL = 250,
    REGISTER_TAX = 1,
}

AuctionManager.STATUS = {
    ONSALE = "ONSALE",
    SOLD = "SOLD",
    EXPIRED = "EXPIRED",
    CANCEL = "CANCEL",
}

function AuctionManager.load(cfg)
    -- Load AuctionItem
    local result, err = loadTable("auction")
    if result then
        result:forEach(function(data)
            local auction = AuctionItem.new(data)

            if auction.status == AuctionManager.STATUS.ONSALE and auction:isExpired() then
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

    -- Overwrite Configurations
    if cfg then
        AuctionManager.TAX = cfg.tax
        AuctionManager.MAX_ITEM = cfg.maxitem
        AuctionManager.MIN_LEVEL = (cfg.minlevel or AuctionManager.MIN_LEVEL)
        AuctionManager.REGISTER_TAX = (cfg.registertax or AuctionManager.REGISTER_TAX)
        for _, id in ipairs(cfg.blacklist or {}) do
            AuctionManager.BLACKLIST[id] = true
        end
    end
end

function AuctionManager.isAllowed(id)
    if AuctionManager.BLACKLIST[id] then
        return false
    end

    return true
end

function AuctionManager.getItems(playerId)
    return AuctionManager.auctionItems:filter(function(data)
        return data.playerId == playerId
    end)
end

function AuctionManager.hasSlot(session)
    return AuctionManager.auctionItems:filter(function(item)
        return item.playerId == session.p.objectId and item.status == 0
    end):size() < AuctionManager.MAX_ITEM
end

function AuctionManager.hasItemOnSale(session)
    return AuctionManager.auctionItems:filter(function(item)
        return item.playerId == session.p.objectId and item.status == AuctionManager.STATUS.ONSALE
    end):size() > 0
end

function AuctionManager.openAuction(session)
    local items = AuctionManager.auctionItems:filter(function(data)
        return data.status == AuctionManager.STATUS.ONSALE
    end)

    local packet = Java.new("client.io.Message", Cmd.NPC_INFO)
    packet:writer():writeUTF("Black Market")
    packet:writer():writeByte(1)
    packet:writer():writeShort(items:size())
    items:forEach(function(auction)
        local item = auction.itemObject
        packet:writer():writeShort(auction.id)
        packet:writer():writeUTF(string.format("%s +%d", item.name, auction.itemInfo.tier))
        packet:writer():writeByte(item.clazz)
        packet:writer():writeByte(item.type)
        packet:writer():writeShort(item.icon)
        packet:writer():writeLong(auction.price)
        packet:writer():writeShort(item.level)
        packet:writer():writeByte(item.color)
        packet:writer():writeByte(item.op:size())
        item.op:forEach(function(opt)
            packet:writer():writeByte(opt:getId())
            packet:writer():writeInt(opt:getParam(auction.itemInfo.tier))
        end)
        packet:writer():writeByte(1)
    end)

    session:addmsg(packet)
    session.state:put("auction", true)
end

function AuctionManager.registerItem(session, item, price, quantity)
    local options = {}
    item.op:forEach(function(opt)
        table.insert(options, { id = opt.id, value = opt.param })
    end)

    -- Check membership
    if not MemberManager.has(session.p.objectId) then
        -- Bukan membership potong pajak pendaftaran
        local tax = math.floor(price * AuctionManager.REGISTER_TAX / 100)
        if session.p:getGem() < tax then
            Service.notice(session, "Permata tidak cukup untuk biaya pendaftaran")
            return false
        end

        session.p:updateGem(-tax)
    end

    -- Create auction
    local itemData = AuctionItem.new({
        player_id = session.p.objectId,
        item_id = item.id,
        item_name = item.name,
        item_category = 3,
        price = price,
        quantity = quantity or 1,
        days = 7,
        status = AuctionManager.STATUS.ONSALE,
        created_at = os.date("%Y-%m-%d %H:%M:%S"),
        item_info = {
            tier = item.tier,
            tierStar = item.tierStar,
            color = item.color,
            options = options,
        }
    })


    -- Insert to auction table
    local id, err = insertTable("auction", itemData:toDatabase())
    if not id then
        log("[AuctionManager] error: %s", err)
        return false
    end

    itemData.id = id
    AuctionManager.auctionItems:add(itemData)
    log("Add item to black market" .. itemData.id)
    return true
end

function AuctionManager.cancel(session, auctionId)
    local auction = AuctionManager.auctionItems:findFirst(function(item)
        return item.id == auctionId
            and item.playerId == session.p.objectId
            and item.status == AuctionManager.STATUS.ONSALE
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
                quantity = auction.quantity,
                name = auction.itemName,
                category = auction.category,
                tier = auction.itemInfo.tier,
                tierStar = auction.itemInfo.tierStar,
                options = auction.itemInfo.options
            }
        },
        type = Mail.TYPE.AUCTION
    })

    auction.status = AuctionManager.STATUS.CANCEL

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
            and item.status == AuctionManager.STATUS.ONSALE
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
                    quantity = auction.quantity,
                    name = auction.itemName,
                    category = auction.category,
                    tier = auction.itemInfo.tier,
                    tierStar = auction.itemInfo.tierStar,
                    options = auction.itemInfo.options
                }
            },
            type = Mail.TYPE.AUCTION
        })

        auction.status = AuctionManager.STATUS.CANCEL

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
    local auction = AuctionManager.auctionItems:findFirst(function(auction)
        return auction.id == auctionId and auction.status == AuctionManager.STATUS.ONSALE
    end)


    if not auction then
        Service.notice(session, "Item tidak ditemukan")
        return
    end

    if auction.playerId == session.p.objectId then
        Service.notice(session, "Kamu tidak dapat membeli item sendiri")
        return
    end


    local price = auction.price

    if session.p:getGem() < price then
        Service.notice(session, "Permata tidak cukup")
        return
    end

    session.p:updateGem(-price)

    -- Give item to buyer

    session.p.item:add_item_bag3(auction.itemObject)
    session.p.item:updateBag()

    -- Potong pajak jika pemilik item bukan membership
    local isMembersip = MemberManager.has(auction.playerId)

    local tax = isMembersip and math.floor(price * AuctionManager.TAX / 100) or 0
    local received = price - tax

    MailManager.send({
        player_id = auction.playerId,
        sender = "Black Market",
        message = string.format("Item %s telah terjual.", auction.itemName),
        gem = received,
        type = Mail.TYPE.AUCTION
    })

    auction.status = AuctionManager.STATUS.SOLD

    local ok, err = deleteTable("auction", { id = auctionId })

    if not ok then
        log("[AuctionManager] Failed to delete auction %d: %s", auction.id, err)
        return
    end

    AuctionManager.auctionItems:remove(auction)

    AuctionManager.openAuction(session)

    Service.notice(session, "Pembelian berhasil")
end

return AuctionManager
