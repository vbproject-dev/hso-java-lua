--[[
--
--Filename: Logger.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]



_G.DEBUG = false


_G.log = function(format, ...)
    if not DEBUG then return end

    local msg = string.format(format, ...)

    print("[LUA] " .. msg)
end

_G.printTable = function(t, indent, done)
    indent = indent or 0
    done = done or {}

    -- Avoid infinite recursion for circular references
    if done[t] then
        print(string.rep("  ", indent) .. "*circular reference*")
        return
    end

    done[t] = true

    -- Handle non-table values
    if type(t) ~= "table" then
        print(string.rep("  ", indent) .. tostring(t))
        return
    end

    -- Print table
    print(string.rep("  ", indent) .. "{")

    for k, v in pairs(t) do
        local keyStr
        if type(k) == "string" then
            keyStr = '"' .. k .. '"'
        else
            keyStr = tostring(k)
        end

        io.write(string.rep("  ", indent + 1) .. "[" .. keyStr .. "] = ")

        -- Recursively print value
        if type(v) == "table" then
            printTable(v, indent + 2, done)
        else
            print(tostring(v))
        end
    end

    print(string.rep("  ", indent) .. "}")
end
