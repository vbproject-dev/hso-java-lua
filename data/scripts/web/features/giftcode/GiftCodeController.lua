local GiftCodeController = {}

function GiftCodeController.list()
    local list = loadTable("giftcode")
    return list:toTable()
end

return GiftCodeController
