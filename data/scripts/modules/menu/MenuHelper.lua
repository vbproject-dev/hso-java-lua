--[[
--
--Filename: MenuHelper.lua
--
--Created Date: Friday, October 2nd 2026, 6:47:04 am
--
--Author: VBPROJECT
--
--]]

local MenuHelper = {}

function MenuHelper.when(condition, entries)
    return condition and entries or {}
end

function MenuHelper.build(Menu, name, npcId, entries)
    local menu = Menu.new(name, nil, npcId)
    MenuHelper.add(menu, entries)
    return menu
end

function MenuHelper.add(menu, entries)
    for _, entry in ipairs(entries) do
        if entry.name or type(entry[1]) == "string" then
            local name = entry.name or entry[1]
            local action = entry.action or entry[2]
            local children = entry.children or entry[3]

            local child = menu:add(name, action)

            if children then
                MenuHelper.add(child, children)
            end
        else
            MenuHelper.add(menu, entry)
        end
    end
end

return MenuHelper
