--[[
--
--Filename: JWT.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local JWT = {}

function JWT.encode(payload, secret)
    local header = Crypto.base64UrlEncode(
        JSON.fromTable({
            alg = "HS256",
            typ = "JWT"
        })
    )

    local body = Crypto.base64UrlEncode(
        JSON.fromTable(payload)
    )

    local unsigned = header .. "." .. body

    local signature =
        Crypto.hmacSha256(secret, unsigned)

    return unsigned .. "." ..
        Crypto.base64UrlEncode(signature)
end

function JWT.decode(token, secret)
    local header, body, signature =
        token:match("^([^.]+)%.([^.]+)%.([^.]+)$")

    if not header or not body or not signature then
        return nil, "Invalid JWT"
    end

    local unsigned = header .. "." .. body

    local expected =
        Crypto.base64UrlEncode(
            Crypto.hmacSha256(secret, unsigned)
        )

    if not Crypto.secureCompare(expected, signature) then
        return nil, "Invalid signature"
    end

    local payload

    local ok, err = pcall(function()
        payload = JSON.toTable(
            Crypto.base64UrlDecode(body)
        )
    end)

    if not ok then
        return nil, "Invalid payload"
    end

    local now = os.time()

    if payload.exp and now >= payload.exp then
        return nil, "Token expired"
    end

    if payload.nbf and now < payload.nbf then
        return nil, "Token not active"
    end

    return payload
end

return JWT
