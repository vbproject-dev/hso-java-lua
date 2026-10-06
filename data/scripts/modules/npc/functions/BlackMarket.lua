--[[
--
--Filename: BlackMarket.lua
--
--Created Date: Friday, October 2nd 2026, 9:33:59 pm
--
--Author: VIBE
--
--]]



local Service        = require("core.JavaClass").Service
local MenuHelper     = require("modules.menu.MenuHelper")
local AuctionManager = require("modules.features.auction.AuctionManager")

local MailManager    = require("modules.features.mail.MailManager")


return {
    onTalk = function(session, npcId)
        local menu = MenuHelper.build("Black Market", npcId, {
            {
                name = "Store",
                action = function()
                    AuctionManager.openAuction(session)
                end
            },

            MenuHelper.when(AuctionManager.hasItemOnSale(session), {
                name = "Batalkan Penjualan",
                action = function()
                    AuctionManager.cancelAll(session)
                end
            }),

            {
                name = "Mail",
                action = function()
                    local mails = MailManager.get(session.p.objectId)
                    if mails:size() <= 0 then
                        Service.notice(session, "Kamu tidak memiliki Mail")
                        return
                    end


                    MailManager.claim(session, mails)
                end
            },

            {
                name = "Informasi",
                action = function()
                    local tax = AuctionManager.TAX
                    local maxItem = AuctionManager.MAX_ITEM
                    local minLevel = AuctionManager.MIN_LEVEL
                    local registerTax = AuctionManager.REGISTER_TAX


                    Service.chat(session, "Black Market",
                        "\nPanduan dan Persyaratan\n" ..
                        "- Level karakter harus mencapai " .. minLevel .. "+.\n" ..
                        "- Maksimal " .. maxItem .. " item dapat dijual secara bersamaan.\n" ..
                        "- Membership mendapatkan 5 slot penjualan tambahan.\n" ..
                        "- Membership tidak dikenakan pajak registrasi dan penjualan.\n" ..
                        "- Durasi penjualan adalah 7 hari.\n" ..
                        "- Pajak penjualan sebesar " .. tax .. "% dari harga item.\n" ..
                        "- Biaya pendaftaran penjualan sebesar " .. registerTax .. "% dari harga item.\n" ..
                        "- Jika item terjual, hasil penjualan akan dikirim melalui Mail.\n" ..
                        "- Hasil penjualan akan dikirim setelah dipotong pajak.\n" ..
                        "- Jika penjualan dibatalkan, item akan dikembalikan melalui Mail.\n" ..
                        "- Item yang tidak terjual hingga masa berlaku berakhir akan dikembalikan melalui Mail.\n" ..
                        "- Kamu tidak dapat membeli item yang kamu jual sendiri."
                    )
                end
            },

            -- {
            --     name = "Go Map",
            --     action = function()
            --         local input = Input.build({
            --             npcId = npcId,
            --             title = "Buat Guild",
            --             fields = {
            --                 { name = "Map ID", type = InputType.NUMERIC },
            --                 { name = "X",      type = InputType.NUMERIC },
            --                 { name = "Y",      type = InputType.NUMERIC },
            --             },

            --             action = function(session, values)
            --                 local map, x, y = values[1], values[2], values[3]
            --                 if not map or not x or not y then
            --                     return
            --                 end

            --                 Service.goMap(session, map, x, y)
            --             end,
            --         })
            --         session.state:put("input", input)
            --         Service.openInput(session, input)
            --     end
            -- }
        })

        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
