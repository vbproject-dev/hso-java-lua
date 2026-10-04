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
                        Service.notice(session, "Kamu tidak memiliki pesan")
                        return
                    end


                    MailManager.claim(session, mails)
                end
            },

            {
                name = "Informasi",
                action = function()
                    Service.notice(session,
                        "Black Market\n" ..
                        "Maksimal 10 item yang dapat dijual.\n" ..
                        "Durasi penjualan adalah 7 hari.\n" ..
                        "Pajak penjualan sebesar 10% dari harga item.\n" ..
                        "Setelah item terjual, hasil penjualan dikirim melalui Mail.\n" ..
                        "Hasil yang diterima adalah harga setelah dipotong pajak.\n" ..
                        "Penjualan yang dibatalkan akan dikembalikan melalui Mail.\n" ..
                        "Item yang tidak terjual hingga masa berlaku habis akan dikembalikan melalui Mail.\n" ..
                        "Kamu tidak dapat membeli item milik sendiri."
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
