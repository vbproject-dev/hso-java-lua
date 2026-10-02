--[[
--
--Filename: NpcRegistry.lua
--
--Created Date: Friday, October 2nd 2026, 3:11:54 am
--
--Author: VBPROJECT
--
--]]

local NpcRegistry = {}
local scripts = {}

function NpcRegistry.register(id, script)
    scripts[id] = script
end

function NpcRegistry.get(id)
    return scripts[id]
end

function NpcRegistry.clear()
    scripts = {}
end

return NpcRegistry
