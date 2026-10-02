local Menu = require "modules.menu.Menu"
local Service = require("core.JavaClass").Service
local MenuHelper = require("modules.menu.MenuHelper")
local Input = require("modules.menu.Input")

--[[
--
--Filename: Lisa.lua
--
--Created Date: Friday, October 2nd 2026, 9:33:59 pm
--
--Author: VIBE
--
--]]

return {
    onTalk = function(session, npcId)
        local menu = MenuHelper.build(Menu, "Zoro", npcId, {
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
