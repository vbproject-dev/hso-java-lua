--[[
--
--Filename: Menu.lua
--
--Created Date: Friday, October 2nd 2026, 4:20:54 am
--
--Author: VBPROJECT
--
--]]

local Menu = class("Menu")

function Menu:ctor(name, action, npcId)
    self.name = name
    self.action = action
    self.npcId = npcId
    self.parent = nil
    self.children = ArrayList.new()
end

function Menu:add(name, action)
    local child = Menu.new(name, action, self.npcId)
    child.parent = self
    self.children:add(child)
    return child
end

function Menu:perform(...)
    if self.action then
        return self.action(...)
    end
end

function Menu:get(index)
    return self.children:get(index)
end

function Menu:size()
    return self.children:size()
end

function Menu:back()
    return self.parent
end

function Menu.build(def)
    local name, action = def.name, def.action
    local start = 1
    if not name then
        name, action = def[1], def[2]
        start = 3
    end

    local menu = Menu.new(name, action)
    for i = start, #def do
        local child = Menu.build(def[i])
        child.parent = menu
        menu.children:add(child)
    end
    return menu
end

function Menu.fromJSON(def, actions)
    local action = def.action and actions[def.action]
    local menu = Menu.new(def.name, action)
    for _, childDef in ipairs(def.children or {}) do
        local child = Menu.fromJSON(childDef, actions)
        child.parent = menu
        menu.children:add(child)
    end
    return menu
end

return Menu
