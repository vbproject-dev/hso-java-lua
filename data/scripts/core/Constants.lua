--[[
--
--Filename: Constants.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]

local MySQL     = require "core.MySQL"
local JavaClass = require "core.JavaClass"

_G.loadTable    = function(tableName, where)
    local db = MySQL.instance()
    local query = db:from(tableName)

    if where then
        local field, value = next(where)
        if type(value) == "table" then
            query = query:where(field, value[1], value[2])
        else
            query = query:where(field, value)
        end
    end

    local data, err = query:getAll()

    if err then
        return false, err
    end

    local list = ArrayList.new(data)

    log("[Table] %d rows Loaded from %s", list:size(), tableName)

    return list
end

_G.updateTable  = function(tableName, data, where)
    local field, value = next(where)
    local db = MySQL.instance()

    local result, err = db:from(tableName):where(field, value):update(data)

    if err then
        return false, err
    end

    return true
end

_G.findTable    = function(tableName, where)
    local field, value = next(where)
    local db = MySQL.instance()
    local data, err = db:from(tableName):where(field, value):getFirst()

    if err then
        return false, err
    end

    return data
end

_G.insertTable  = function(tableName, data)
    local db = MySQL.instance()
    local result, err = db:from(tableName):insert(data)

    if err then
        return false, err
    end

    return result
end

_G.deleteTable  = function(tableName, where)
    local field, value = next(where)
    local db = MySQL.instance()

    local result, err = db:from(tableName):where(field, value):delete()

    if err then
        return false, err
    end

    return result
end

_G.try          = function(func, ...)
    local success, err = xpcall(func, debug.traceback, ...)

    if not success then
        local lines = {}
        local traceback = tostring(err)

        for line in traceback:gmatch("[^\n]+") do
            if not line:match("^stack traceback:$")
                and not line:match("^%s*%[C%]:")
                and not line:match("^%s*%(%.%.%.tail calls%.%.%.%)")
                and not line:match("base%.xpcall")
                and not line:match("base%.try") then
                table.insert(lines, line)
            end
        end

        log("[LUA ERROR]\n%s", table.concat(lines, "\n"))
        return false
    end

    return err
end

_G.CLASS        = JavaClass
