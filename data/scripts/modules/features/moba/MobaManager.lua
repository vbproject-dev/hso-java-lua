local Service = require("core.JavaClass").Service
local Config = require("modules.features.moba.MobaConfig")
local State = require("modules.features.moba.MobaState")
local MobaSession = require("modules.features.moba.MobaSession")

local MobaManager = {
    queue = ArrayList.new(),
    sessions = ArrayList.new(),
    nextId = 1,
}

function MobaManager.getEntry(session)
    return MobaManager.queue:findFirst(function(entry)
        return entry.session.p.objectId == session.p.objectId
    end)
end

function MobaManager.getSession(session)
    return MobaManager.sessions:findFirst(function(moba)
        return moba:has(session)
    end)
end

function MobaManager.getFight(session)
    local moba = MobaManager.getSession(session)
    return moba and moba.state == State.FIGHTING and moba or nil
end

function MobaManager.join(session, modeId)
    local mode = Config.MODES[modeId]
    if not mode then return false end

    if MobaManager.getEntry(session) or MobaManager.getSession(session) then
        Service.notice(session, "Kamu sudah berada di MOBA")
        return false
    end

    MobaManager.queue:add({ session = session, mode = modeId, expiresAt = os.time() + Config.QUEUE_TIMEOUT })
    Service.countdown(session, "Menunggu pemain " .. mode.NAME, Config.QUEUE_TIMEOUT)

    local entries = MobaManager.queue:filter(function(entry) return entry.mode == modeId end)
    local size = mode.SIZE * 2
    if entries:size() < size then
        entries:forEach(function(entry)
            Service.notice(entry.session, string.format("Pemain: %d/%d", entries:size(), size))
        end)
        return true
    end

    -- First half to team 1, second half to team 2
    local teams, i = { {}, {} }, 0
    entries:forEach(function(entry)
        i = i + 1
        if i > size then return end

        MobaManager.queue:remove(entry)
        table.insert(teams[i <= mode.SIZE and 1 or 2], entry.session)
    end)

    local moba = MobaSession.new(MobaManager.nextId, mode, teams)
    MobaManager.nextId = moba.id + 1
    MobaManager.sessions:add(moba)
    moba:enterMap()
    return true
end

function MobaManager.leave(session)
    local entry = MobaManager.getEntry(session)
    if entry then
        MobaManager.queue:remove(entry)
        Service.countdown(session, "", 0)
        Service.notice(session, "Antrian dibatalkan")
        return true
    end

    local moba = MobaManager.getSession(session)
    if not moba then return false end

    if moba.state ~= State.FIGHTING then
        MobaManager.finish(moba)
        return true
    end

    Service.countdown(session, "", 0)
    local left, team = moba:remove(session)
    if left == 0 then
        MobaManager.finish(moba, 3 - team)
    else
        moba:notice(string.format("%s keluar dari pertandingan", session.p.name))
    end
    return true
end

function MobaManager.finish(moba, winner)
    MobaManager.sessions:remove(moba)
    moba:countdown("", 0)
    -- TODO: teleport everyone home, give rewards (p.kills / p.deaths / p.assists)

    moba:each(function(p)
        Service.notice(p.session, not winner and "Pertandingan selesai tanpa pemenang"
            or p.team == winner and "Tim kamu menang" or "Tim kamu kalah")
    end)
end

-- Hooks -----------------------------------------------------------------------

function MobaManager.onEnter(session)
    local moba = MobaManager.getSession(session)
    if moba and moba.state == State.WAITING then moba:enter(session) end
end

function MobaManager.attack(attacker, structureId, damage)
    local moba = MobaManager.getFight(attacker)
    local s = moba and moba:attack(attacker, structureId, damage)
    if not s then return false end

    if s.lane == 0 and s.hp == 0 then MobaManager.finish(moba, 3 - s.team) end
    return true
end

function MobaManager.onKill(killer, victim, assists)
    local moba = MobaManager.getFight(killer)
    if moba then moba:kill(killer, victim, assists) end
end

function MobaManager.onLordKill(killer)
    local moba = MobaManager.getFight(killer)
    if moba then moba:lordKill(killer) end
end

function MobaManager.update()
    local now = os.time()

    MobaManager.queue:filter(function(entry)
        return entry.expiresAt <= now
    end):forEach(function(entry)
        MobaManager.queue:remove(entry)
        Service.countdown(entry.session, "", 0)
        Service.notice(entry.session, "Pemain tidak cukup")
    end)


    local ended = {}
    MobaManager.sessions:forEach(function(moba)
        local over, winner = moba:update(now)
        if over then table.insert(ended, { moba, winner }) end
    end)

    for _, e in ipairs(ended) do MobaManager.finish(e[1], e[2]) end
end

return MobaManager
