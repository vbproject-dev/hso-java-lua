--[[
--
--Filename: FirebaseService.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local FirebaseService = {}

local SERVICE_ACCOUNT = {
    type = "service_account",
    project_id = "globalauth-b77eb",
    private_key_id = "9960feee7da0231120981f288440820c2c25c580",
    private_key =
    "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCiqoivTaJaqDfd\noA5DGXS5vfOgFVDCzNNkrknaKMSQkWgXgWSU1+zc6UzM6sxrdqsV8sY9AR7a1tnh\nKT4nxdkvpIy9mTrVpEWmkQ6+DFZsK7nP0/7geZv15YXpS32yqXmCXcN1KJXi0ips\nFW+lyfSXnErqLJsmMQy8dZAwZDiVad/rIQTnKquvAWnB44DYKXl6hoQXfzFF459z\nTIgUZOdCRAOyfz2+zJxHaNDpTdmV+7vqCyaoBAIs2ylMcC9FYKL37xTRX+O3oxyw\ncy8WfXsuoYlH0SZt7KsQup2cxjPXloo7opDmGQ18uSGvLMlbbzmG8b9KeBEDs5bS\n8i0QSCBfAgMBAAECggEAB0Lw0k9dtAmJlOct1Wj8tCrHbJoG/oTOQWxWXg372GHl\nV62uPWBcpO/1Pob5lFwgmKlumA9fJT3G52CqWadg+3N8PoQcnQ6CqkQURTLS/zkx\nR+Nnn8KBNKG7L9LvslrgBb9rZLHouJtqyv0zleve2cKSxY/NAHaoFUtAoihj+w3d\ndFctQCavtff7Qx8GqoXFGuzi47MfearkKmcqHig5u9L//oN/Uci8px9Wm4EVyems\nauYWSiq/eE7rqDadOeZRm1SRJsnD3xkunYfG7XBrDCZwIwhbxBW6534REClDdJgK\nus+Lr29Nbum1Jaalpn6yZ6pqsBIc8QASVObR17IloQKBgQDWIApFNctQ+WTWYP5o\nQRWcZ40fTf/OXDQsdueMazRq6AD5V85eshT9xffqY0zLZY85MNInXESlJlFFlQxA\nA6xCFLGD10i2yOmzlONHj4if+PrQwUX6QX9eLGPEVDTegNWYr9r80cEgtxLpSONx\npS2y0Pr+tTt5PluqV+1AAVcL0wKBgQDCekAfBoBWc7IrORK4sN7VheJp/Gxq9sde\npFHTpMjAUiW5pg7vP4jIusYnBvnfPClQaVciaj93FEKPTIHo6VS6WMt9XNG1yEVE\nHm7Ssy952yXSFn71CuedE/8ER7vttln1bTMGNADV5bc5ySX7fQiJBi9jeL7ZcObJ\nrIzpRKJ9xQKBgFIv/CEyk7ah8z2B/0R+7s+Yw4cnhi9sHq6OeTPhlj4OjQkn1dNt\nITeC/DSgJsLPWZkHDzMCbGrDeWBu5EPR5RV8IeLMCGH4XhOK6231PujARW1JMhXr\ne/rmqOibtatN4i54GWL/E9T90Clwy7Q8RX0kT6LiZ1CTSdXpZ+wwV3v3AoGALVVs\nDw9n6T8tADBcsdrhBusfvU8PQtvl26T4Qhq+hT5g9ubDwneP/iKzwDM7GhOfGdSE\ncExOIQcDAP53pgCGNK4wOTfi1ropk1h4wvrsDT7NkSyXSa3SEeawYqIKJ76DN9fN\ntht3OmVDEeBWz0n1LGPZthlWe796vPZRHqtWKXUCgYEAtSCfYj6EhG5EyYnrtRAf\nET/+76q3xHiBpgf5Rnoa52VCRz/qECJfzrz1pf9SFOfiC+pBJyF8KvnVR6jslQ0y\nFuZiOJPU77+iyDhLG6/SUAPvpSz0U47Fo7Ek8A3OHNDeeuxBrszjCQFZii6xyE4t\nl5s5izyTTeptqzOf2dappos=\n-----END PRIVATE KEY-----\n",
    client_email = "firebase-adminsdk-gjtyv@globalauth-b77eb.iam.gserviceaccount.com",
    client_id = "110120723550346125668",
    auth_uri = "https://accounts.google.com/o/oauth2/auth",
    token_uri = "https://oauth2.googleapis.com/token",
    auth_provider_x509_cert_url = "https://www.googleapis.com/oauth2/v1/certs",
    client_x509_cert_url =
    "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-gjtyv%40globalauth-b77eb.iam.gserviceaccount.com",
    universe_domain = "googleapis.com"

}

local SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

local accessToken = nil
local expireIn = 0
local tokenType = nil

local function getAccessToken()
    local now = os.time()

    if accessToken
        and now < expireIn - 60 then
        return accessToken
    end

    local header = Crypto.base64UrlEncode(
        JSON.fromTable({
            alg = "RS256",
            typ = "JWT"
        })
    )

    local payload = Crypto.base64UrlEncode(
        JSON.fromTable({
            iss = SERVICE_ACCOUNT.client_email,
            scope = SCOPE,
            aud = SERVICE_ACCOUNT.token_uri,
            iat = now,
            exp = now + 3600
        })
    )

    local unsigned =
        header .. "." .. payload

    local signature = Crypto.rsaSha256Sign(
        SERVICE_ACCOUNT.private_key,
        unsigned
    )

    local assertion =
        unsigned .. "." ..
        Crypto.base64UrlEncode(signature)

    local response, err = Http.post(
        SERVICE_ACCOUNT.token_uri,
        {
            grant_type =
            "urn:ietf:params:oauth:grant-type:jwt-bearer",
            assertion = assertion
        },
        "application/x-www-form-urlencoded"
    )

    if not response then
        log("Firebase OAuth error:", tostring(err))
        return nil, err
    end

    local result = JSON.toTable(response)
    if not result.access_token then
        local message = result.error_description or result.error or "Failed to obtain access token"

        log("Firebase OAuth error:", message)

        return nil, message
    end

    accessToken =
        result.access_token

    expireIn = os.time() + (result.expires_in or 3600)

    tokenType = result.token_type or "Bearer"

    return accessToken
end

function FirebaseService.sendNotification(
    deviceToken,
    title,
    body
)
    local accessToken, err = getAccessToken()

    if not accessToken then
        return nil, err
    end

    local url =
        "https://fcm.googleapis.com/v1/projects/" ..
        SERVICE_ACCOUNT.project_id ..
        "/messages:send"

    local response, err = Http.post(
        url,
        {
            message = {
                token = deviceToken,
                notification = {
                    title = title,
                    body = body
                }
            }
        },
        "application/json",
        {
            ["Authorization"] =
                FirebaseService.tokenType ..
                " " ..
                accessToken
        }
    )

    if not response then
        return nil, err
    end

    return response
end

return FirebaseService
