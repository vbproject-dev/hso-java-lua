local MemberController = require "web.features.membership.MemberController"
--[[
--
--Filename: MemberApi.lua
--
--Created Date: Tuesday, October 6th 2026, 10:10:44 am
--
--Author: VIBE
--
--]]


local MemberApi = {}

function MemberApi:post(request)
    if request.path ~= "/api/membership" then
        return
    end

    return MemberController:create(request)
end

return MemberApi
