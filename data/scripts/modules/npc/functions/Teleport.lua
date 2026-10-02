--[[
--Filename: Teleport.lua
--Lokasi  : data/scripts/modules/npc/functions/Teleport.lua
--
--Menu teleport case -10. Lua hanya membangun menu; perpindahan map
--diserahkan ke MenuController.Menu_DaDichChuyen10 (Java), jadi cek blokir item,
--cek akun aktif, dan koordinat tetap satu sumber di Java.
--]]
local Java = require "core.JavaClass"
local Menu       = require "modules.menu.Menu"
local MenuHelper = require "modules.menu.MenuHelper"
local Service    = require("core.JavaClass").Service

-- Urutan HARUS sama dengan switch(index) di Menu_DaDichChuyen10 (index mulai dari 0)
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

local function buildMenu(npcId)
    local entries = {}

    for i, name in ipairs(DESTINATIONS) do
        entries[#entries + 1] = {
            name = name,
            action = function(session)
                Java.callStatic("core.MenuController", "Menu_DaDichChuyen10", session, i - 1)
            end,
        }
    end

    return MenuHelper.build(Menu, "Teleport", npcId, entries)
end

return {
    onTalk = function(session, npcId)
        local menu = buildMenu(npcId)

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}