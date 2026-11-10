local MobaTeam = class("MobaTeam")

function MobaTeam:ctor(index, sessions, mode)
    self.index = index
    self.mode = mode
    self.kills = 0
    self.players = {}    -- objectId -> { session, team, kills, deaths, assists, deadUntil }
    self.structures = {} -- id -> { id, team, lane, tier, hp }

    for _, session in ipairs(sessions) do
        self.players[session.p.objectId] = { session = session, team = index, kills = 0, deaths = 0, assists = 0 }
    end

    for lane = 1, mode.LANES do
        for tier, hp in ipairs(mode.TURRET_HP) do
            self:build(lane, tier, hp)
        end
    end
    self:build(0, 0, mode.BASE_HP)
end

-- id = team * 100 + lane * 10 + tier (base = team * 100)
function MobaTeam:build(lane, tier, hp)
    local id = self.index * 100 + lane * 10 + tier
    self.structures[id] = { id = id, team = self.index, lane = lane, tier = tier, hp = hp }
end

function MobaTeam:each(fn)
    for _, p in pairs(self.players) do fn(p) end
end

function MobaTeam:size()
    local count = 0
    for _ in pairs(self.players) do count = count + 1 end
    return count
end

-- Returns how many players are left
function MobaTeam:remove(session)
    self.players[session.p.objectId] = nil
    return self:size()
end

-- Structures this team has lost
function MobaTeam:lost()
    local count = 0
    for _, s in pairs(self.structures) do
        if s.hp <= 0 then count = count + 1 end
    end
    return count
end

-- Can the enemy hit this structure? Turret: previous tier must be dead.
-- Base: some lane's last turret must be dead
function MobaTeam:canAttack(s)
    if s.hp <= 0 then return false end
    if s.lane > 0 then return s.tier == 1 or self.structures[s.id - 1].hp <= 0 end

    for lane = 1, self.mode.LANES do
        if self.structures[self.index * 100 + lane * 10 + #self.mode.TURRET_HP].hp <= 0 then return true end
    end
    return false
end

return MobaTeam
