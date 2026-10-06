local AuctionManager = require "modules.features.auction.AuctionManager"
local MailManager    = require "modules.features.mail.MailManager"
local MemberManager  = require "modules.features.membership.MemberManager"

local GameData       = {
    npcData = ArrayList.new(),
    configs = ArrayList.new(),
}

function GameData.loadData()
    local datasets = {
        { table = "npc",       field = "npcData" },
        { table = "sv_config", field = "configs" },

    }

    for _, dataset in ipairs(datasets) do
        local result, err = loadTable(dataset.table)

        if not result then
            log("Failed to load %s: %s", dataset.table, err)
            return false
        end

        GameData[dataset.field] = result
    end

    AuctionManager.load(GameData.getConfig().auction)
    MailManager.load()
    MemberManager.load()
    return true
end

function GameData.getNpc(id)
    return GameData.npcData:findFirst(function(npc)
        return npc.id == id
    end)
end

function GameData.getConfig()
    return GameData.configs:first()
end

return GameData
