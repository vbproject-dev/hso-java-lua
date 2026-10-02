--[[
--
--Filename: Config.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]

local Config = {}

function Config.load(path)
    path = path or "config.json"

    if not File.exists(path) then
        log("[Config] File doesn't exists: %s", path)
        return nil
    end

    local content = File.read(path)
    local data = JSON.toTable(content)

    if not data then
        log("[Config] Failed to parse config file: %s", path)
        return nil
    end

    return data
end

return Config
