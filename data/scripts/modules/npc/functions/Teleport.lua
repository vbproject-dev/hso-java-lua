--[[
--
--Filename: Teleport.lua
--
--Created Date: Friday, October 2nd 2026, 10:55:27 pm
--
--Author: VIBE
--
--]]



local Menu         = require "modules.menu.Menu"
local MenuHelper   = require "modules.menu.MenuHelper"
local Service      = require("core.JavaClass").Service

local DESTINATIONS = {
    "Desa as Putih",
    "Kota Harta Karun",
    "Mataram Kuno",
    "Area Perdagangan",
    "Gua Api",
    "Hutan Ilusi",
    "Lembah Misterius",
    "Danau Kenangan",
    "Pantai",
    "Jurang Batu",
    "Karang Tersembunyi",
    "Rawa",
    "Kuil Kuno",
    "Gua Kelelawar",
}

local function buildMenu(session, npcId)
    local entries = {}

    for index, name in ipairs(DESTINATIONS) do
        table.insert(entries, {
            name = name,
            action = function()
                Java.callStatic("core.MenuController", "Menu_DaDichChuyen10", session, index - 1)
            end
        })
    end

    return MenuHelper.build(Menu, "Teleport", npcId, entries)
end


return {
    onTalk = function(session, npcId)
        local menu = buildMenu(session, npcId)

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
