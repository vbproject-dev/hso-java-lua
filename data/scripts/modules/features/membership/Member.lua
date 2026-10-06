--[[
--
--Filename: Member.lua
--
--Created Date: Tuesday, October 6th 2026, 9:11:27 am
--
--Author: VIBE
--
--]]


local Member = class("Member")
Member.TYPE = {
    REGULER = "REGULER",
    PREMIUM = "PREMIUM",
    VIP = "VIP",
}
function Member:ctor(data)
    self.playerId = data.player_id
    self.type = data.type
    self.createdAt = data.created_at
    self.expiredAt = data.expired_at
end

function Member:isExpired()
    local expiredAt = os.time({
        year = tonumber(self.expiredAt:sub(1, 4)),
        month = tonumber(self.expiredAt:sub(6, 7)),
        day = tonumber(self.expiredAt:sub(9, 10)),
        hour = tonumber(self.expiredAt:sub(12, 13)),
        min = tonumber(self.expiredAt:sub(15, 16)),
        sec = tonumber(self.expiredAt:sub(18, 19))
    })
    return os.time() >= expiredAt
end

function Member:isActive()
    return not self:isExpired()
end

return Member
