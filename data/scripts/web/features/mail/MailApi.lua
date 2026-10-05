local MailController = require("web.features.mail.MailController")

local MailApi = {}

function MailApi:post(request)
    if request.path ~= "/api/mail" then
        return
    end

    return MailController:create(request)
end

return MailApi
