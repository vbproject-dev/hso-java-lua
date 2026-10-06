--[[
--
--Filename: WebServer.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local FileApi = require("web.features.files.FileApi")
local MailApi = require("web.features.mail.MailApi")
local MemberApi = require("web.features.membership.MemberApi")

local WebServer = class("WebServer")

function WebServer:ctor()
    self.server = HttpServer.new()
    self.features = {
        FileApi,
        MailApi,
        MemberApi
    }
end

function WebServer:init()
    local cfg = Java.callStatic("core.Manager", "gI")
    self.server:setMaxBodySize(1000000000)
    self.server:setHandler({
        onGet = function(request)
            return self:handle("get", request)
        end,

        onPost = function(request)
            return self:handle("post", request)
        end,

        onPut = function(request)
            return self:handle("put", request)
        end,

        onDelete = function(request)
            return self:handle("delete", request)
        end
    })

    self.server:start(cfg.web_port)

    return true
end

function WebServer:handle(method, request)
    for _, feature in ipairs(self.features) do
        local handler = feature[method]

        if handler then
            local response = handler(feature, request)

            if response then
                return response
            end
        end
    end
end

function WebServer:pollEvents()
    self.server:pollEvents()
end

function WebServer:stop()
    self.server:stop()
end

return WebServer
