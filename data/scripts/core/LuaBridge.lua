--[[
--
--Filename: LuaBridge.lua
--
--Created Date: Friday, October 2nd 2026, 3:27:01 am
--
--Author: VBPROJECT
--
--]]


local NpcRegistry     = require "modules.npc.NpcRegistry"
local HandlerRegistry = require "modules.handlers.HandlerRegistry"
local Service         = require("core.JavaClass").Service


local MAX_PACKETS_PER_SECOND = 10
local rateLimits             = setmetatable({}, { __mode = "k" })
local blockedIPs             = {}
local LuaBridge              = {}

local function isRateLimited(session, cmd)
    local now = os.time()

    local sessionLimits = rateLimits[session]
    if not sessionLimits then
        sessionLimits = {}
        rateLimits[session] = sessionLimits
    end

    local limit = sessionLimits[cmd]

    if not limit or limit.second ~= now then
        sessionLimits[cmd] = {

            second = now,
            count = 1
        }
        return false
    end

    if limit.count >= MAX_PACKETS_PER_SECOND then
        return true
    end

    limit.count = limit.count + 1
    return false
end


function LuaBridge.onMessage(session, msg)
    -- Reject previously blocked IPs.
    if blockedIPs[session.ip] then
        session:close()
        return true
    end

    -- Block the IP if it exceeds the limit.
    if isRateLimited(session, msg.cmd) then
        blockedIPs[session.ip] = true
        session:close()

        log("[SPAM PACKET DETECTED] Blocked IP: %s, command: %s", session.ip, msg.cmd)
        return true
    end

    local handler = HandlerRegistry.get(msg.cmd)

    if not handler then
        return false
    end

    return try(function()
        return handler(session, msg)
    end)
end

function LuaBridge.onTalk(session, npcId)
    return try(function()
        session.state:clear()
        local script = NpcRegistry.get(npcId)

        if not script then
            return false
        end

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

-- SESSION HOOK
function LuaBridge.onConnected(session)
    --log("%s CONNECTED TO SERVER", session.ip)
end

function LuaBridge.onDisconnected(session)
    --log("%s DISCONNECTED FROM SERVER", session.p and session.p.name or session.ip)
end

function LuaBridge.onDie(mainObject)

end

return LuaBridge
