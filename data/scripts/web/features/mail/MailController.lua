--[[
--
--Filename: MailController.lua
--
--Created Date: Monday, October 5th 2026, 8:24:57 am
--
--Author: VIBE
--
--]]


local MailManager    = require("modules.features.mail.MailManager")
local Mail           = require("modules.features.mail.Mail")

local MailController = {}

local API_KEY        = "@vbproject2026"

function MailController:create(request)
    if request.headers["X-API-Key"] ~= API_KEY then
        return {
            success = false,
            error = "Unauthorized"
        }
    end

    local data = JSON.toTable(request.body)

    local mail, err = MailManager.send({
        player_id = data.player_id,
        sender = data.sender or "System",
        message = data.message or "",
        gold = data.gold or 0,
        gem = data.gem or 0,
        items = data.items or {},
        type = data.type or Mail.TYPE.SYSTEM
    })

    if not mail then
        return {
            success = false,
            error = err
        }
    end

    return {
        success = true,
        id = mail.id
    }
end

return MailController
