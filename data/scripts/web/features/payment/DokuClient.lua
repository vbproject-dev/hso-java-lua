local Config        = require("modules.features.doku.DokuConfig")

local DokuClient    = {}

local BASE_URLS     = {
    sandbox    = "https://api-sandbox.doku.com",
    production = "https://api.doku.com",
}
local CHECKOUT_PATH = "/checkout/v1/payment"
local STATUS_PATH   = "/orders/v1/status/"

local function decode(text)
    if type(text) ~= "string" or text == "" then return nil end
    local ok, data = pcall(JSON.toTable, text)
    return ok and type(data) == "table" and data or nil
end

local function lowerHeaders(headers)
    local out = {}
    for key, value in pairs(headers or {}) do
        out[tostring(key):lower()] = tostring(value)
    end
    return out
end

local function clean(text)
    return (tostring(text or ""):gsub("[%c'\"\\]", ""):sub(1, 200))
end

local function normalize(data)
    local order = type(data.order) == "table" and data.order or {}
    local transaction = type(data.transaction) == "table" and data.transaction or {}
    local channel = type(data.channel) == "table" and data.channel or {}

    return {
        invoice = order.invoice_number,
        amount  = tonumber(order.amount),
        status  = tostring(transaction.status or ""):upper(),
        channel = tostring(channel.id or ""),
    }
end

function DokuClient.configError()
    if not BASE_URLS[Config.environment] then
        return "Config.environment harus 'sandbox' atau 'production'"
    end

    if Config.clientId == "BRN-0000-0000000000000" or Config.secretKey == "SK-change-me" then
        return "DOKU belum dikonfigurasi (clientId dan secretKey di DokuConfig.lua)"
    end

    return nil
end

function DokuClient.sign(clientId, secretKey, requestId, timestamp, target, body)
    local lines = {
        "Client-Id:" .. clientId,
        "Request-Id:" .. requestId,
        "Request-Timestamp:" .. timestamp,
        "Request-Target:" .. target,
    }

    if body and body ~= "" then
        lines[#lines + 1] = "Digest:" .. Crypto.base64Encode(Crypto.sha256(body))
    end

    return "HMACSHA256=" .. Crypto.base64Encode(Crypto.hmacSha256(secretKey, table.concat(lines, "\n")))
end

local function request(method, target, bodyTable)
    local requestId = string.format("RQ-%d-%s", os.time(), Crypto.randomHex(6))
    local timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ")
    local body = bodyTable and JSON.fromTable(bodyTable) or nil

    local headers = {
        ["Client-Id"]         = Config.clientId,
        ["Request-Id"]        = requestId,
        ["Request-Timestamp"] = timestamp,
        ["Signature"]         = DokuClient.sign(Config.clientId, Config.secretKey, requestId, timestamp, target, body),
    }

    local url = BASE_URLS[Config.environment] .. target
    local response

    if method == "GET" then
        response = Http.get(url, headers)
    else
        -- Content-Type goes in the third argument; the body is sent exactly as signed.
        response = Http.post(url, body or "", "application/json", headers)
    end

    if not response or response.status == 0 then
        return nil, response and response.error or "tidak ada respons"
    end

    return { status = response.status, data = decode(response.body), raw = response.body }
end

function DokuClient.createCheckout(order, customer)
    local body = {
        order    = {
            amount              = order.amount,
            invoice_number      = order.invoice,
            currency            = Config.currency,
            language            = Config.language,
            callback_url        = Config.callbackUrl,
            callback_url_cancel = Config.cancelUrl,
            auto_redirect       = true,
            line_items          = { { name = order.name, price = order.amount, quantity = 1 } },
        },
        payment  = { payment_due_date = Config.paymentDueMinutes },
        customer = customer,
    }

    local response, err = request("POST", CHECKOUT_PATH, body)
    if not response then
        return nil, err
    end

    local payment = response.data and response.data.response and response.data.response.payment
    if not payment or type(payment.url) ~= "string" then
        local errors = response.data and response.data.error_messages
        log("[DokuClient] checkout rejected (%s): %s", tostring(response.status),
            clean(errors and errors[1] or response.raw))
        return nil, "DOKU menolak permintaan pembayaran (" .. tostring(response.status) .. ")"
    end

    return { url = payment.url, expiresAt = payment.expired_date }
end

function DokuClient.fetchStatus(invoice)
    local response, err = request("GET", STATUS_PATH .. invoice)
    if not response then
        return nil, err
    end

    if response.data and type(response.data.transaction) == "table" then
        return normalize(response.data)
    end

    if response.status == 404 then
        return { invoice = invoice, status = "NOT_FOUND", channel = "" }
    end

    return nil, "DOKU status HTTP " .. tostring(response.status)
end

function DokuClient.verify(request)
    local h = lowerHeaders(request.headers)
    local clientId, requestId, timestamp, signature = h["client-id"], h["request-id"], h["request-timestamp"],
        h["signature"]

    if not (clientId and requestId and timestamp and signature) then
        return false, "header DOKU tidak lengkap"
    end

    if clientId ~= Config.clientId then
        return false, "Client-Id tidak cocok"
    end

    local target = Config.notificationPath or request.path
    local expected = DokuClient.sign(clientId, Config.secretKey, requestId, timestamp, target, request.body or "")

    if not Crypto.secureCompare(signature, expected) then
        return false, "signature tidak cocok"
    end

    return true
end

function DokuClient.parseNotification(body)
    local data = decode(body)
    return data and normalize(data) or nil
end

return DokuClient
