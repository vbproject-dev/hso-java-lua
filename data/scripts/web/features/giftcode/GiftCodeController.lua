local GiftCodeManager = require("modules.features.giftcode.GiftCodeManager")
local GiftCodeController = {}

local VALID_CATEGORIES = { [3] = true, [4] = true, [7] = true }

local function isWhole(n, min)
    return type(n) == "number" and n == math.floor(n) and n >= min
end

-- Returns a clean gift code table, or nil plus an error message.
local function validate(data, code)
    if type(data) ~= "table" then
        return nil, "Isi permintaan tidak valid"
    end

    local clean = {
        code = code or data.code,
        description = tostring(data.description or ""),
        expired = data.expired,
        gem = data.gem or 0,
        gold = data.gold or 0,
        limit = data.limit or 0,
        items = {}
    }

    if type(clean.code) ~= "string" or not clean.code:match("^[%w_%-]+$") then
        return nil, "Kode hanya boleh berisi huruf, angka, tanda hubung, dan garis bawah"
    end
    if type(clean.expired) ~= "string" or not clean.expired:match("^%d%d%d%d%-%d%d%-%d%d %d%d:%d%d:%d%d$") then
        return nil, "Format tanggal berakhir harus YYYY-MM-DD HH:MM:SS"
    end
    if not isWhole(clean.gem, 0) or not isWhole(clean.gold, 0) or not isWhole(clean.limit, 0) then
        return nil, "Gem, gold, dan batas harus bilangan bulat, 0 atau lebih"
    end

    for _, item in ipairs(data.items or {}) do
        if not VALID_CATEGORIES[item.category] or not isWhole(item.id, 0) or not isWhole(item.quantity, 1) then
            return nil, "Setiap item harus punya kategori dan id yang valid serta jumlah minimal 1"
        end
        local entry = {
            category = item.category,
            id = item.id,
            quantity = item.quantity
        }

        -- Only equipment (category 3) has a duration, in hours. 0 = permanent.
        if item.category == 3 then
            local duration = item.duration or 0
            if not isWhole(duration, 0) or duration > 87600 then
                return nil, "Durasi perlengkapan harus bilangan bulat jam antara 0 dan 87600 (0 = permanen)"
            end
            entry.duration = duration
        end

        clean.items[#clean.items + 1] = entry
    end

    return clean
end

-- request.body can be a raw JSON string or an already-decoded table.
local function parseBody(body)
    if type(body) == "table" then return body end
    if type(body) ~= "string" or body == "" then return nil end
    local ok, data = pcall(JSON.toTable, body) -- native decoder: string -> Lua table
    return ok and type(data) == "table" and data or nil
end

function GiftCodeController.list()
    return GiftCodeManager.giftcodes:toTable(function(item)
        return item:toTable()
    end)
end

function GiftCodeController.create(body)
    local clean, err = validate(parseBody(body))
    if not clean then return { error = err } end

    if GiftCodeManager.getByCode(clean.code) then
        return { error = "Kode hadiah sudah ada" }
    end

    local giftcode, createErr = GiftCodeManager.register(clean)
    if not giftcode then return { error = createErr } end

    return { success = true }
end

function GiftCodeController.update(body)
    local data = parseBody(body)
    local id = data and tonumber(data.id)
    if not id then return { error = "Id kode hadiah wajib diisi" } end

    local giftcode = GiftCodeManager.getById(id)
    if not giftcode then
        return { error = "Kode hadiah tidak ditemukan" }
    end

    local clean, err = validate(data)
    if not clean then return { error = err } end

    local other = GiftCodeManager.getByCode(clean.code)
    if other and other ~= giftcode then
        return { error = "Kode ini sudah dipakai kode hadiah lain" }
    end

    local ok, updateErr = GiftCodeManager.update(giftcode, clean)
    if not ok then return { error = updateErr } end

    return { success = true }
end

function GiftCodeController.delete(body)
    local data = parseBody(body)
    local id = data and tonumber(data.id)
    if not id then return { error = "Id kode hadiah wajib diisi" } end

    local giftcode = GiftCodeManager.getById(id)
    if not giftcode then
        return { error = "Kode hadiah tidak ditemukan" }
    end

    local ok, err = GiftCodeManager.remove(id)
    if not ok then return { error = err } end

    return { success = true }
end

return GiftCodeController
