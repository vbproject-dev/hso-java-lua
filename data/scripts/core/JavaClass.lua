--[[
--
--Filename: JavaClass.lua
--
--Created Date: Thursday, October 1st 2026, 9:03:49 am
--
--Author: VBPROJECT
--
--]]

local Cmd = require "core.Cmd"

return {

    Service = {
        notice = function(session, text)
            Java.callStatic("core.Service", "send_notice_box", session, text)
        end,

        openMenu = function(session, menu)
            local packet = Java.new("client.io.Message", Cmd.DYNAMIC_MENU)

            packet:writer():writeShort(menu.npcId)
            packet:writer():writeByte(0)
            packet:writer():writeByte(menu.children:size())

            menu.children:forEach(function(item)
                packet:writer():writeUTF(item.name)
            end)

            packet:writer():writeUTF(menu.name)

            session:addmsg(packet)
        end,

        openInput = function(session, input)
            local m = Java.new("client.io.Message", Cmd.DIALOG_MORE_OPTION_SERVER)
            m:writer():writeShort(input.npcId)
            m:writer():writeByte(0)
            m:writer():writeUTF(input.title)
            m:writer():writeByte(#input.fields)
            for __, field in ipairs(input.fields) do
                m:writer():writeUTF(field.name)
                m:writer():writeByte(0)
            end
            for __, field in ipairs(input.fields) do
                m:writer():writeUTF("")
                m:writer():writeByte(0)
            end
            session:addmsg(m)
        end,

        goMap = function(session, mapId, x, y)
            local player = session.p
            local vgo = Java.callStatic("model.map.Vgo", "create", mapId, x, y)
            player:changeMap(player, vgo)
        end,

        openUI = function(session, id)
            Java.callStatic("core.Service", "send_box_UI", session, id)
        end,

        buatJubah = function(session, id)
            Java.callStatic("core.MenuController", "createCloak", session, id)
        end,

        buatTitle = function(session, id)
            Java.callStatic("core.MenuController", "createTitle", session, id)
        end,

        chat = function(session, from, msg)
            local packet = Java.new("client.io.Message", Cmd.CHAT_TAB)
            packet:writer():writeUTF(from)
            packet:writer():writeUTF(msg)
            session:addmsg(packet)
        end,


        showReward = function(session, data)
            local packet = Java.new("client.io.Message", 78)
            packet:writer():writeUTF(data.title)
            packet:writer():writeByte(data.items:size())

            data.items:forEach(function(item)
                local name, icon = item.name, item.icon
                if item.category == 4 then
                    if item.id == -1 then
                        name, icon = "Gold", 0
                    elseif item.id == -2 then
                        name, icon = "Permata", 246
                    end
                end

                packet:writer():writeUTF(name)
                packet:writer():writeShort(icon)
                packet:writer():writeInt(item.quantity)
                packet:writer():writeByte(item.category)
                packet:writer():writeByte(0)
                packet:writer():writeByte(item.category == 3 and item.color or 0)
            end)

            packet:writer():writeUTF(data.message)
            packet:writer():writeByte(1)
            packet:writer():writeByte(1)

            session:addmsg(packet)
        end
    }

}
