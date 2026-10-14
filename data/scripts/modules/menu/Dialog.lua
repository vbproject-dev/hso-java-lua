--[[
--
--Filename: Dialog.lua
--
--Created Date: Wednesday, October 14th 2026
--
--Author: VBPROJECT
--
--]]

local Dialog = class("Dialog")

function Dialog:ctor(id, text, args, onRespond)
    self.id = id
    self.text = text
    self.args = args or {}
    self.onRespond = onRespond
end

function Dialog.build(def)
    return Dialog.new(
        def.id,
        def.text,
        def.args or {},
        def.onRespond
    )
end

function Dialog:perform(yesOrNo)
    if self.onRespond then
        self.onRespond(yesOrNo)
    end
end

return Dialog
