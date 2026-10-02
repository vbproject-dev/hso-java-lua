--[[
--Filename: Zoro.lua
--Created Date: Friday, October 2nd 2026, 3:10:32 am
--Author: VBPROJECT
--]]


local Service    = require("core.JavaClass").Service
local Menu       = require("modules.menu.Menu")
local MenuHelper = require("modules.menu.MenuHelper")
local Input      = require("modules.menu.Input")


local CREATE_GUILD_COST = 20000

local function createMenu(session, npcId)
    local clan = session.p.myclan

    local hasGuild = clan ~= nil
    return MenuHelper.build(Menu, "Zoro", npcId, {
        MenuHelper.when(hasGuild, {
            {
                MenuHelper.when(hasGuild and clan:isLeader(session.p.name), {
                    {
                        name = "Kelola Guild",
                        children = {
                            {
                                name = "Tingkatkan Guild",
                                action = function()
                                    Java.callStatic("core.MenuController", "Menu_Clan_Manager", session, 1)
                                end,
                            },
                            {
                                name = "Bubarkan Guild",
                                action = function()
                                    Java.callStatic("core.MenuController", "Menu_Clan_Manager", session, 2)
                                end,
                            },

                            {
                                name = "Transfer Pemimpin",
                                action = function()
                                    Java.callStatic("core.MenuController", "Menu_Clan_Manager", session, 3)
                                end,
                            },


                        }
                    },

                    {
                        name = "Shop Icon",
                        action = function()
                            Service.openUI(session, 29)
                        end,
                    },
                    {
                        name = "Shop Guild",
                        action = function()
                            Service.openUI(session, 30)
                        end,
                    },
                    {
                        name = "Buat Jubah",
                        children = {
                            {
                                name = "Jubah Ksatria",
                                action = function()
                                    Service.buatJubah(session, 0)
                                end,
                            },
                            {
                                name = "Jubah Penyihir",
                                action = function()
                                    Service.buatJubah(session, 1)
                                end,
                            },
                            {
                                name = "Jubah Assasin",
                                action = function()
                                    Service.buatJubah(session, 2)
                                end,
                            },
                            {
                                name = "Jubah Penembak",
                                action = function()
                                    Service.buatJubah(session, 3)
                                end,
                            },

                        }
                    },
                    {
                        name = "Buat Gelar",
                        children = {
                            {
                                name = "Gelar Ksatria",
                                action = function()
                                    Service.buatTitle(session, 0)
                                end,
                            },
                            {
                                name = "Gelar Penyihir",
                                action = function()
                                    Service.buatTitle(session, 1)
                                end,
                            },
                            {
                                name = "Gelar Assasin",
                                action = function()
                                    Service.buatTitle(session, 2)
                                end,
                            },
                            {
                                name = "Gelar Penembak",
                                action = function()
                                    Service.buatTitle(session, 3)
                                end,
                            },

                        }
                    },
                }),

                {
                    name = "Gudang Guild",
                    action = function()
                        clan:open_box_clan(session)
                    end,
                },

                {
                    name = "Donate Gold",
                    action = function()
                        local input = Input.build({
                            npcId = npcId,
                            title = "Gold Donation",
                            fields = {
                                { name = "Jumlah", type = InputType.NUMERIC },
                            },
                            action = function(session, values)
                                local value = values[1]
                                if not value then
                                    return
                                end


                                if session.p:getGold() < value then
                                    Service.notice(session,
                                        string.format("Gold kurang kamu kurang dari %d", value))
                                    return
                                end

                                clan:contributeGold(session, value)
                                session.p:updateGold(-value)
                                session.p.item:charInventory(5)
                                Service.notice(session, string.format("Kamu telah menyumbang %d gold", value))
                            end,
                        })
                        session.state:put("input", input)
                        Service.openInput(session, input)
                    end,
                },

                {
                    name = "Donate Permata",
                    action = function()
                        local input = Input.build({
                            npcId = npcId,
                            title = "Gem Donation",
                            fields = {
                                { name = "Jumlah", type = InputType.NUMERIC },
                            },
                            action = function(session, values)
                                local value = values[1]
                                if not value then
                                    return
                                end


                                if session.p:getGem() < value then
                                    Service.notice(session,
                                        string.format("Permata kurang kamu kurang dari %d", value))
                                    return
                                end

                                clan:contributeGem(session, value)
                                session.p:updateGem(-value)
                                session.p.item:charInventory(5)
                                Service.notice(session, string.format("Kamu telah menyumbang %d permata", value))
                            end,
                        })
                        session.state:put("input", input)
                        Service.openInput(session, input)
                    end,
                },

                MenuHelper.when(hasGuild and not clan:isLeader(session.p.name), {
                    name = "Tinggalkan Guild",
                    action = function()
                        Java.callStatic("core.Service", "send_box_input_yesno", session, 117,
                            "Yakin ingin meninggalkan guild ?")
                    end
                })

            },
        }),


        MenuHelper.when(not hasGuild, {
            {
                name = "Buat Guild",
                action = function()
                    local input = Input.build({
                        npcId = npcId,
                        title = "Buat Guild",
                        fields = {
                            { name = "Nama Guild",  type = InputType.TEXT },
                            { name = "Alias Guild", type = InputType.TEXT },
                        },
                        action = function(session, values)
                            local name, alias = values[1], values[2]
                            if not name or not alias then
                                return
                            end
                            if #name < 4 or #name > 10 then
                                Service.notice(session, "Nama guild harus 4–10 karakter")
                                return
                            end

                            if #alias < 3 or #alias > 6 then
                                Service.notice(session, "Alias harus 3–6 karakter")
                                return
                            end

                            if session.p:getGem() < CREATE_GUILD_COST then
                                Service.notice(session,
                                    string.format("Membutuhkan %d gem untuk melanjutkan", CREATE_GUILD_COST))
                                return
                            end

                            local guild = Java.callStatic("game.guild.Guild", "create_clan", session, name, alias)
                            if not guild then
                                Service.notice(session, "Terjadi kesalahan")
                                return
                            end

                            session.p:updateGem(-CREATE_GUILD_COST)
                            session.p.item:charInventory(5)
                            Service.openUI(session, 20)
                            Service.notice(session, "Pilih icon guild")
                        end,
                    })
                    session.state:put("input", input)
                    Service.openInput(session, input)
                end,
            },

            {
                name = "Informasi",
                action = function()
                    Service.notice(session,
                        "Guild digunakan untuk berkumpul dan bekerja sama dengan pemain lain.\n" ..
                        "Sebagai anggota, kamu bisa mengikuti kegiatan guild dan menggunakan fasilitas yang tersedia.\n" ..
                        "Pemimpin dan anggota tertentu memiliki akses untuk mengelola guild serta anggotanya."
                    )
                end,
            },
        }),

    })
end


return {
    onTalk = function(session, npcId)
        local menu = createMenu(session, npcId)
        session.state:put("menu", menu)
        Service.openMenu(session, menu)
        return true
    end
}
