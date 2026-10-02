--[[
--
--Filename: Lisa.lua
--
--Created Date: Friday, October 2nd 2026, 9:33:59 pm
--
--Author: VIBE
--
--]]

local Service = require("core.JavaClass").Service
local MenuHelper = require("modules.menu.MenuHelper")


return {
    onTalk = function(session, npcId)
        local menu = MenuHelper.build("Zoro", npcId, {
            {
                name = "Black Market",
                action = function()

                end
            },
        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
