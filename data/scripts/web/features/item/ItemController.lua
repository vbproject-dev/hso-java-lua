local ItemController = {}

local ITEM_TABLES = {
    [3] = "equipment",
    [4] = "item4",
    [7] = "item7",
}

function ItemController.find(query)
    local category = tonumber(query.category)
    local tableName = ITEM_TABLES[category]
    if not tableName then
        return { error = "Invalid item category" }
    end

    local ids, seen = {}, {}
    for part in tostring(query.ids or query.id or ""):gmatch("[^,]+") do
        local id = tonumber(part)
        if id and not seen[id] then
            seen[id] = true
            ids[#ids + 1] = id
        end
        if #ids >= 50 then break end -- hard cap per request
    end

    if #ids == 0 then
        return { error = "ids is required" }
    end

    local items = {}
    for _, id in ipairs(ids) do
        local row, err = findTable(tableName, { id = id })

        if not row then -- findTable returns false, err when the query fails
            return { error = tostring(err) }
        end


        items[#items + 1] = row
    end

    return { items = items }
end

function ItemController.list(page, limit, category)
    local tableName = ITEM_TABLES[category]
    if not tableName then
        return { error = "Invalid item category" }
    end

    local list, err = loadTable(tableName, nil, page, limit, "id", "DESC")

    if not list then
        return {
            error = err
        }
    end

    local items = list:toTable()
    return {
        items = items,
        pagination = {
            page = page,
            limit = limit,
            hasNextPage = #items == limit,
            hasPreviousPage = page > 1,
        },
    }
end

return ItemController
