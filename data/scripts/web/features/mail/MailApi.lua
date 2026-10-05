--[[
--
--Filename: MailApi.lua
--
--Created Date: Monday, October 5th 2026, 8:24:45 am
--
--Author: VIBE
--
--]]



local MailController = require("web.features.mail.MailController")

local MailApi = {}

function MailApi:post(request)
    if request.path ~= "/api/mail" then
        return
    end

    return MailController:create(request)
end

return MailApi
