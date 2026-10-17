local GiftCodeController = require "web.features.giftcode.GiftCodeController"
local GiftCodeApi = {}

function GiftCodeApi:post(request)
    if request.path ~= "/api/giftcode" then
        return
    end

    return GiftCodeController:create(request)
end

function GiftCodeApi:get(request)
    if request.path == "/api/giftcode/list" then
        return GiftCodeController.list()
    end
end

return GiftCodeApi
