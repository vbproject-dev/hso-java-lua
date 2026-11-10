local Config       = {}

Config.environment = "sandbox"                 -- CHANGE to "production" when you go live
Config.clientId    = "BRN-0289-1793274857416"  -- CHANGE: Client ID from DOKU Back Office (Integration > API Keys)
Config.secretKey   = "SK-u9WXL8RCvYwxyRwd5gAc" -- CHANGE: Secret Key from the same page


Config.callbackUrl       = "https://example.com/payment/finished" --
Config.cancelUrl         = nil                                    --

Config.notificationPath  = "/lua/doku/notify"

Config.currency          = "IDR"
Config.language          = "ID"
Config.paymentDueMinutes = 60 -- how long the payment link stays valid


Config.packages = {
    gem_100  = { name = "100 Gem", price = 10000, gem = 100 },
    gem_500  = { name = "500 Gem", price = 45000, gem = 500 },
    gem_1000 = { name = "1.000 Gem", price = 85000, gem = 1000 },
}

-- Called once when a payment is confirmed
Config.onPaid   = function(tx)
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
