--[[
--
--Filename: Main.lua
--
--Created Date: Wednesday, September 30th 2026, 11:29:14 pm
--
--Author: VBPROJECT
--
--]]

require "core.Class"
require "core.Constants"
require "core.Logger"
require "core.LuaBridge"
local WebServer = require "web.WebServer"



local MySQL           = require "core.MySQL"
local NpcRegistry     = require "modules.npc.NpcRegistry"
local HandlerRegistry = require "modules.handlers.HandlerRegistry"
local GameData        = require "data.GameData"




local Main                = {}
DEBUG                     = true
local WEB                 = nil

local MYSQL_ELAPSED       = 0
local MYSQL_PING_INTERVAL = 300

function Main.onLoad()
    -- Setup MYSQL
    local cfg = Java.callStatic("core.Manager", "gI")
    local ok, error = MySQL.connect(cfg.mysql_host, cfg.mysql_user, cfg.mysql_pass, cfg.mysql_database, cfg.mysql_port)
    if error then
        log("[MySQL] Connection fail %s", error)
        return
    end


    -- Load Database
    if not GameData.loadData() then
        log("[GameData] Failed to load game data")
        return
    end

    -- Register Npc Scripts
    GameData.npcData:forEach(function(npc)
        if npc.script_name then
            local script = require("modules.npc.functions." .. npc.script_name)
            NpcRegistry.register(npc.id, script)
        end
    end)


    -- Register Handlers
    HandlerRegistry.loadAll({
        { module = "modules.handlers.CommonHandler" }
    })

    -- Web Setup
    WEB = WebServer.new()
    WEB:init()
end

function Main.onUpdate(dt)
    MYSQL_ELAPSED = (MYSQL_ELAPSED + dt)

    if MYSQL_ELAPSED >= MYSQL_PING_INTERVAL then
        MySQL.instance():ping()
        MYSQL_ELAPSED = 0
    end

    if WEB then
        WEB:pollEvents()
    end
end

function Main.onDestroy()
    if WEB then
        WEB:stop()
    end

    MySQL.instance():close()
end

return Main
