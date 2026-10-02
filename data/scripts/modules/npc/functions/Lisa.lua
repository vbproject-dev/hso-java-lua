local Menu = require "modules.menu.Menu"
local Service = require("core.JavaClass").Service
local MenuHelper = require("modules.menu.MenuHelper")

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
                name = "Test",
                action = function()
                    Service.notice(session, "OI")
                end
            },

            {
                name = "Submenu1",
                children = {
                    {
                        name = "Child 1",
                        action = function()

                        end
                    },
                    {
                        name = "Child 2",
                        action = function()

                        end
                    }
                }
            },

            {
                name = "Submenu2",
                children = {
                    {
                        name = "Child 1",
                        action = function()

                        end
                    },
                    {
                        name = "Child 2",
                        action = function()

                        end
                    }
                }
            },

        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
