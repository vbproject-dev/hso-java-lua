--[[
--
--Filename: Input.lua
--
--Created Date: Friday, October 2nd 2026, 8:45:27 am
--
--Author: VBPROJECT
--
--]]

local Input = require("class")("Input")

InputType = {
    TEXT = 0,
    NUMERIC = 1,
}

function Input:ctor(npcId, title, fields, action)
    self.npcId = npcId
    self.title = title
    self.fields = fields
    self.action = action
end

function Input.build(def)
    return Input.new(def.npcId, def.title, def.fields, def.action)
end

return Input
