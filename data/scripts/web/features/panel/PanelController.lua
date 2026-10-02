--[[
--
--Filename: PanelController.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local PanelController = class("PanelController")

function PanelController:ctor(manager)
    self.manager = manager
end

function PanelController:status()
    return {
        success = true,
        data = {
            running = self.manager:isRunning()
        }
    }
end

function PanelController:startServer()
    self.manager:startServer()
    return {
        success = true,
        message = "Server starting..."
    }
end

function PanelController:stopServer()
    self.manager:stopServer()
    return {
        success = true,
        message = "Server stopping..."
    }
end

function PanelController:reloadModules()
    local ModuleRegistry = require "core.ModuleRegistry"
    if not ModuleRegistry.reload() then
        return {
            success = false,
            message = "Failed to reload modules"
        }
    end

    return {
        success = true,
        message = "Modules reloaded"
    }
end

return PanelController
