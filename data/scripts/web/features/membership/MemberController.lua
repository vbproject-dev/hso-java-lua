--[[
--
--Filename: MemberController.lua
--
--Created Date: Tuesday, October 6th 2026, 10:11:32 am
--
--Author: VIBE
--
--]]


local MemberManager    = require "modules.features.membership.MemberManager"
local Member           = require "modules.features.membership.Member"

local API_KEY          = "@vbproject2026"

local MemberController = {}

function MemberController:create(request)
    if request.headers["X-API-Key"] ~= API_KEY then
        return {
            success = false,
            message = "Unauthorized"
        }
    end

    local data = JSON.toTable(request.body)

    if not data.name or not data.type or not data.days then
        return {
            success = false,
            message = "Data tidak valid. Field name, type, dan days dibutuhkan.\n" ..
                "Contoh: {\"name\":\"PlayerName\",\"type\":\"PREMIUM\",\"days\":30}"
        }
    end

    if data.type ~= Member.TYPE.REGULER and data.type ~= Member.TYPE.PREMIUM and data.type ~= Member.TYPE.VIP then
        return {
            success = false,
            message = "Tipe membership tidak valid."
        }
    end


    local player, err = findTable("player", { name = data.name })
    if not player then
        return {
            success = false,
            message = "Player tidak ditemukan."
        }
    end

    log("name: " .. player.id .. " type " .. data.type .. " days " .. data.days)

    if not MemberManager.add(player.id, data.type, data.days) then
        return {
            success = false,
            message = "Pendaftaran membership tidak berhasil."
        }
    end

    return {
        success = true,
        message = "Pendaftaran membership berhasil."
    }
end

return MemberController
