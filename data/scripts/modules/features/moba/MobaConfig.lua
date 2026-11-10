return {
    MODE_3V3 = 1,
    MODE_5V5 = 2,

    MODES = {
        [1] = {
            NAME = "3 vs 3",
            SIZE = 3,
            LANES = 1,
            TURRET_HP = { 8000, 12000 },
            BASE_HP = 15000,
            FIGHT_TIME = 900,
            LORD_AT = 240,
            LORD_RESPAWN = 150
        },
        [2] = {
            NAME = "5 vs 5",
            SIZE = 5,
            LANES = 3,
            TURRET_HP = { 8000, 10000, 12000 },
            BASE_HP = 20000,
            FIGHT_TIME = 1800,
            LORD_AT = 480,
            LORD_RESPAWN = 180
        },
    },

    QUEUE_TIMEOUT = 60,
    ENTER_TIMEOUT = 30,
    PREPARE_TIME = 10,
    RESPAWN_TIME = 10,
    FIRST_WAVE = 15,
    WAVE_INTERVAL = 30,
}
