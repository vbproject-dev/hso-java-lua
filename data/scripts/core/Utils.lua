--[[
--
--Filename: Utils.lua
--
--Created Date: Saturday, October 3rd 2026, 5:42:57 am
--
--Author: VIBE
--
--]]

local Utils = {}

function Utils.copyTable(value)
    if type(value) ~= "table" then
        return value
    end

    local copy = {}

    for key, item in pairs(value) do
        copy[Utils.copyTable(key)] = Utils.copyTable(item)
    end

    return copy
end

return Utils
