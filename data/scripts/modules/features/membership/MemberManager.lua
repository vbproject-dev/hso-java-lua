local Member = require "modules.features.membership.Member"
local MailManager = require "modules.features.mail.MailManager"
--[[
--
--Filename: MemberManager.lua
--
--Created Date: Tuesday, October 6th 2026, 9:10:16 am
--
--Author: VIBE
--
--]]

local MemberManager = {
    members = ArrayList.new()
}

function MemberManager.load()
    local result, err = loadTable("membership")
    if not result then
        log("[MemberManager] load failed %s", err)
        return
    end

    result:forEach(function(data)
        local member = Member.new(data)

        if member:isExpired() then
            local ok, err = deleteTable("membership", { player_id = member.playerId })

            if not ok then
                log("[MemberManager] failed to delete expired membership %d: %s", member.playerId, err)
            end

            return
        end

        MemberManager.members:add()
    end)
end

function MemberManager.get(playerId)
    return MemberManager.members:findFirst(function(member)
        return member.playerId == playerId
    end)
end

function MemberManager.has(playerId)
    return MemberManager.members:anyMatch(function(member)
        return member.playerId == playerId
    end)
end

function MemberManager.add(playerId, memberType, days)
    local now = os.time()
    local member = MemberManager.get(playerId)
    if member and member:isActive() then
        local expiredAt = os.time({
            year = tonumber(member.expiredAt:sub(1, 4)),
            month = tonumber(member.expiredAt:sub(6, 7)),
            day = tonumber(member.expiredAt:sub(9, 10)),
            hour = tonumber(member.expiredAt:sub(12, 13)),
            min = tonumber(member.expiredAt:sub(15, 16)),
            sec = tonumber(member.expiredAt:sub(18, 19))
        })

        expiredAt = expiredAt + (days * 86400)
        member.expiredAt = os.date("%Y-%m-%d %H:%M:%S", expiredAt)
        local ok, err = updateTable("membership", { expired_at = member.expiredAt }, { player_id = playerId })
        if not ok then
            log("[MemberManager] failed to extend membership %d: %s", playerId, err)
            return false
        end
        return true
    end


    local createdAt = os.date("%Y-%m-%d %H:%M:%S", now)
    local expiredAt = os.date("%Y-%m-%d %H:%M:%S", now + (days * 86400))
    local data = { player_id = playerId, type = memberType, created_at = createdAt, expired_at = expiredAt }
    local id, err = insertTable("membership", data)
    if not id then
        log("[MemberManager] failed to add membership %d: %s", playerId, err)
        return false
    end
    MemberManager.members:add(Member.new(data))

    MailManager.send({
        player_id = playerId,
        sender = "System",
        message = string.format("Membership %s %d hari telah aktif", memberType, days),
        items = {}
    })
    return true
end

function MemberManager.remove(playerId)
    local member = MemberManager.get(playerId)
    if not member then return false end
    local ok, err = deleteTable("membership", { player_id = playerId })
    if not ok then
        log("[MemberManager] failed to remove membership %d: %s", playerId, err)
        return false
    end
    MemberManager.members:remove(member)
    return true
end

return MemberManager
