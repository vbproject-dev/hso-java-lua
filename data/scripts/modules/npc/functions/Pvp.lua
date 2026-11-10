local Service    = require("core.JavaClass").Service
local MenuHelper = require("modules.menu.MenuHelper")
local PvpManager = require("modules.features.pvp.PvpManager")
local PvpState   = require("modules.features.pvp.PvpState")

return {
    onTalk = function(session, npcId)
        local entry = PvpManager.getEntry(session)

        local menu = MenuHelper.build("PVP", npcId, {

            MenuHelper.when(not entry,
                {
                    name = "1VS1",
                    action = function()
                        PvpManager.join(session)
                    end
                }),

            MenuHelper.when(entry, {
                name = "Cancel Match",
                action = function()
                    PvpManager.leave(session)
                end
            })
            ,
            {
                name = "Moba",
                action = function()
                    PvpManager.join(session)
                end
            }
        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
