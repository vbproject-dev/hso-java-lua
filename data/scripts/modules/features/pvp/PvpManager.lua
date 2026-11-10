local Service = require("core.JavaClass").Service
local PvpState = require("modules.features.pvp.PvpState")
local PvpSession = require("modules.features.pvp.PvpSession")

local PvpManager = {
    queue = ArrayList.new(),
    sessions = ArrayList.new(),
    nextId = 1,

    -- Configurations
    MAX_LEVEL = 300,
    RANGE = 100,
    QUEUE_TIMEOUT = 30,
    PREPARE_TIMEOUT = 10,
    FIGHT_TIMEOUT = 300,
}

function PvpManager.getRange(level)
    return math.ceil(level / PvpManager.RANGE)
end

function PvpManager.getEntry(session)
    return PvpManager.queue:findFirst(function(entry)
        return entry.session.p.objectId == session.p.objectId
    end)
end

function PvpManager.getSession(session)
    return PvpManager.sessions:findFirst(function(pvp)
        return pvp:has(session)
    end)
end

function PvpManager.join(session)
    if session.p.level > PvpManager.MAX_LEVEL then
        Service.notice(session, "Level kamu tidak dapat mengikuti PvP")
        return false
    end

    if PvpManager.getEntry(session) or PvpManager.getSession(session) then
        Service.notice(session, "Kamu sudah berada di PvP")
        return false
    end

    local range = PvpManager.getRange(session.p.level)
    local opponent = PvpManager.queue:findFirst(function(entry)
        return entry.range == range
    end)

    if not opponent then
        PvpManager.queue:add({
            session = session,
            range = range,
            expiresAt = os.time() + PvpManager.QUEUE_TIMEOUT
        })
        Service.countdown(session, "Menunggu lawan", PvpManager.QUEUE_TIMEOUT)
        return true
    end

    PvpManager.queue:remove(opponent)

    local pvp = PvpSession.new(PvpManager.nextId, opponent.session, session, PvpManager.PREPARE_TIMEOUT)
    PvpManager.nextId = pvp.id + 1
    PvpManager.sessions:add(pvp)

    -- Enter boith to PVP MAP
    pvp:enterMap()
    return true
end

-- Player entering pvp map
function PvpManager.onEnter(session)
    local pvp = PvpManager.getSession(session)
    if not pvp or pvp.state ~= PvpState.WAITING then
        return
    end

    pvp:onEnter(session, PvpManager.PREPARE_TIMEOUT)
end

function PvpManager.finish(pvp, winner)
    if winner then
        local winPlayer = winner.p
        local losePlayer = pvp:opponent(winner).p

        losePlayer.isdie = false
        losePlayer.hp = losePlayer.body:getMaxHP()
        losePlayer.mp = losePlayer.body:getMaxMP()

        Service.worldChat(string.format("(PVP) %s telah dibunuh oleh %s", losePlayer.name, winPlayer.name))
    end


    pvp:goHome()

    PvpManager.sessions:remove(pvp)
end

function PvpManager.onKilled(session)
    local pvp = PvpManager.getSession(session)
    if not pvp or not pvp:isFighting() then return end

    PvpManager.finish(pvp, pvp:opponent(session))
end

function PvpManager.leave(session)
    local entry = PvpManager.getEntry(session)
    if entry then
        PvpManager.queue:remove(entry)
        Service.countdown(session, "", 0)
        Service.notice(session, "Antrian PvP dibatalkan")
        return true
    end

    local pvp = PvpManager.getSession(session)
    if not pvp then return false end

    PvpManager.finish(pvp, pvp:isFighting() and pvp:opponent(session) or nil)
    return true
end

function PvpManager.update(dt)
    local now = os.time()

    PvpManager.queue:filter(function(entry)
        return entry.expiresAt <= now
    end):forEach(function(entry)
        PvpManager.queue:remove(entry)
        Service.countdown(entry.session, "", 0)
        Service.notice(entry.session, "Lawan tidak ditemukan")
    end)

    -- Prepare finished: start the fight
    PvpManager.sessions:filter(function(pvp)
        return pvp:isPrepareDone(now)
    end):forEach(function(pvp)
        pvp:startFight(PvpManager.FIGHT_TIMEOUT)
    end)

    -- Enter / fight timeout
    PvpManager.sessions:filter(function(pvp)
        return pvp:isExpired(now)
    end):forEach(function(pvp)
        PvpManager.finish(pvp, nil)
    end)
end

return PvpManager
