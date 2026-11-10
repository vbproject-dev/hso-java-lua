local Service = require("core.JavaClass").Service
local Config = require("modules.features.moba.MobaConfig")
local State = require("modules.features.moba.MobaState")
local MobaTeam = require("modules.features.moba.MobaTeam")

local MobaSession = class("MobaSession")

function MobaSession:ctor(id, mode, groups)
    self.id = id
    self.mode = mode
    self.state = State.WAITING
    self.expiresAt = os.time() + Config.ENTER_TIMEOUT
    self.teams = { MobaTeam.new(1, groups[1], mode), MobaTeam.new(2, groups[2], mode) }
    self.entered = {}
end

function MobaSession:each(fn)
    for _, team in ipairs(self.teams) do team:each(fn) end
end

function MobaSession:player(session)
    local id = session.p.objectId
    return self.teams[1].players[id] or self.teams[2].players[id]
end

function MobaSession:has(session)
    return self:player(session) ~= nil
end

function MobaSession:notice(message)
    self:each(function(p) Service.notice(p.session, message) end)
end

function MobaSession:countdown(label, seconds)
    self:each(function(p) Service.countdown(p.session, label, seconds) end)
end

function MobaSession:enterMap()
    self:notice("Pertandingan ditemukan")
    self:countdown("Menuju arena", Config.ENTER_TIMEOUT)
end

function MobaSession:enter(session)
    self.entered[session.p.objectId] = true

    local all = true
    self:each(function(p)
        all = all and self.entered[p.session.p.objectId]
    end)

    if not all then
        return
    end

    self.state = State.PREPARING
    self.expiresAt = os.time() + Config.PREPARE_TIME
    self:notice("Bersiap, pertarungan segera dimulai")
    self:countdown("Persiapan", Config.PREPARE_TIME)
end

-- Returns the structure when the hit counts. Only the enemy team is searched,
-- so your own buildings can't be hit
function MobaSession:attack(session, id, damage)
    local p = self:player(session)
    local enemy = self.teams[3 - p.team]
    local s = enemy.structures[id]
    if not s or p.deadUntil or not enemy:canAttack(s) then return end

    s.hp = math.max(s.hp - damage, 0)
    if s.hp == 0 and s.lane > 0 then
        self:notice(string.format("Turret tim %d lane %d hancur", s.team, s.lane))
    end
    -- TODO: send s.hp to the clients
    return s
end

function MobaSession:kill(killer, victim, assists)
    local k, v = self:player(killer), self:player(victim)
    if not v or k.team == v.team then return end

    k.kills, v.deaths = k.kills + 1, v.deaths + 1
    self.teams[k.team].kills = self.teams[k.team].kills + 1
    for _, helper in ipairs(assists or {}) do
        local h = self:player(helper)
        if h and h.team == k.team then h.assists = h.assists + 1 end
    end

    v.deadUntil = os.time() + Config.RESPAWN_TIME
    Service.countdown(victim, "Kebangkitan", Config.RESPAWN_TIME)
end

function MobaSession:lordKill(session)
    if not self.lordAlive then return end

    self.lordAlive = false
    self.lordAt = os.time() + self.mode.LORD_RESPAWN
    -- TODO: spawn the team's Lord for self:player(session).team
end

-- Team that lost fewer structures, then more kills. nil on a tie
function MobaSession:leading()
    local a, b = self.teams[1], self.teams[2]
    if a:lost() ~= b:lost() then return a:lost() < b:lost() and 1 or 2 end
    if a.kills ~= b.kills then return a.kills > b.kills and 1 or 2 end
end

-- Returns players left in the team, and the team index
function MobaSession:remove(session)
    local team = self:player(session).team
    return self.teams[team]:remove(session), team
end

-- Returns true, winner when the match is over
function MobaSession:update(now)
    if self.state == State.PREPARING then
        if now < self.expiresAt then return false end

        self.state = State.FIGHTING
        self.expiresAt = now + self.mode.FIGHT_TIME
        self.nextWave = now + Config.FIRST_WAVE
        self.lordAt = now + self.mode.LORD_AT
        self:notice("Hancurkan base musuh!")
        self:countdown("Waktu pertarungan", self.mode.FIGHT_TIME)
        return false
    end

    if now >= self.expiresAt then
        return true, self.state == State.FIGHTING and self:leading() or nil
    end

    if self.state ~= State.FIGHTING then return false end

    self:each(function(p)
        if p.deadUntil and p.deadUntil <= now then
            p.deadUntil = nil
            Service.countdown(p.session, "", 0)
            -- TODO: respawn p.session at its team spawn
        end
    end)

    if now >= self.nextWave then
        self.nextWave = now + Config.WAVE_INTERVAL
        -- TODO: spawn minions for each team and lane (1..self.mode.LANES)
    end

    if not self.lordAlive and now >= self.lordAt then
        self.lordAlive = true
        -- TODO: spawn the Lord monster
        self:notice("Lord muncul")
    end
    return false
end

return MobaSession
