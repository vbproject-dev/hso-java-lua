package game.event.kemerdekaan;

import core.SQL;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Konfigurasi satu wave monster di event wave-based (mis. Persimpangan Kematian).
 *
 * Setiap wave punya:
 *  - waveNumber  : nomor urut wave (1-based)
 *  - mobTemplateId : ID template monster (dari tabel monster_template / MobTemplate)
 *  - mobCount    : jumlah monster yang dispawn per wave
 *  - mobHp       : HP tiap monster
 *  - mobDamage   : damage tiap monster ke kristal (mob biasa) / ke player (boss) per tick serangan
 *  - mobDef      : defense tiap monster (dipakai MainObject.setBaseDefense() saat spawn;
 *                  dampaknya di getDefBase() = mobDef * level — lihat MobInMap.getDefBase())
 *  - spawnX/Y    : koordinat spawn monster (dalam pixel = tile * 24)
 *  - isBossWave  : true = wave boss (isATK aktif, AI bawaan nyerang player).
 *                  false = mob biasa (fokus nyerang kristal, tidak menyerang player).
 *
 * ── Konfigurasi dari Database ────────────────────────────────────────────────
 * Wave TIDAK LAGI harus di-hardcode di kode Java. GM bisa atur mob biasa
 * maupun boss langsung lewat tabel `event_wave_config`, per event (dibedakan
 * lewat kolom event_key), tanpa perlu rebuild/redeploy server.
 *
 * Skema tabel:
 * CREATE TABLE `event_wave_config` (
 *   `id`              INT AUTO_INCREMENT PRIMARY KEY,
 *   `event_key`       VARCHAR(50) NOT NULL COMMENT 'Kode event, contoh: PERSIMPANGAN_KEMATIAN',
 *   `wave_number`     INT NOT NULL COMMENT 'Urutan wave, mulai dari 1',
 *   `mob_template_id` SMALLINT NOT NULL COMMENT 'ID di monster_template (mob biasa ATAU boss)',
 *   `mob_count`       INT NOT NULL DEFAULT 1,
 *   `mob_hp`          INT NOT NULL,
 *   `mob_damage`      INT NOT NULL,
 *   `mob_def`         INT NOT NULL DEFAULT 0 COMMENT 'Defense mob, dipakai MainObject.setBaseDefense()',
 *   `spawn_x`         SMALLINT NOT NULL DEFAULT 0,
 *   `spawn_y`         SMALLINT NOT NULL DEFAULT 0,
 *   `is_boss_wave`    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=boss (nyerang player), 0=mob biasa (nyerang kristal)',
 *   `active`          TINYINT(1) NOT NULL DEFAULT 1,
 *   UNIQUE KEY `uq_event_wave` (`event_key`, `wave_number`),
 *   INDEX `idx_event_key` (`event_key`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 *
 * Cara pakai: isi/edit baris di tabel ini (boleh campur is_boss_wave=0 dan
 * =1 dalam 1 event), lalu panggil PersimpanganKematian.adminReloadWaves()
 * (lihat menu GM "Reload Wave Event") supaya perubahan langsung kepakai
 * tanpa restart server. Kalau tabel kosong/gagal diakses, caller (event)
 * otomatis fallback ke daftar wave default bawaan kode.
 */
@Slf4j
public class WaveConfig {

    public final int waveNumber;
    public final short mobTemplateId;
    public final int mobCount;
    public final int mobHp;
    public final int mobDamage;
    public final int mobDef;
    public final short spawnX;
    public final short spawnY;
    public final boolean isBossWave;

    public WaveConfig(int waveNumber, short mobTemplateId, int mobCount,
                      int mobHp, int mobDamage, int mobDef,
                      short spawnX, short spawnY,
                      boolean isBossWave) {
        this.waveNumber   = waveNumber;
        this.mobTemplateId = mobTemplateId;
        this.mobCount     = mobCount;
        this.mobHp        = mobHp;
        this.mobDamage    = mobDamage;
        this.mobDef       = mobDef;
        this.spawnX       = spawnX;
        this.spawnY       = spawnY;
        this.isBossWave   = isBossWave;
    }

    /**
     * Ambil daftar wave (mob biasa maupun boss) untuk 1 event dari tabel
     * `event_wave_config`, diurutkan berdasarkan wave_number.
     *
     * @param eventKey kode event, mis. "PERSIMPANGAN_KEMATIAN"
     * @return list wave aktif (active=1) sesuai konfigurasi di DB, atau list
     *         kosong kalau tabel belum ada baris/gagal diakses — caller
     *         WAJIB fallback ke default hardcoded kalau hasilnya kosong,
     *         supaya event tidak pernah gagal total gara-gara DB belum
     *         di-setup / lagi bermasalah.
     */
    public static List<WaveConfig> loadFromDB(String eventKey) {
        try {
            List<WaveConfig> list = SQL.gI().selectList(
                "SELECT wave_number, mob_template_id, mob_count, mob_hp, mob_damage, mob_def, " +
                "spawn_x, spawn_y, is_boss_wave FROM `event_wave_config` " +
                "WHERE `event_key` = ? AND `active` = 1 ORDER BY `wave_number` ASC",
                rs -> new WaveConfig(
                    rs.getInt("wave_number"),
                    rs.getShort("mob_template_id"),
                    rs.getInt("mob_count"),
                    rs.getInt("mob_hp"),
                    rs.getInt("mob_damage"),
                    rs.getInt("mob_def"),
                    rs.getShort("spawn_x"),
                    rs.getShort("spawn_y"),
                    rs.getBoolean("is_boss_wave")
                ),
                eventKey
            );
            return list != null ? list : List.of();
        } catch (Exception e) {
            // Tabel belum di-migrate / DB lagi down / dll — jangan sampai
            // bikin event gagal start, cukup log & biarkan caller fallback.
            log.error("Gagal load event_wave_config untuk event_key={}", eventKey, e);
            return List.of();
        }
    }
}
