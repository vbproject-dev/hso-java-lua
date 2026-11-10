local DokuController = require("web.features.doku.DokuController")

local DokuApi = {}

function DokuApi:get(request)
    if request.path == "/lua/doku/packages" then
        return DokuController.packages()
    end

    if request.path == "/lua/doku/status" then
        return DokuController.status(request.query)
    end

    -- Admin: newest transactions first. Protect this route if the server is reachable from the internet.
    if request.path == "/lua/doku/history" then
        return DokuController.history(request.query)
    end
end

function DokuApi:post(request)
    if request.path == "/lua/doku/create" then
        return DokuController.create(request.body)
    end

    -- DOKU calls this. It is safe to leave open: every notification is checked against its signature.
    if request.path == "/lua/doku/notify" then
        return DokuController.notify(request)
    end

    -- Admin: check pending payments and retry failed deliveries.
    if request.path == "/lua/doku/reconcile" then
        return DokuController.reconcile()
    end
end

return DokuApi
