local Service   = require("core.JavaClass").Service
local PvpState  = require("modules.features.pvp.PvpState")
local PvpConfig = require("modules.features.pvp.PvpConfig")


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

function PvpSession:isFinished()
    return self.state == PvpState.FINISHED
end

-- Waiting and fighting sessions can time out. Preparing has its own
-- check, and finished sessions have their own countdown.
function PvpSession:isExpired(now)
    return self.state ~= PvpState.PREPARING
        and self.state ~= PvpState.FINISHED
        and self.expiresAt <= now
end

function PvpSession:isFinishDone(now)
    return self.state == PvpState.FINISHED and self.expiresAt <= now
end

-- Fight is over: stop the fight and start the countdown before going home
function PvpSession:startFinish(homeDelay)
    self.state = PvpState.FINISHED
    self.expiresAt = os.time() + homeDelay

    Service.setFlag(self.a, -1)
    Service.setFlag(self.b, -1)
    self:countdown("Kembali ke kota", homeDelay)
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
    Service.goMap(self.a, PvpConfig.MAP_ID, 228, 228)
    Service.goMap(self.b, PvpConfig.MAP_ID, 492, 228)
end

function PvpSession:goHome()
    Service.countdown(self.a, "", 0)
    Service.countdown(self.b, "", 0)

    Service.goMap(self.a, 1, 432, 354)
    Service.goMap(self.b, 1, 432, 432)
end

return PvpSession
