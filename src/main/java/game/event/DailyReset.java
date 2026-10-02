package game.event;

import client.Player;
import core.SQL;
import game.map.GameMap;
import org.json.simple.JSONArray;
import utils.SQLHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyReset {
    private static LocalDate lastResetDate = LocalDate.now();


    /**
     * FIX: dulu tidak pernah dipanggil dari mana pun (dead code) -- reset harian
     * malah dipicu ServerManager.serverTick() lewat kondisi "hour==0 && min==0
     * && sec==1", yang cuma cocok PERSIS di detik 00:00:01 sekali sehari, tanpa
     * catch-up. Kalau satu eksekusi serverTick() saja telat lewat 1 detik pas
     * lewat tengah malam (GC pause, DB lambat, dll), jendela itu kelewat dan
     * reset harian (termasuk jatah ambil daily quest) tidak jalan sama sekali
     * hari itu. Method ini dipakai ServerManager sebagai gantinya: dipanggil
     * tiap tick (murah, cuma bandingkan tanggal), otomatis nangkep pergantian
     * hari kapanpun tick berikutnya jalan -- tidak butuh detik yang presisi.
     *
     * @return true kalau baru saja melakukan reset (dipakai ServerManager untuk
     *         tahu kapan perlu jalankan pekerjaan tambahan per-player yang tidak
     *         termasuk resetAll(), seperti ip_create_char.clear() dan
     *         p.change_new_date()).
     */
    public static boolean checkMidnightReset() {
        LocalDate currentDate = LocalDate.now();

        // Check if the date has changed (passed midnight)
        if (!currentDate.equals(lastResetDate)) {
            System.out.println("Midnight passed! Resetting game data...");
            resetAll();
            lastResetDate = currentDate;
            return true;
        }
        return false;
    }
    public static void resetAll() {
        // Reset Daily Rewards
        SQLHelper.
                update("user_reward")
                .set("claimed_by", "[]")
                .where("name", "daily_login")
                .execute();

        // Reset Daily Quest (tabel lama)
        SQLHelper.delete()
                .from("daily_quest")
                .execute();

        // Reset quest_daily semua player online
        resetOnlinePlayerDailyQuest();

    }

    /**
     * Reset quest harian untuk semua player yang sedang online.
     * Player offline akan di-reset saat login berikutnya karena kolom quest_daily
     * direset via SQL di DB.
     */
    private static void resetOnlinePlayerDailyQuest() {
        try {
            // Reset player online
            for (GameMap[] maps : GameMap.entrys) {
                if (maps == null) continue;
                for (GameMap map : maps) {
                    if (map == null || map.players == null) continue;
                    for (Player p : map.players) {
                        if (p != null && p.quest_daily != null) {
                            p.quest_daily[0] = -1;  // target_id
                            p.quest_daily[1] = -1;  // difficulty
                            p.quest_daily[2] = 0;   // progress
                            p.quest_daily[3] = 0;   // target
                            p.quest_daily[4] = 5;  // sisa ambil (reset ke 5)
                            p.quest_daily[5] = -1;  // quest_type
                        }
                    }
                }
            }

            // Reset kolom quest_daily di DB untuk semua player (termasuk offline)
            SQLHelper.update("player")
                    .set("quest_daily", "[-1,-1,0,0,5,-1]")
                    .execute();

            System.out.println("[DailyReset] quest_daily semua player berhasil direset.");
        } catch (Exception e) {
            System.err.println("[DailyReset] Error reset quest_daily: " + e.getMessage());
        }
    }


}



