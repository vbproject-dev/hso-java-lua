--[[
--
--Filename: HandlerRegistry.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]

local HandlerRegistry = {
    handlers = {}
}

function HandlerRegistry.load(module)
    for command, handler in pairs(module) do
        if type(handler) ~= "function" then
            error("[HandlerRegistry] Invalid handler: " .. tostring(command))
        end
        HandlerRegistry.handlers[command] = handler
    end
end

function HandlerRegistry.loadAll(rows)
    HandlerRegistry.clear()
    for _, row in ipairs(rows) do
        HandlerRegistry.load(require(row.module))
    end
end

function HandlerRegistry.get(command)
    return HandlerRegistry.handlers[command]
end

function HandlerRegistry.has(command)
    return HandlerRegistry.handlers[command] ~= nil
end

function HandlerRegistry.clear()
    HandlerRegistry.handlers = {}
end

function HandlerRegistry.reload(rows)
    local previous = HandlerRegistry.handlers

    local success, err = xpcall(function()
        for _, row in ipairs(rows) do
            package.loaded[row.module] = nil
        end
        HandlerRegistry.loadAll(rows)
    end, debug.traceback)

    if not success then
        HandlerRegistry.handlers = previous
        log("[HandlerRegistry] Reload failed:\n%s", err)
        return false
    end

    log("[HandlerRegistry] Handlers reloaded")
    return true
end

return HandlerRegistry
