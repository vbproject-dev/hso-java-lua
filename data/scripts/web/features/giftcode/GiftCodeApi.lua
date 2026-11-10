local GiftCodeController = require "web.features.giftcode.GiftCodeController"
local GiftCodeApi = {}

-- Must match the paths in api.js (ENDPOINTS).
local BASE = "/lua/giftcode"

function GiftCodeApi:get(request)
    if request.path == BASE then
        return GiftCodeController.list()
    end
end

-- Every write is a POST with a JSON body.
function GiftCodeApi:post(request)
    if request.path == BASE then
        return GiftCodeController.create(request.body)
    end

    if request.path == BASE .. "/update" then
        return GiftCodeController.update(request.body)
    end

    if request.path == BASE .. "/delete" then
        return GiftCodeController.delete(request.body)
    end
end

return GiftCodeApi
