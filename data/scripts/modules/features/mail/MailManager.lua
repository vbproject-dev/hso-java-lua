--[[
--
--Filename: MailManager.lua
--
--Created Date: Saturday, October 3rd 2026, 10:29:10 pm
--
--Author: VIBE
--
--]]
local Mail        = require "modules.features.mail.Mail"
local Service     = require("core.JavaClass").Service

local MailManager = {
    mails = ArrayList.new()
}

function MailManager.load()
    local result, err = loadTable("mail", { status = Mail.STATUS.UNREAD })

    if not result then
        log("[MailManager] error: %s", err)
        return
    end

    result:forEach(function(data)
        MailManager.mails:add(Mail.new(data))
    end)

    log("[MailManager] %d mails loaded", MailManager.mails:size())
end

function MailManager.send(data)
    local mail = Mail.new(data)

    local id, err = insertTable("mail", mail:toDatabase())
    if not id then
        log("[MailManager] Failed to send mail: %s", err)
        return nil, err
    end

    mail.id = id
    MailManager.mails:add(mail)

    return mail
end

function MailManager.get(playerId)
    return MailManager.mails:filter(function(mail)
        return mail.playerId == playerId and mail.status ~= Mail.STATUS.CLAIMED
    end)
end

function MailManager.getUnread(playerId)
    return MailManager.mails:filter(function(mail)
        return mail.playerId == playerId
            and mail.status == Mail.STATUS.UNREAD
    end)
end

function MailManager.read(mailId)
    local mail = MailManager.mails:findFirst(function(mail)
        return mail.id == mailId
    end)

    if not mail then
        return false, "Mail not found"
    end

    if mail.status == Mail.STATUS.UNREAD then
        mail.status = Mail.STATUS.READ

        local ok, err = updateTable("mail", {
            status = mail.status
        }, {
            id = mail.id
        })

        if not ok then
            return false, err
        end
    end

    return true
end

function MailManager.claim(session, mails)
    local gold = 0
    local gem = 0
    local items = {}

    mails:forEach(function(mail)
        if mail.status == Mail.STATUS.CLAIMED then
            return
        end

        gold = gold + mail.gold
        gem = gem + mail.gem

        for _, item in ipairs(mail.items) do
            table.insert(items, item)
        end
    end)

    if gold > 0 then
        session.p.updateGem(gold)
    end

    if gem > 0 then
        session.p.updateGem(gold)
    end

    for _, item in ipairs(items) do
        local itemObject = Java.callStatic("template.Item3", "fromTemplate", item.item_id)

        if itemObject then
            itemObject.tier = item.tier or 0
            itemObject.tierStar = item.tierStar or 0
            itemObject.color = item.color or itemObject.color

            itemObject.op:clear()

            for _, option in ipairs(item.options or {}) do
                itemObject.op:add(Java.new("template.Option", option.id, option.value))
            end

            session.p.item:add_item_bag3(itemObject)
        end
    end

    mails:forEach(function(mail)
        if mail.status ~= Mail.STATUS.CLAIMED then
            mail.status = Mail.STATUS.CLAIMED

            updateTable("mail", { status = Mail.STATUS.CLAIMED }, { id = mail.id })

            MailManager.notifyClaim(session, mail)
        end
    end)

    session.p.item:updateBag()

    return true
end

function MailManager.delete(mailId)
    local ok, err = deleteTable("mail", { id = mailId })

    if not ok then
        return false, err
    end

    local mail = MailManager.mails:findFirst(function(mail)
        return mail.id == mailId
    end)

    if mail then
        MailManager.mails:remove(mail)
    end

    return true
end

function MailManager.notifyUnread(session)
    local mails = MailManager.getUnread(session.p.objectId)

    mails:forEach(function(mail)
        MailManager.read(mail.id)

        Service.chat(session, mail.sender, mail.message)
    end)
end

function MailManager.notifyClaim(session, mail)
    if mail.status ~= Mail.STATUS.CLAIMED then
        return
    end

    local rewards = {}

    if mail.gold > 0 then
        table.insert(rewards, string.format("%d Gold", mail.gold))
    end

    if mail.gem > 0 then
        table.insert(rewards, string.format("%d Permata", mail.gem))
    end

    for _, item in ipairs(mail.items) do
        table.insert(rewards, string.format(
            "%s x%d",
            item.name,
            item.amount or 1
        ))
    end

    if #rewards == 0 then
        return
    end

    Service.chat(session, mail.sender, "Kamu menerima " .. table.concat(rewards, " "))
end

return MailManager
