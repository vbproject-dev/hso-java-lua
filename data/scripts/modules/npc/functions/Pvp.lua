local Service    = require("core.JavaClass").Service
local MenuHelper = require("modules.menu.MenuHelper")
local PvpManager = require("modules.features.pvp.PvpManager")


return {
    onTalk = function(session, npcId)
        local manager = Java.callStatic("game.event.GameEventManager", "gI")
        local moba = manager:getEventName("MOBA");

        local entry = PvpManager.getEntry(session)
        local menu = MenuHelper.build("PVP", npcId, {
            MenuHelper.when(moba:isOpen(),
                {
                    name = "Register",
                    action = function()
                        moba:register(session.p)
                    end
                }),
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

        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
