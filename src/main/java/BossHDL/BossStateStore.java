package BossHDL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import game.map.MobInMap;

/**
 * BossStateStore — menyimpan status "boss sudah dikalahkan" ke FILE LOKAL
 * (data/boss_kill_state.txt), tanpa menyentuh database, supaya World Boss &
 * Premium Boss TIDAK hidup lagi hanya karena server di-restart di tengah
 * jendela event.
 *
 * Format per baris: boss_key|killed_at|respawn_at|window_key
 * boss_key: "WB:<mobId>:<map>:<area>" (World Boss) / "PB:<mobId>:<map>:<area>" (Premium).
 */
public final class BossStateStore {

    public record State(String key, long killedAt, long respawnAt, String windowKey) {}

    private static final Path FILE = Paths.get("data", "boss_kill_state.txt");
    private static final long KEEP_MS = 7L * 24 * 60 * 60 * 1000;
    private static final Map<String, State> CACHE = new HashMap<>();
    private static boolean loaded = false;

    private BossStateStore() {}

    public static String key(String prefix, MobInMap mob) {
        return prefix + ":" + mob.template.mob_id + ":" + mob.map_id + ":" + mob.zone_id;
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(FILE)) return;
            long now = System.currentTimeMillis();
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\\|", -1);
                if (p.length < 4) continue;
                try {
                    long killed = Long.parseLong(p[1]);
                    if (killed < now - KEEP_MS) continue; // buang data lama
                    CACHE.put(p[0], new State(p[0], killed, Long.parseLong(p[2]), p[3]));
                } catch (NumberFormatException ignore) {}
            }
        } catch (IOException e) {
            System.err.println("[BossState] gagal baca " + FILE + ": " + e);
        }
    }

    private static void flush() {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (State s : CACHE.values()) {
                sb.append(s.key()).append('|').append(s.killedAt()).append('|')
                  .append(s.respawnAt()).append('|').append(s.windowKey()).append('\n');
            }
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[BossState] gagal simpan " + FILE + ": " + e);
        }
    }

    /** Dipanggil saat boss mati. Boss tanpa persistKey diabaikan. */
    public static synchronized void recordKill(MobInMap mob) {
        if (mob == null || mob.persistKey == null) return;
        ensureLoaded();
        CACHE.put(mob.persistKey, new State(mob.persistKey, System.currentTimeMillis(),
                mob.time_back, mob.persistWindow == null ? "" : mob.persistWindow));
        flush();
    }

    /** Salinan semua status tersimpan (dipanggil sekali per event start). */
    public static synchronized Map<String, State> loadAll() {
        ensureLoaded();
        return new HashMap<>(CACHE);
    }
}
