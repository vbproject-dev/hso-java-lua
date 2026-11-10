local Service = require("core.JavaClass").Service
local PvpState = require("modules.features.pvp.PvpState")


local PvpSession = class("PvpSession")

function PvpSession:ctor(id, a, b, timeout)
    self.id = id
    self.a = a
    self.b = b
    self.entered = {}
    self.state = PvpState.WAITING
    self.expiresAt = os.time() + timeout
end

function PvpSession:has(session)
    return self.a.p.objectId == session.p.objectId or self.b.p.objectId == session.p.objectId
end

function PvpSession:opponent(session)
    return self.a.p.objectId == session.p.objectId and self.b or self.a
end

function PvpSession:isFighting()
    return self.state == PvpState.FIGHTING
end

function PvpSession:isExpired(now)
    return self.state ~= PvpState.PREPARING and self.expiresAt <= now
end

function PvpSession:isPrepareDone(now)
    return self.state == PvpState.PREPARING and self.expiresAt <= now
end

function PvpSession:notice(message)
    Service.notice(self.a, message)
    Service.notice(self.b, message)
end

function PvpSession:countdown(text, count)
    Service.countdown(self.a, text, count)
    Service.countdown(self.b, text, count)
end

function PvpSession:onEnter(session, prepareTime)
    self.entered[session.p.objectId] = true
    if not (self.entered[self.a.p.objectId] and self.entered[self.b.p.objectId]) then return end

    self.state = PvpState.PREPARING
    self.expiresAt = os.time() + prepareTime
    self:countdown("Persiapan", prepareTime)
end

function PvpSession:startFight(fightTimeout)
    self.state = PvpState.FIGHTING
    self.expiresAt = os.time() + fightTimeout

    Service.setFlag(self.a, 1)
    Service.setFlag(self.b, 2)
    self:notice("Pertarungan dimulai")
    self:countdown("Waktu pertarungan", fightTimeout)
end

function PvpSession:enterMap()
    Service.goMap(self.a, 102, 228, 228)
    Service.goMap(self.b, 102, 492, 228)
end

function PvpSession:goHome()
    Service.setFlag(self.a, -1)
    Service.setFlag(self.b, -1)

    Service.goMap(self.a, 1, 432, 354)
    Service.goMap(self.b, 1, 432, 432)
end

return PvpSession
