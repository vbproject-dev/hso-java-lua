local ItemController = require "web.features.item.ItemController"
local ItemApi = {}

function ItemApi:get(request)
    if request.path == "/lua/item/find" then
        return ItemController.find(request.query)
    elseif request.path == "/lua/item" then
        local page = tonumber(request.query.page) or 1
        local limit = tonumber(request.query.limit) or 20
        local category = tonumber(request.query.category) or 3

        return ItemController.list(page, limit, category)
    end
end

return ItemApi
