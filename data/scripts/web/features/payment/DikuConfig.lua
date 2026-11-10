local Config             = {}

Config.environment       = "sandbox"                 -- CHANGE to "production" when you go live
Config.clientId          = "BRN-0289-1793274857416"  -- CHANGE: Client ID from DOKU Back Office (Integration > API Keys)
Config.secretKey         = "SK-u9WXL8RCvYwxyRwd5gAc" -- CHANGE: Secret Key from the same page

-- Where DOKU sends the player after paying or cancelling (your own website or a thank-you page).
Config.callbackUrl       = "https://example.com/payment/finished" -- CHANGE
Config.cancelUrl         = nil                                    -- optional: where the player goes if they cancel

-- The path DOKU calls to report a payment. In DOKU Back Office set the Notification URL to
--   https://YOUR-DOMAIN + this path        (it must be reachable from the internet)
Config.notificationPath  = "/lua/doku/notify"

Config.currency          = "IDR"
Config.language          = "ID" -- language of the DOKU payment page: "ID" or "EN"
Config.paymentDueMinutes = 60   -- how long the payment link stays valid

-- What players can buy. The price is ALWAYS taken from here, never from the request.
Config.packages          = {
    gem_100  = { name = "100 Gem", price = 10000, gem = 100 },
    gem_500  = { name = "500 Gem", price = 45000, gem = 500 },
    gem_1000 = { name = "1.000 Gem", price = 85000, gem = 1000 },
}

-- Called once when a payment is confirmed. Give the player what they bought, then return true.
-- Return false (or raise an error) if you could not: the payment stays "PAID, not delivered"
-- and delivery is retried by DokuManager.reconcile().
Config.onPaid            = function(tx)
    -- tx.player_id, tx.gem, tx.amount, tx.package_id, tx.invoice
    --
    -- Example for an online player (adapt to your own code):
    --   local session = SessionManager.getByPlayerId(tx.player_id)
    --   if session then
    --       session.p:updateGem(tx.gem)
    --       return true
    --   end
    --   -- offline player: update the player's row in your database here, then return true
    log("[DOKU] Config.onPaid is not implemented yet (invoice %s, player %s, %s gem)", tx.invoice, tostring(tx.player_id),
        tostring(tx.gem))
    return false
end

return Config
