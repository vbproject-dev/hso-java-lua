package game.afk;

public enum AfkBotState {
    IDLE,    // diam, scan musuh
    PATROL,  // jalan keliling area spawn
    CHASE,   // mengejar target
    ATTACK,  // dalam jangkauan — serang target
    RETURN   // kembali ke spawn
}