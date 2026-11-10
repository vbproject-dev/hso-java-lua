local GifCode         = require("modules.features.giftcode.Giftcode")
local Service         = require("core.JavaClass").Service

local GiftCodeManager = {
    giftcodes = ArrayList.new()
}

function GiftCodeManager.load()
    local list, err = loadTable("giftcode_v2")
    if not list then
        log("[GiftCodeManager Load] error: %s", err)
        return
    end

    list:forEach(function(data)
        GiftCodeManager.giftcodes:add(GifCode.new(data))
    end)
end

function GiftCodeManager.getByCode(code)
    return GiftCodeManager.giftcodes:findFirst(function(data)
        return data.code == code
    end)
end

function GiftCodeManager.getById(id)
    return GiftCodeManager.giftcodes:findFirst(function(data)
        return data.id == id
    end)
end

function GiftCodeManager.create(data)
    data = data or {}

    local code, err = GiftCodeManager.generateCode()
    if not code then
        return nil, err
    end

    data.code = code
    local giftcode = GifCode.new(data)
    GiftCodeManager.giftcodes:add(giftcode)

    return giftcode
end

function GiftCodeManager.register(data)
    local giftcode = GifCode.new(data)

    local id, err = insertTable("giftcode_v2", {
        code = giftcode.code,
        description = giftcode.description,
        limit = giftcode.limit,
        used = 0,
        expired = giftcode.expired,
        gold = giftcode.gold,
        gem = giftcode.gem,
        items = JSON.fromTable(giftcode.items)
    })

    if not id then
        log("[GiftCodeManager Register] Failed to save: %s", tostring(err))
        return nil, "Failed to save gift code"
    end

    local row = findTable("giftcode_v2", { code = giftcode.code })
    if not row or not row.id then
        log("[GiftCodeManager Register] Saved but could not read back id for %s", giftcode.code)
        return nil, "Saved, but could not load the new gift code. Refresh and check."
    end

    giftcode.id = row.id
    GiftCodeManager.giftcodes:add(giftcode)
    return giftcode
end

function GiftCodeManager.update(giftcode, data)
    local oldCode = giftcode.code

    local ok, err = updateTable("giftcode_v2", {
        code = data.code,
        description = data.description,
        limit = data.limit,
        expired = data.expired,
        gold = data.gold,
        gem = data.gem,
        items = JSON.fromTable(data.items)
    }, { id = giftcode.id })

    if not ok then
        log("[GiftCodeManager Update] Failed to update: %s", tostring(err))
        return false, "Failed to save changes"
    end

    -- Keep claim history attached if the code text was changed.
    if oldCode ~= data.code then
        updateTable("giftcode_v2_claim", { giftcode = data.code }, { giftcode = oldCode })
    end

    giftcode.code = data.code
    giftcode.description = data.description
    giftcode.limit = data.limit
    giftcode.expired = data.expired
    giftcode.gold = data.gold
    giftcode.gem = data.gem
    giftcode.items = data.items -- memory keeps the TABLE (claim() loops over it)
    return true
end

function GiftCodeManager.remove(id)
    local giftcode = GiftCodeManager.getById(id)
    if not giftcode then
        return false, "Gift code not found"
    end

    local ok, err = deleteTable("giftcode_v2", { id = id })
    if not ok then
        log("[GiftCodeManager Remove] Failed to delete id %s: %s", tostring(id), tostring(err))
        return false, "Failed to delete gift code"
    end

    -- Remove claim history too, so the same code can be created again later.
    deleteTable("giftcode_v2_claim", { giftcode = giftcode.code })

    GiftCodeManager.giftcodes = GiftCodeManager.giftcodes:filter(function(entry)
        return entry ~= giftcode
    end)
    return true
end

function GiftCodeManager.claim(session, code)
    local giftcode = GiftCodeManager.getByCode(code)
    if not giftcode then
        Service.notice(session, "Kode tidak tersedia")
        return
    end

    if giftcode:isExpired() then
        Service.notice(session, "Kode telah kadaluarsa")
        return
    end

    if giftcode.used >= giftcode.limit then
        Service.notice(session, "Kode telah mencapai batas maksimal")
        return
    end

    local playerId = session.p.objectId
    local history, err = findTable("giftcode_v2_claim", {
        player_id = playerId,
        giftcode = code
    })

    if err then
        log("[GiftCodeManager Claim] History query failed: %s", tostring(err))
        Service.notice(session, "Terjadi kesalahan, silakan coba lagi")
        return
    end

    if history then
        Service.notice(session, "Kamu sudah menggunakan kode ini")
        return
    end

    -- Validate rewards before saving the claim.
    for _, item in ipairs(giftcode.items) do
        if not item.category or not item.id then
            Service.notice(session, "Hadiah kode tidak valid")
            return
        end

        if item.category ~= 3 and item.category ~= 4 and item.category ~= 7 then
            Service.notice(session, "Hadiah kode tidak valid")
            return
        end

        if not item.quantity or item.quantity < 1 then
            Service.notice(session, "Jumlah hadiah tidak valid")
            return
        end
    end

    -- Save claim history first.
    local historyId, insertErr = insertTable("giftcode_v2_claim", {
        player_id = playerId,
        giftcode = code,
        claimed_at = os.date("%Y-%m-%d %H:%M:%S")
    })

    if not historyId then
        log("[GiftCodeManager Claim] Failed to save history: %s", tostring(insertErr))
        Service.notice(session, "Gagal menggunakan kode, silakan coba lagi")
        return
    end

    -- Update usage in the database.
    local newUsed = giftcode.used + 1
    local updated, updateErr = updateTable("giftcode_v2", { used = newUsed }, { id = giftcode.id })

    if not updated then
        deleteTable("giftcode_v2_claim", { id = historyId })
        log("[GiftCodeManager Claim] Failed to update usage: %s", tostring(updateErr))
        Service.notice(session, "Gagal menggunakan kode, silakan coba lagi")
        return
    end

    giftcode.used = newUsed

    -- Grant rewards only after database operations succeed.
    local itemShows = ArrayList.new()

    if giftcode.gold > 0 then
        session.p:updateGold(giftcode.gold)
        itemShows:add({
            id = -1,
            quantity = giftcode.gold,
            category = 4
        })
    end

    if giftcode.gem > 0 then
        session.p:updateGem(giftcode.gem)
        itemShows:add({
            id = -2,
            quantity = giftcode.gem,
            category = 4
        })
    end

    for _, item in ipairs(giftcode.items) do
        local itemObject

        if item.category == 3 then
            itemObject = Java.callStatic("template.Item3", "fromTemplate", item.id)
        elseif item.category == 4 then
            itemObject = Java.callStatic("template.Item47", "getPotion", item.id)
        elseif item.category == 7 then
            itemObject = Java.callStatic("template.Item47", "getMaterial", item.id)
        end

        if itemObject then
            if item.category == 3 then
                itemObject.expiry_date = item.duration and item.duration > 0 and
                (os.time() * 1000 + item.duration * 3600000) or 0
                session.p.item:add_item_bag3(itemObject)
            else
                session.p.item:add_item_bag47(item.id, item.quantity, item.category)
            end

            itemShows:add({
                id = itemObject.id,
                name = itemObject.name,
                icon = itemObject.icon or 0,
                color = itemObject.color or 0,
                quantity = item.quantity,
                category = item.category
            })
        end
    end

    session.p.item:updateBag()

    Service.showReward(session, {
        title = "GiftCode",
        message = "Hadiah telah berhasil diterima.",
        items = itemShows
    })
end

return GiftCodeManager
