--[[
--
--Filename: CommonHandler.lua
--
--Created Date: Thursday, October 1st 2026, 11:34:13 pm
--
--Author: VBPROJECT
--
--]]

local Service        = require("core.JavaClass").Service
local Cmd            = require("core.Cmd")
local MenuHelper     = require("modules.menu.MenuHelper")
local AuctionManager = require("modules.features.auction.AuctionManager")
local Input          = require("modules.menu.Input")



local function onDynamicMenu(session, packet)
    local reader = packet:reader()
    local npcId = reader:readShort()
    local menuId = reader:readByte()
    local index = reader:readByte()

    Service.notice(session, "Belum ada fitur untuk menu ini")
    return true
end


local function createOtherMenu(session, itemIndex, item)
    local menu = MenuHelper.build("Lainya", -1, {
        MenuHelper.when(item.islock, {
            name = "Unlock",
            action = function()
                item.islock = false
                session.p.updateGem(-20000)
                session.p.item:updateBag()
                Service.notice(session, "Item unlocked cost 20.000 permata")
            end
        }),

        {
            name = "Lelang",
            action = function()
                if not AuctionManager.hasSlot(session) then
                    Service.notice(session, "Slot lelang sudah penuh")
                    return
                end

                local input = Input.build({
                    npcId = -1,
                    title = "Pasang Harga",
                    fields = {
                        { name = "Jumlah", type = InputType.NUMERIC },
                    },
                    action = function(session, values)
                        local value = values[1]
                        if not value then
                            Service.notice(session, "Input tidak valid")
                            return
                        end

                        if AuctionManager.registerItem(session, item, value) then
                            session.p.item:remove(3, itemIndex, 1)
                            session.p.item:updateBag()
                            Service.notice(session, "Item berhasil di lelangkan")
                        end
                    end,
                })
                session.state:put("input", input)
                Service.openInput(session, input)
            end
        }

    })

    return menu
end

local function onMiniGame(session, packet)
    local reader = packet:copyReader()

    local size = reader:available()
    if size == 4 then
        local type = reader:readByte()
        local category = reader:readByte()
        local index = reader:readShort()

        if category == 3 then
            local item = session.p.item.bag3[index]

            if not item then return false end

            local menu = createOtherMenu(session, index, item)
            session.state:put("menu", menu)
            Service.openMenu(session, menu)
            return true
        end

        return false
    end

    return false
end

return {
    [Cmd.MINI_GAME] = onMiniGame
}
