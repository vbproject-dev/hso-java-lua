--[[
--
--Filename: Cmd.lua
--
--Created Date: Thursday, October 1st 2026, 11:16:47 pm
--
--Author: VBPROJECT
--
--]]



local Cmd = {
    -- Client to Server Cmds
    LOGIN = 1,
    OBJECT_MOVE = 4,
    CHAR_INFO = 5,
    FIRE_PK = 6,
    MONSTER_INFO = 7,
    FIRE_MONSTER = 9,
    USE_ITEM = 11,
    OK_CHANGE_MAP = 12,
    SELECT_CHAR = 13,
    CREATE_CHAR = 14,
    DELETE_ITEM = 18,
    GET_ITEM_MAP = 20,
    ITEM_MORE_INFO = 21,
    ADD_BASE_SKILL_POINT = 22,
    NPC_INFO = 23,
    BUY_ITEM = 24,
    CHAT_POPUP = 27,
    GET_ITEM_TEMPLATE = 28,
    GO_HOME = 31,
    USE_POTION = 32,
    CHAT_TAB = 34,
    FRIEND = 35,
    BUY_SELL = 36,
    REGISTER = 39,
    BUFF = 40,
    PK = 42,
    UPDATE_PET_CONTAINER = 44,
    PET_EAT = 45,
    PARTY = 48,
    OTHER_PLAYER_INFO = 49,
    CHANGE_AREA = 51,
    QUEST = 52,
    REQUEST_AREA = 54,
    SAVE_RMS_SERVER = 55,
    SET_PAGE = 56,
    CHAT_NPC = 60,
    UPDATE_CHAR_CHEST = 65,
    REBUILD_ITEM = 67,
    THACH_DAU = 68,
    CLAN = 69,
    CHAT_WORLD = 71,
    REPLACE_ITEM = 73,
    REBUILD_WING = 77,

    -- Missing client Cmds discovered
    NOTICE_BOX = 37,
    SEND_CMD_SERVER_59 = 59,
    SEND_CMD_SERVER_61 = 61,
    SEND_CMD_SERVER_MINUS_56 = -56,

    -- Server to Client Cmds
    LOGIN_FAIL = 2,
    MAIN_CHAR_INFO = 3,
    PLAYER_EXIT = 8,
    MONSTER_FIRE = 10,
    CHANGE_MAP = 12,
    CHAR_WEARING = 15,
    CHAR_INVENTORY = 16,
    DIE_MONSTER = 17,
    ITEM_DROP = 19,
    ITEM_TEMPLATE = 25,
    CATALOG_MONSTER = 26,
    LIST_SKILL = 29,
    SET_EXP = 30,
    LEVEL_UP = 33,
    SKILL_COOLDOWN = 50,
    -- QUEST_LIST = 51,
    INFO_FROM_SERVER = 37,
    DIE_PLAYER = 41,
    EFF_PLUS_TIME = 50,
    INFO_EASY_SERVER = 53,
    UPDATE_STATUS_AREA = 54,
    LIST_SERVER = 56,
    LIST_PLAYER_PK = 57,
    PLAYER_SUCKHOE = 59,
    NAME_SERVER = 61,
    X2_XP = 62,
    DELETE_RMS = 63,
    HELP_FROM_SERVER = 64,
    UPDATE_HP_NPC = 70,
    SHOW_NUM_EFF = 74,
    EFF_SERVER = 75,
    EFF_WEATHER = 76,
    OPEN_BOX = 78,
    PET_ATTACK = 84,
    MONSTER_DETONATE = 85,
    MONSTER_SKILL_INFO = 86,
    PET_GAIN_XP = 87,
    REMOVE_ACTOR = 90,

    -- Special Cmds
    MONSTER_CAPCHAR = -28,
    DYNAMIC_MENU = -30,
    DIALOG_MORE_OPTION_SERVER = -31,
    DIALOG_SERVER = -32,
    NEW_NPC_INFO = -44,
    LOAD_IMAGE_DATA_AUTO_EFF = -49,
    NPC_BIG = -50,
    LOAD_IMAGE = -51,
    LOAD_IMAGE_DATA_PART_CHAR = -52,
    NAP_TIEN = -53,
    CHECK_UPDATE_DATA = -54,
    NAP_DIAMOND = -56,
    UPDATE_DATA = -57,
    GOOGLE_PURCHASE_MESSAGE = -75,
    NOKIA_PURCHASE_MESSAGE = -76,
    MINI_GAME = -91,
    USE_ITEM_ARENA = -92,
    IN_APP_PURCHASE = -93,
    STATUS_ARENA = -94,
    MARKKILLER = -95,
    SERVER_ADD_NPC = -96,
    USE_MOUNT = -97,
    USE_SHIP = -98,
    LAST_LOGIN = -99,
    KHAM_NGOC = -100,
    COMPETITION = -101,
    MUA_BAN = -102,
    INFO_MI_NUONG = -103,
    UPDATE_INFO_CLAN_HOLD = -104,
    HOP_RAC = -105,
    GET_MATERIAL_TEMPLATE = -106,
    USE_MATERIAL = -107,
    FILL_REC_UPDATE_TIME = -108,
    GET_NAP_STORE_APPLE = -109,


}

function Cmd.getName(value)
    for name, id in pairs(Cmd) do if type(id) == "number" and id == value then return name end end
    return tostring(value)
end

-- Return the table (for module usage)
return Cmd
