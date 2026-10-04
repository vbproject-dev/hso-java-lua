--[[
--
--Filename: LuaBridge.lua
--
--Created Date: Friday, October 2nd 2026, 3:27:01 am
--
--Author: VBPROJECT
--
--]]

local Cmd             = require "core.Cmd"
local NpcRegistry     = require "modules.npc.NpcRegistry"
local HandlerRegistry = require "modules.handlers.HandlerRegistry"
local Service         = require("core.JavaClass").Service

local LuaBridge       = {}

function LuaBridge.onMessage(session, msg)
    local handler = HandlerRegistry.get(msg.cmd)

    if not handler then
        log("[Network] Unhandled Command %s from %s", Cmd.getName(msg.cmd), session.ip)
        return false
    end

    return try(function()
        return handler(session, msg)
    end)
end

function LuaBridge.onTalk(session, npcId)
    return try(function()
        local script = NpcRegistry.get(npcId)

        if not script then
            return false
        end
        session.state:put("auction", false)
        return script.onTalk(session, npcId)
    end)
end

function LuaBridge.onInput(session, msg)
    return try(function()
        local input = session.state:get("input")

        if not input then
            return false
        end

        local reader = msg:copyReader()

        local npcId = reader:readShort()
        local menuId = reader:readShort()
        local size = reader:readByte()

        if input.npcId ~= npcId or size ~= #input.fields then
            session.state:remove("input")
            return false
        end

        local values = {}

        for _, field in ipairs(input.fields) do
            local value = reader:readUTF()

            if field.type == InputType.NUMERIC then
                value = tonumber(value)

                if not value then
                    session.state:remove("input")
                    return false
                end
            end

            table.insert(values, value)
        end

        session.state:remove("input")
        input.action(session, values)

        return true
    end)
end

function LuaBridge.onSelectMenu(session, npcId, menuId, index)
    return try(function()
        local menu = session.state:get("menu")

        if not menu then
            return false
        end

        if menu.npcId ~= npcId then
            session.state:remove("menu")
            return false
        end


        local selected = menu:get(index)

        if not selected then
            return false
        end

        if selected:size() > 0 then
            Service.openMenu(session, selected)
            session.state:put("menu", selected)
            return true
        end

        session.state:remove("menu")
        selected:perform(session)

        return true
    end)
end

return LuaBridge
