local GameData = {
    npcData = ArrayList.new(),
    auction = ArrayList.new(),
}

function GameData.loadData()
    local datasets = {
        { table = "npc",     field = "npcData" },
        { table = "auction", field = "auction" }
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

function GameData.getAuctionByPlayerId(id)
    return GameData.auction:findFirst(function(item)
        return item.player_id == id
    end)
end

return GameData
