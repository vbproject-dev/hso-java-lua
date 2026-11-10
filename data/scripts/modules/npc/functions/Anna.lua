local Service         = require("core.JavaClass").Service
local MenuHelper      = require("modules.menu.MenuHelper")
local GiftCodeManager = require("modules.features.giftcode.GiftCodeManager")
local Input           = require("modules.menu.Input")

return {
    onTalk = function(session, npcId)
        local menu = MenuHelper.build("Anna", npcId, {
            {
                name = "GiftCode",
                action = function()
                    local input = Input.build({
                        npcId = npcId,
                        title = "GiftCode",
                        fields = {
                            { name = "Kode", type = InputType.TEXT },
                        },

                        action = function(session, values)
                            local value = values[1]
                            if not value then
                                return
                            end

                            GiftCodeManager.claim(session, string.lower(value))
                        end
                    })
                    session.state:put("input", input)
                    Service.openInput(session, input)
                end
            }
        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
