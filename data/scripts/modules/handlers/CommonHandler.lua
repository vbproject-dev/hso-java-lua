--[[
--
--Filename: CommonHandler.lua
--
--Created Date: Thursday, October 1st 2026, 11:34:13 pm
--
--Author: VBPROJECT
--
--]]

local Service = require("core.JavaClass").Service

local function onDynamicMenu(session, packet)
    local reader = packet:reader()
    local npcId = reader:readShort()
    local menuId = reader:readByte()
    local index = reader:readByte()

    Service.notice(session, "Belum ada fitur untuk menu ini")
    return true
end

return {
    -- [Cmd.DYNAMIC_MENU] = onDynamicMenu
}
