--[[
--
--Filename: PanelApi.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local PanelController = require("web.features.panel.PanelController")

local PanelApi = class("PanelApi")

function PanelApi:ctor(manager)
    self.controller = PanelController.new(manager)
end

function PanelApi:get(request)
    if request.path == "/api/panel/status" then
        return self.controller:status()
    end
end

function PanelApi:post(request)
    if request.path == "/api/panel/start" then
        return self.controller:startServer()
    elseif request.path == "/api/panel/stop" then
        return self.controller:stopServer()
    elseif request.path == "/api/panel/reload" then
        return self.controller:reloadModules()
    end
end

return PanelApi
