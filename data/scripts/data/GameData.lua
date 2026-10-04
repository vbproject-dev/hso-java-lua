local AuctionManager = require "modules.features.auction.AuctionManager"
local MailManager    = require "modules.features.mail.MailManager"
local GameData       = {
    npcData = ArrayList.new(),

}

function GameData.loadData()
    local datasets = {
        { table = "npc", field = "npcData" },

    }

    for _, dataset in ipairs(datasets) do
        local result, err = loadTable(dataset.table)

        if not result then
            log("Failed to load %s: %s", dataset.table, err)
            return false
        end

        GameData[dataset.field] = result
    end

    AuctionManager.load()
    MailManager.load()

    return true
end

function GameData.getNpc(id)
    return GameData.npcData:findFirst(function(npc)
        return npc.id == id
    end)
end

return GameData
