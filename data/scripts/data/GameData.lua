local GameData = {
    npcData = ArrayList.new(),
    auctions = ArrayList.new(),
}

function GameData.loadData()
    local datasets = {
        { table = "npc",     field = "npcData" },
        { table = "auction", field = "auctions" }
    }

    for _, dataset in ipairs(datasets) do
        local result, err = loadTable(dataset.table)

        if not result then
            log("Failed to load %s: %s", dataset.table, err)
            return false
        end

        GameData[dataset.field] = result
    end

    return true
end

function GameData.getNpc(id)
    return GameData.npcData:findFirst(function(npc)
        return npc.id == id
    end)
end

return GameData
