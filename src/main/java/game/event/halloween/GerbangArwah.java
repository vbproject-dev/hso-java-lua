package game.event.halloween;

import game.event.kemerdekaan.PersimpanganState;
import game.event.kemerdekaan.WaveConfig;

import client.Player;
import core.Manager;
import core.Service;
import game.event.GameEvent;
import game.event.GameEventManager;
import game.map.Eff_special_skill;
import game.map.GameMap;
import game.map.MapService;
import game.map.MobInMap;
import model.map.Vgo;
import template.MobTemplate;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import core.Util;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerbangArwah extends GameEvent {

    // ── Konstanta map ─────────────────────────────────────────────────────────
    // GANTI ke id map kosong yang tersedia di server kamu — 118 di sini
    // CUMA PLACEHOLDER, harus beda dari MAP_ID Persimpangan Kematian (117)
    // supaya 2 event ini tidak bentrok kalau kebetulan berjalan bersamaan.
    // Jangan lupa juga daftarkan map ini di GameMap.is_map__load_board_player()
    // (sama seperti map 117), atau kristal & lilin tidak akan terlihat client.
    public static final byte  MAP_ID        = 120;
    public static final byte  ZONE_ID       = 0;
    private static final short PLAYER_SPAWN_X = (short)(10 * 24);
    private static final short PLAYER_SPAWN_Y = (short)(10 * 24);
    private static final int   VILLAGE_MAP   = 1;
    private static final short VILLAGE_X     = 320;
    private static final short VILLAGE_Y     = 320;

    // ── Konstanta timing ──────────────────────────────────────────────────────
    private static final int COUNTDOWN_SEC = 30;  // jeda sebelum wave 1
    private static final int REST_SEC      = 20;  // jeda antar wave

    // ── State ─────────────────────────────────────────────────────────────────
    private PersimpanganState state = PersimpanganState.CLOSED;
    private int timer               = 0;
    private int currentWaveIndex    = 0;

    private final CopyOnWriteArrayList<MobInMap> activeMobs = new CopyOnWriteArrayList<>();
    private KristalArwahEvent kristal;
    private long lastKristalDrainTime = 0; // timer drain HP kristal selama boss hidup (tiap 1 detik)
    private long lastMobMoveTime   = 0; // timer pergerakan mob → kristal (lihat MOB_MOVE_TICK_MS)
    private final java.util.Set<Player> lilinVisualSent = new java.util.concurrent.CopyOnWriteArraySet<>();

    // ── Konstanta pergerakan mob (logic sama seperti Dungeon.java / "Batu
    // Meteor" dungeon: mob berjalan bertahap tiap tick menuju target, lalu
    // begitu sudah cukup dekat berhenti & mulai menyerang) ─────────────────────
    private static final int MOB_MOVE_SPEED    = 32;   // px yang ditempuh tiap tick pergerakan (≈1.3 tile)
    private static final long MOB_MOVE_TICK_MS = 400;  // interval pergerakan (lebih sering = lebih halus)
    private static final int MOB_ATTACK_RANGE  = 48;   // px, jarak dari kristal supaya mob dianggap "sampai" & mulai nyerang (2 tile)

    // ── Konstanta skill area boss (dramatis, HP kritis) ──────────────────────
    private static final int BOSS_WIPE_HP_PCT_1  = 20;   // trigger pertama: HP boss <= 20%
    private static final int BOSS_WIPE_HP_PCT_2  = 10;   // trigger kedua: HP boss <= 10%
    private static final long BOSS_WIPE_WARNING_MS = 3000; // jeda peringatan sebelum skill benar2 membunuh (dramatis)

    private boolean bossSkill20Used   = false;
    private boolean bossSkill10Used   = false;
    private long    pendingBossWipeAt = 0; // 0 = tidak ada wipe terjadwal; selain itu = timestamp eksekusi

    // ── HP Kristal per wave ────────────────────────────────────────────────────
    // Semua wave sekarang world boss saja (2 boss/wave, bukan lagi puluhan mob
    // biasa), dan kristal TIDAK lagi diserang lewat MainObject.attack() mob
    // biasa tiap 3 detik — selama minimal 1 boss masih hidup, HP kristal
    // otomatis berkurang KRISTAL_DRAIN_PER_TICK tiap detik (lihat
    // drainKristalWhileBossAlive()). Nilai di bawah diturunkan drastis dari
    // versi lama (120jt) supaya seimbang dengan drain rate baru yang jauh
    // lebih lambat. Sesuaikan lagi sesuai lama rata-rata 1 wave boss bertahan.
    private static final int KRISTAL_HP = 2_000_000;

    // Damage flat ke kristal tiap detik, SELAMA minimal 1 world boss masih
    // hidup di wave berjalan (tidak peduli jumlah/posisi boss).
    private static final int KRISTAL_DRAIN_PER_TICK = 1000;
    private static final long KRISTAL_DRAIN_INTERVAL_MS = 1000;

    // ── Konfigurasi wave ──────────────────────────────────────────────────────
    /**
     * Kode event dipakai sebagai key untuk baca konfigurasi wave dari tabel
     * `event_wave_config` (lihat WaveConfig.loadFromDB()). SENGAJA disamakan
     * dengan nama event yang dikirim ke constructor GameEvent di bawah,
     * supaya 1 baris di tabel event_wave_config jelas milik event yang mana.
     */
    private static final String EVENT_KEY = "GERBANG_ARWAH";

    /**
     * WaveConfig(waveNumber, mobTemplateId, mobCount, mobHp, mobDamage, mobDef, spawnX, spawnY, isBossWave)
     *
     * Ini HANYA fallback bawaan kode — dipakai kalau tabel `event_wave_config`
     * belum diisi/gagal diakses saat event mulai (lihat loadWaves()). Cara
     * normal mengatur wave sekarang lewat DB, BUKAN edit konstanta ini:
     * GM bisa isi baris di `event_wave_config` (mob biasa ATAUPUN boss,
     * boleh dicampur per wave lewat kolom is_boss_wave), lalu reload lewat
     * menu GM "Reload Wave Event" (adminReloadWaves()) tanpa restart server.
     *
     * Isi default di bawah masih mengikuti setup lama: semua wave world boss
     * (2 boss/wave). spawnX/Y TIDAK DIPAKAI untuk penempatan boss — boss
     * selalu spawn dekat kristal (lihat BOSS_SPAWN_X/Y di bawah), jadi
     * nilainya cuma placeholder/kompatibilitas. Untuk mob biasa (is_boss_wave
     * = 0 di DB), spawnX/Y JUGA tidak dipakai — mob biasa spawn dari
     * SPAWN_POINTS gerbang (lihat startWave()).
     *
     * mobTemplateId diambil dari monster_template yang typeMove=10 (AI world
     * boss) untuk wave boss, SENGAJA beda dari ID yang dipakai di tabel
     * boss_premium (233 Rock Joe, 235 Dagnu, 236 Dagon, 238 Blink) supaya
     * boss event ini tidak numpuk/duplikat sama world boss premium map
     * 111-114:
     *   Wave 1 = 221 Malaikat Maut
     *   Wave 2 = 223 Kunti Mantan
     *   Wave 3 = 226 Goblin Etanol
     *   Wave 4 = 229 Kecoa
     *   Wave 5 (final) = 234 Dagan
     * mobHp/mobDamage/mobDef di bawah TETAP dipakai (override HP/damage/def
     * bawaan template saat spawn — lihat startWave() & MainObject.setBaseDefense()),
     * jadi angka hp/dame/def asli di monster_template untuk ID-ID ini tidak
     * berpengaruh ke event ini (berlaku sama untuk baris yang datang dari DB).
     */
    private static final List<WaveConfig> DEFAULT_WAVES = List.of(
        new WaveConfig(1, (short)221, 2,  3_000_000,  30_000,  5_000, (short)(25*24), (short)(15*24), true),
        new WaveConfig(2, (short)223, 2,  5_000_000,  50_000,  8_000, (short)(25*24), (short)(15*24), true),
        new WaveConfig(3, (short)226, 2,  8_000_000,  80_000, 12_000, (short)(25*24), (short)(15*24), true),
        new WaveConfig(4, (short)229, 2, 12_000_000, 120_000, 16_000, (short)(25*24), (short)(15*24), true),
        new WaveConfig(5, (short)234, 2, 20_000_000, 200_000, 25_000, (short)(25*24), (short)(15*24), true)
    );

    /**
     * Daftar wave yang benar-benar dipakai saat ini. Diisi dari DB (lihat
     * loadWaves()) tiap kali event mulai (onStart()) supaya perubahan yang
     * GM lakukan di tabel event_wave_config sebelum event jalan otomatis
     * kepakai — tidak perlu restart server. Fallback ke DEFAULT_WAVES kalau
     * DB kosong/gagal.
     */
    private List<WaveConfig> WAVES = DEFAULT_WAVES;

    // ── Titik spawn gerbang persimpangan ─────────────────────────────────────
    // Diambil dari 3 tanda merah di map (Utara/Barat/Timur menuju persimpangan).
    // {x, y, orientation}: orientation menentukan arah formasi mob mengular
    // supaya sejajar jalan (bukan malah nembus pohon di pinggir jalan):
    //   0 = jalur vertikal (Utara)  -> lebar formasi di X, memanjang ke atas (Y berkurang)
    //   1 = jalur horizontal (Barat)-> lebar formasi di Y, memanjang ke kiri  (X berkurang)
    //   2 = jalur horizontal (Timur)-> lebar formasi di Y, memanjang ke kanan (X bertambah)
    // GANTI koordinat ini kalau posisi jalan di map kamu beda.
    private static final short[][] SPAWN_POINTS = {
        {(short) (19 * 24), (short) (3 * 24), 0},   // Utara
        {(short) (2 * 24),  (short) (20 * 24), 1},  // Barat
        {(short) (38 * 24), (short) (20 * 24), 2},  // Timur
    };
    private static final int SPAWN_FORMATION_WIDTH = 5; // lebar formasi (tile) tegak lurus jalan

    // ── Titik spawn khusus BOSS ──────────────────────────────────────────────
    // Boss TIDAK spawn dari gerbang jauh seperti mob biasa — langsung muncul
    // dekat kristal (5 tile di utara kristal) supaya begitu wave boss mulai,
    // dia cuma butuh beberapa langkah buat "sampai" dan momen-nya lebih
    // dramatis/langsung, bukan jalan jauh dari luar map.
    private static final short BOSS_SPAWN_X = KristalArwahEvent.KRISTAL_X;
    private static final short BOSS_SPAWN_Y = (short) (KristalArwahEvent.KRISTAL_Y - 5 * 24);
    // Jarak antar boss (px) supaya 2 boss dalam 1 wave tidak spawn dobel di titik sama.
    private static final short BOSS_SPAWN_SPREAD = 2 * 24;
    // SET HADIAH KILL MOB WAVE — reuse currency Jimat Malam Suro yang sama
    // dipakai MalamSuroAmbush/NpcDukun/NpcPocong (MalamSuroAmbush.JIMAT_ID),
    // biar semua sub-mekanik Halloween pakai 1 currency yang sama, bukan
    // bikin currency baru lagi per mekanik.
    private static final int   KOIN_DROP_MIN        = 2;   // jimat per mob biasa
    private static final int   KOIN_DROP_MAX        = 5;
    private static final int   KOIN_DROP_MIN_BOSS    = 15;  // jimat per mob boss wave
    private static final int   KOIN_DROP_MAX_BOSS    = 25;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GerbangArwah() {
        super(
            "GERBANG_ARWAH",
            EnumSet.allOf(DayOfWeek.class),
            List.of(new TimeEvent(LocalTime.of(12,35), LocalTime.of(15, 0)))
        );
    }

    // ── GameEvent lifecycle ───────────────────────────────────────────────────

    @Override
    protected boolean isEnabled() {
        return Manager.gI().event_gerbang_arwah;
    }

    @Override
    protected void onStart() {
        // Cek apakah event Halloween sedang aktif — KECUALI kalau GM yang
        // memaksa start manual (lewat adminForceStart()/forceStart()), maka
        // syarat periode 25 Okt-1 Nov ini diabaikan supaya GM bisa
        // mengaktifkan Gerbang Arwah kapan saja untuk testing/acara.
        HalloweenEvent halloween = GameEventManager.gI().getEvent(HalloweenEvent.class);
        if (!isManualOverride() && (halloween == null || halloween.shouldBeRemoved())) {
            // Gerbang Arwah hanya jalan otomatis selama event Halloween aktif
            return;
        }

        loadWaves();

        state            = PersimpanganState.COUNTDOWN;
        timer            = COUNTDOWN_SEC;
        currentWaveIndex = 0;
        activeMobs.clear();
        lastKristalDrainTime = 0;

        // Inisialisasi kristal
        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps != null && maps.length > ZONE_ID) {
            GameMap map = maps[ZONE_ID];
            kristal = new KristalArwahEvent(this, map, KRISTAL_HP);
            // FIX: JANGAN daftarkan ke Boss_entrys — kalau didaftarkan, engine
            // otomatis menganggap kristal sebagai monster biasa (lewat
            // MapService.update_inside_player()) dan ikut mengirim nameplate+HP
            // bar bawaannya sendiri ("Mining Crystal Lv80"), NUMPUK persis di
            // koordinat yang sama dengan visual lilin manual di bawah
            // ("Lilin Malam Suro") -> muncul dobel di client.
            // Serangan mob ke kristal (MainObject.attack()) tidak butuh
            // keanggotaan Boss_entrys sama sekali (pakai referensi objek
            // langsung), dan visibility/nama/HP kristal sudah sepenuhnya
            // ditangani manual lewat sendLilinVisual()/sendLilinInfo() di
            // bawah, jadi registrasi ke Boss_entrys memang tidak diperlukan.

            // Spawn visual "lilin ritual" (dekoratif) ke semua player
            // yang sudah ada di map saat event mulai.
            lilinVisualSent.clear();
            for (Player p : map.players) {
                try {
                    kristal.sendLilinFall(p); // spawn + animasi jatuh dari atas
                    lilinVisualSent.add(p);
                } catch (IOException ignore) {}
                // Nyalakan efek cuaca ambient untuk player yang sudah ada di map
                // saat event mulai (player yang masuk belakangan otomatis dapat
                // lewat MessageHandler case 12 saat mereka pindah zona ke map ini).
                sendWeather(p, (byte) 4);
            }
        }

        broadcast(
            "[Malam Suro] DIMULAI! Semua Pemberani, lindungi Api Ritual!" +
            " Bergabunglah ke NPC Penjaga Gerbang Arwah di desa. Wave dimulai dalam " +
            COUNTDOWN_SEC + " detik!"
        );
    }

    @Override
    protected void onEnd() {
        if (state == PersimpanganState.WAVE || state == PersimpanganState.REST
                || state == PersimpanganState.COUNTDOWN) {
            // Event selesai sebelum waktunya (misal server jam sudah lewat)
            broadcast("[Malam Suro] Waktu habis, event dihentikan.");
            clearMobsFromMap();
            teleportAllToVillage();
        }
        cleanup();
    }

    @Override
    protected void onUpdate() {
        if (!isActive()) return;

        // Jika event halloween tidak aktif, skip — kecuali sedang di-paksa GM
        HalloweenEvent halloween = GameEventManager.gI().getEvent(HalloweenEvent.class);
        if (!isManualOverride() && (halloween == null || halloween.shouldBeRemoved())) return;

        // Kirim visual lilin ke player yang baru masuk map setelah event dimulai
        if (kristal != null) {
            GameMap[] maps0 = GameMap.getMapById(MAP_ID);
            if (maps0 != null && maps0.length > ZONE_ID) {
                for (Player p : maps0[ZONE_ID].players) {
                    if (!lilinVisualSent.contains(p)) {
                        try {
                            kristal.sendLilinFall(p); // spawn + animasi jatuh dari atas
                            lilinVisualSent.add(p);
                        } catch (IOException ignore) {}
                    }
                }
            }
        }

        switch (state) {

            case COUNTDOWN -> {
                timer--;
                if (timer == 15 || timer == 5) {
                    broadcastToMap("Gerombolan Arwah pertama dimulai dalam " + timer + " detik! Siapkan dirimu!");
                }
                if (timer <= 0) startWave();
            }

            case WAVE -> {
                for (MobInMap mob : activeMobs) {
                    if (mob.isdie) {
                        rewardKillers(mob);
                    }
                }
                // Mob yang sudah mati dihapus dari list aktif
                activeMobs.removeIf(m -> m.isdie);

                if (activeMobs.isEmpty()) {
                    currentWaveIndex++;
                    if (currentWaveIndex >= WAVES.size()) {
                        triggerVictory(halloween);
                    } else {
                        startRest();
                    }
                }

                // ── Pergerakan mob menuju kristal (logic sama seperti Dungeon.java:
                // mob_act()/update_mob() di "dungeon Batu Meteor" — mob jalan
                // bertahap tiap tick ke arah target, berhenti begitu cukup dekat) ──
                moveMobsTowardKristal();

                // ── Skill area boss saat HP kritis (20% & 10%) — dramatis: kasih
                // peringatan dulu beberapa detik, lalu bunuh semua player di map. ──
                checkBossEnrageSkill();

                // ── Drain HP kristal selama world boss masih hidup ──────────
                // Semua mob sekarang world boss — tidak ada lagi mob biasa yang
                // "jalan mendekat lalu menyerang kristal" lewat MainObject.attack().
                // Selama minimal 1 boss di wave ini masih hidup (tidak peduli
                // posisi/jaraknya ke kristal), HP kristal otomatis berkurang
                // KRISTAL_DRAIN_PER_TICK tiap KRISTAL_DRAIN_INTERVAL_MS.
                drainKristalWhileBossAlive();
            }

            case REST -> {
                timer--;
                if (timer <= 0) startWave();
            }

            case VICTORY, DEFEAT, CLOSED -> { /* tidak ada update */ }
        }
    }

    // ── Konfigurasi wave dari DB ──────────────────────────────────────────────

    /**
     * Baca ulang daftar wave (mob biasa & boss) dari tabel `event_wave_config`
     * untuk EVENT_KEY ini. Kalau tabel kosong atau gagal diakses, tetap pakai
     * DEFAULT_WAVES supaya event tidak pernah gagal start gara-gara DB belum
     * di-setup / lagi bermasalah.
     */
    private void loadWaves() {
        List<WaveConfig> fromDb = WaveConfig.loadFromDB(EVENT_KEY);
        WAVES = fromDb.isEmpty() ? DEFAULT_WAVES : fromDb;
    }

    /**
     * Dipanggil GM lewat menu "Reload Wave Event" supaya perubahan yang baru
     * diisi/diedit di tabel event_wave_config langsung kepakai tanpa perlu
     * restart server. Aman dipanggil kapan saja — kalau event sedang
     * berjalan (WAVE/REST/COUNTDOWN), wave yang SEDANG berlangsung tidak
     * diubah di tengah jalan; perubahan baru berlaku mulai wave berikutnya
     * atau saat event di-start ulang.
     *
     * @return pesan status untuk ditampilkan ke GM.
     */
    public String adminReloadWaves() {
        List<WaveConfig> fromDb = WaveConfig.loadFromDB(EVENT_KEY);
        if (fromDb.isEmpty()) {
            WAVES = DEFAULT_WAVES;
            return "Tabel event_wave_config kosong/gagal dibaca untuk '" + EVENT_KEY
                    + "' — memakai " + DEFAULT_WAVES.size() + " wave default bawaan kode.";
        }
        WAVES = fromDb;
        return "Berhasil reload " + WAVES.size() + " wave dari database untuk '" + EVENT_KEY + "'.";
    }

    // ── Transisi state ────────────────────────────────────────────────────────

    private void startWave() {
        if (currentWaveIndex >= WAVES.size()) return;

        WaveConfig cfg = WAVES.get(currentWaveIndex);
        state = PersimpanganState.WAVE;
        activeMobs.clear();

        // Reset status skill area boss (20%/10% HP) tiap kali wave boss dimulai
        // ulang, supaya tidak ke-skip kalau event direset/di-restart.
        if (cfg.isBossWave) {
            bossSkill20Used  = false;
            bossSkill10Used  = false;
            pendingBossWipeAt = 0;
        }

        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null || maps.length <= ZONE_ID) return;
        GameMap map = maps[ZONE_ID];

        // Lookup template sekali saja di luar loop (dipanggil 100-150x per
        // wave sekarang, lookup di dalam loop cuma buang-buang waktu karena
        // hasilnya sama terus untuk 1 wave).
        MobTemplate tpl = MobTemplate.getMob(cfg.mobTemplateId);
        if (tpl == null) {
            // FIX: dulu di sini cuma `return` diam-diam kalau mob_template_id
            // di wave config tidak ketemu di tabel `monster_template` — wave
            // jadi gagal total tanpa mob yang muncul SAMA SEKALI, dan tidak
            // ada log/error apapun buat GM debug kenapa. Sekarang dikasih log
            // + notice ke map biar ketauan.
            log.error("[GerbangArwah] Wave {} gagal start: mob_template_id={} "
                    + "tidak ditemukan di tabel `monster_template`. Cek data mob-nya.",
                    cfg.waveNumber, cfg.mobTemplateId);
            broadcastToMap("[Malam Suro] ERROR: Wave " + cfg.waveNumber
                    + " gagal dimulai (mob_template_id=" + cfg.mobTemplateId
                    + " belum terdaftar). Hubungi admin.");
            return;
        }

        // Mob dibagi rata bergiliran (round-robin) ke 3 titik gerbang
        // (Utara/Barat/Timur) supaya nyerbu dari 3 arah sekaligus, bukan
        // numpuk di 1 titik. pointLocalIdx melacak indeks lokal tiap titik
        // biar formasinya rapi per titik (bukan ikut index global i).
        int numPoints = SPAWN_POINTS.length;
        int[] pointLocalIdx = new int[numPoints];

        for (int i = 0; i < cfg.mobCount; i++) {
            short mobX, mobY;

            if (cfg.isBossWave) {
                // Boss langsung spawn dekat kristal, bukan dari gerbang jauh.
                // Digeser sedikit tiap boss (kelipatan BOSS_SPAWN_SPREAD) supaya
                // 2 boss dalam 1 wave tidak numpuk persis di koordinat yang sama.
                mobX = (short) (BOSS_SPAWN_X + i * BOSS_SPAWN_SPREAD);
                mobY = BOSS_SPAWN_Y;
            } else {
                int group = i % numPoints;
                int localIdx = pointLocalIdx[group]++;
                short[] sp = SPAWN_POINTS[group];
                int baseX = sp[0], baseY = sp[1], orientation = sp[2];
                int row = localIdx / SPAWN_FORMATION_WIDTH;   // makin jauh dari gerbang
                int col = localIdx % SPAWN_FORMATION_WIDTH;   // posisi melintang jalan
                int lateral = (col - SPAWN_FORMATION_WIDTH / 2) * 24;

                switch (orientation) {
                    case 0 -> { mobX = (short) (baseX + lateral); mobY = (short) (baseY - row * 24); }
                    case 1 -> { mobY = (short) (baseY + lateral); mobX = (short) (baseX - row * 24); }
                    default -> { mobY = (short) (baseY + lateral); mobX = (short) (baseX + row * 24); }
                }
            }

            MobInMap mob = new MobInMap();
            // Base 20_000, offset 10_000 per wave -> muat sampai 10.000 mob per
            // wave tanpa tabrakan ID (mobCount lama cuma disediakan slot 100).
            mob.objectId       = 20_000 + (currentWaveIndex * 10_000) + i;
            mob.template       = tpl;
            mob.name           = cfg.isBossWave ? "BOSS " + tpl.name : tpl.name;
            mob.level          = tpl.level;
            mob.hp             = cfg.mobHp;
            mob.maxHp          = cfg.mobHp;
            mob.x              = mobX;
            mob.y              = mobY;
            mob.map_id         = MAP_ID;
            mob.zone_id        = ZONE_ID;
            mob.setBaseDamage(cfg.mobDamage);
            mob.setBaseDefense(cfg.mobDef);
            mob.setBoss(cfg.isBossWave);
            mob.is_boss_active = true;
            mob.isdie          = false;
            // Pembagian peran: mob biasa FOKUS nyerang kristal (isATK=false ->
            // tidak nyerang player lewat AI bawaan MobInMap.update(), tapi tetap
            // kena serang kristal lewat panggilan manual di WAVE case onUpdate()).
            // Boss FOKUS nyerang player (isATK=true -> aktif lewat AI bawaan),
            // dan dikecualikan dari serangan ke kristal (lihat pengecekan
            // mob.isBoss() di loop serangan kristal pada onUpdate()).
            mob.isATK          = cfg.isBossWave;

            map.Boss_entrys.add(mob);
            activeMobs.add(mob);
        }

        String label = cfg.isBossWave
            ? "BOSS WAVE! " + cfg.mobCount + " bos menyerang Api Ritual!"
            : "Wave " + cfg.waveNumber + "/" + WAVES.size() +
              " — " + cfg.mobCount + " monster menyerang!";

        broadcastToMap("[Malam Suro] " + label + " Lindungi Api Ritual!");
    }

    private void startRest() {
        state = PersimpanganState.REST;
        timer = REST_SEC;
        clearMobsFromMap();

        broadcastToMap(
            "[Malam Suro] Wave selesai! " +
            "Wave " + (currentWaveIndex + 1) + "/" + WAVES.size() +
            " dimulai dalam " + REST_SEC + " detik!"
        );
    }

    // ── Akhir event ───────────────────────────────────────────────────────────

    /**
     * Dipanggil dari KristalArwahEvent.setDie() saat HP kristal mencapai 0
     * (dipicu otomatis oleh MainObject.attack() ketika mob menyerang kristal).
     * Mengunci hadiah Gerbang Arwah untuk SEMUA player (lewat HalloweenEvent.onKristalHancur()).
     */
    public void onKristalHancur() throws IOException {
        if (state == PersimpanganState.DEFEAT) return;
        state = PersimpanganState.DEFEAT;
        clearMobsFromMap();

        // Kunci hadiah di event halloween
        HalloweenEvent halloween = GameEventManager.gI().getEvent(HalloweenEvent.class);
        if (halloween != null) halloween.onKristalHancur();

        // Teleport semua player keluar setelah 5 detik
        new Thread(() -> {
            try { Thread.sleep(5_000); } catch (InterruptedException ignore) {}
            teleportAllToVillage();
            cleanup();
        }).start();
    }

    private void triggerVictory(HalloweenEvent halloween) {
        state = PersimpanganState.VICTORY;
        clearMobsFromMap();

        // Buka hadiah Gerbang Arwah untuk semua player
        if (halloween != null) halloween.onKristalSelamat();

        // Teleport semua player pulang setelah 10 detik
        new Thread(() -> {
            try { Thread.sleep(10_000); } catch (InterruptedException ignore) {}
            teleportAllToVillage();
            cleanup();
        }).start();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Teleport semua player yang ada di MAP_ID ke desa.
     * Tidak perlu daftar — siapapun yang ada di map ikut dipulangkan.
     */
    private void teleportAllToVillage() {
        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null || maps.length <= ZONE_ID) return;
        GameMap map = maps[ZONE_ID];

        for (Player p : new CopyOnWriteArrayList<>(map.players)) {
            sendWeather(p, (byte) 0); // matikan cuaca sebelum player pindah map
            try {
                p.changeMap(p, Vgo.create(p, VILLAGE_MAP, VILLAGE_X, VILLAGE_Y));
            } catch (IOException ignore) {}
        }
    }

    private void clearMobsFromMap() {
        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null || maps.length <= ZONE_ID) return;
        GameMap map = maps[ZONE_ID];

        for (MobInMap mob : activeMobs) {
            mob.isdie          = true;
            mob.is_boss_active = false;
            map.Boss_entrys.remove(mob);
        }
        activeMobs.clear();
    }

    /**
     * Broadcast ke semua player yang sedang ada di MAP_ID (bukan server-wide).
     */
    private void broadcastToMap(String msg) {
        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null || maps.length <= ZONE_ID) return;
        GameMap map = maps[ZONE_ID];

        for (Player p : map.players) {
            try {
                Service.send_notice_nobox_white(p.conn, msg);
            } catch (IOException ignore) {}
        }
    }

    // ── Pergerakan mob menuju kristal ────────────────────────────────────────
    // Logic diadaptasi dari Dungeon.java (mob_act()/update_mob() di dungeon
    // "Batu Meteor"): tiap tick, mob melangkah MOB_MOVE_SPEED px ke arah
    // target (kristal), lalu berhenti begitu jaraknya <= MOB_ATTACK_RANGE.
    // Bedanya dengan Dungeon.java (yang cuma gerak 1 sumbu/gate karena mob
    // selalu spawn segaris dengan target): mob kita spawn dari 3 arah
    // berbeda (Utara/Barat/Timur, lihat SPAWN_POINTS) jadi pergerakannya
    // dihitung 2 sumbu sekaligus (diagonal) memakai vektor arah dx/dy yang
    // dinormalisasi, supaya tetap lurus menuju kristal dari sudut manapun.
    private void moveMobsTowardKristal() {
        if (kristal == null || kristal.isdie || activeMobs.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastMobMoveTime < MOB_MOVE_TICK_MS) return;
        lastMobMoveTime = now;

        for (MobInMap mob : activeMobs) {
            // Boss TETAP jalan mendekat ke kristal (biar sampai di tengah medan
            // pertempuran, dramatis). Jumlah drain HP kristal per tick (angka)
            // tidak bergantung posisi/jarak boss (lihat drainKristalWhileBossAlive()),
            // TAPI animasi serangannya (sendMobAttackKristalEffect()) HANYA
            // ditampilkan untuk mob yang SUDAH SAMPAI (isMobArrivedAtKristal),
            // supaya visualnya tetap konsisten dengan posisi mob di map.
            if (mob.isdie || isMobArrivedAtKristal(mob)) continue;

            double dx = kristal.x - mob.x;
            double dy = kristal.y - mob.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 0.001) continue; // sudah persis di titik yang sama

            double step = Math.min(MOB_MOVE_SPEED, dist);
            mob.x = (short) Math.round(mob.x + (dx / dist) * step);
            mob.y = (short) Math.round(mob.y + (dy / dist) * step);

            broadcastMobPosition(mob);
        }
    }

    /**
     * Kurangi HP kristal sebesar KRISTAL_DRAIN_PER_TICK tiap
     * KRISTAL_DRAIN_INTERVAL_MS, SELAMA minimal 1 world boss di wave
     * berjalan masih hidup (activeMobs semuanya boss sejak semua wave
     * di-set isBossWave=true). Begitu semua boss di wave itu mati, drain
     * otomatis berhenti (activeMobs kosong -> wave berikutnya dimulai).
     * Kalau kristal habis di sini, trigger setDie() secara manual persis
     * seperti yang biasanya dipicu otomatis oleh MainObject.attack().
     *
     * ── Efek visual serangan ────────────────────────────────────────────
     * Selain drain HP (angka), tiap tick di sini JUGA mengirim animasi
     * "mob menyerang lilin" (opcode 10) ke semua player — formatnya
     * di-copy persis dari Dungeon.java mob_act() (dungeon "Batu Meteor"),
     * lihat sendMobAttackKristalEffect(). Cuma mob yang SUDAH SAMPAI di
     * kristal (isMobArrivedAtKristal — hasil dari moveMobsTowardKristal())
     * yang dianimasikan menyerang, supaya visualnya konsisten sama posisi
     * mob di map (bukan mob yang masih jauh tiba-tiba kelihatan nyerang).
     * Total damage yang ditampilkan di animasi dibagi rata ke semua mob
     * yang sedang menyerang, jumlahnya tetap sama dengan dmg aktual di
     * bawah — animasi ini murni tampilan, tidak menambah damage sungguhan.
     */
    private void drainKristalWhileBossAlive() {
        if (kristal == null || kristal.isdie || activeMobs.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastKristalDrainTime < KRISTAL_DRAIN_INTERVAL_MS) return;
        lastKristalDrainTime = now;

        MobInMap boss = findActiveBoss();
        if (boss == null) return; // tidak ada boss hidup -> tidak ada drain

        int dmg = Math.min(KRISTAL_DRAIN_PER_TICK, kristal.hp);
        kristal.hp -= dmg;

        List<MobInMap> attackers = new ArrayList<>();
        for (MobInMap m : activeMobs) {
            if (!m.isdie && isMobArrivedAtKristal(m)) attackers.add(m);
        }
        if (!attackers.isEmpty()) {
            int share = Math.max(1, dmg / attackers.size());
            for (MobInMap attacker : attackers) {
                sendMobAttackKristalEffect(attacker, share, kristal.hp);
            }
        }

        if (kristal.hp <= 0) {
            kristal.setDie(kristal.gameMap, boss); // trigger DEFEAT manual, sama seperti attack()
        } else {
            kristal.broadcastHp();
        }
    }

    /**
     * Kirim animasi "mob menyerang lilin" ke semua player di map. Format
     * paket (opcode 10) DI-COPY PERSIS dari Dungeon.java mob_act() (dungeon
     * "Batu Meteor", lihat blok if(is_atk) di sana):
     *   byte 1, short objectId mob, int HP mob, byte 2, byte 1,
     *   short index target (lilin), int damage, int HP target setelah
     *   kena, byte id skill (6, sama seperti Dungeon.java), byte 0.
     * Target dikirim sebagai KristalArwahEvent.LILIN_VISUAL_ID (bukan
     * kristal.objectId asli) karena itulah objek yang benar-benar dirender
     * client sebagai "Lilin Malam Suro" (lihat sendLilinVisual() di
     * KristalArwahEvent) — kristal.objectId sendiri sengaja tidak pernah
     * di-spawn ke client (lihat komentar FIX di onStart()).
     */
    private void sendMobAttackKristalEffect(MobInMap mob, int dame, int kristalHpAfter) {
        if (kristal == null || kristal.gameMap == null) return;
        try {
            client.io.Message m = new client.io.Message(10);
            m.writer().writeByte(1);
            m.writer().writeShort(mob.objectId);
            m.writer().writeInt(mob.hp);
            m.writer().writeByte(2);
            m.writer().writeByte(1);
            m.writer().writeShort(KristalArwahEvent.LILIN_VISUAL_ID);
            m.writer().writeInt(dame);
            m.writer().writeInt(kristalHpAfter);
            m.writer().writeByte(6); // id skill mob, sama seperti Dungeon.java
            m.writer().writeByte(0);
            for (Player p : kristal.gameMap.players) {
                p.conn.addmsg(m);
            }
            m.cleanup();
        } catch (IOException ignore) {}
    }

    /** TRUE kalau mob sudah cukup dekat kristal untuk mulai menyerang (berhenti jalan). */
    private boolean isMobArrivedAtKristal(MobInMap mob) {
        if (kristal == null) return false;
        long dx = kristal.x - mob.x;
        long dy = kristal.y - mob.y;
        return (dx * dx + dy * dy) <= (long) MOB_ATTACK_RANGE * MOB_ATTACK_RANGE;
    }

    /** Kirim paket posisi terbaru mob ke semua player di map, supaya kelihatan jalan di client. */
    private void broadcastMobPosition(MobInMap mob) {
        if (kristal == null || kristal.gameMap == null) return;
        client.io.Message m = new client.io.Message(4);
        try {
            m.writer().writeByte(1);
            m.writer().writeShort(mob.template.mob_id);
            m.writer().writeShort(mob.objectId);
            m.writer().writeShort(mob.x);
            m.writer().writeShort(mob.y);
            m.writer().writeByte(-1);
            for (Player p : kristal.gameMap.players) {
                p.conn.addmsg(m);
            }
        } catch (IOException ignore) {
        } finally {
            try { m.cleanup(); } catch (IOException ignore) {}
        }
    }

    // ── Skill area boss (dramatis) ────────────────────────────────────────────
    // Saat HP boss menyentuh 20% lalu 10%, boss "murka" dan mengeluarkan skill
    // area yang membunuh SEMUA player di map (dua fase: peringatan dulu 3 detik,
    // baru eksekusi — biar terasa dramatis & player sempat panik/bersiap,
    // bukan instant kill tanpa aba-aba).
    private void checkBossEnrageSkill() {
        if (kristal == null || kristal.gameMap == null) return;

        long now = System.currentTimeMillis();

        // Fase eksekusi: peringatan sudah lewat, waktunya benar-benar membunuh semua.
        if (pendingBossWipeAt != 0) {
            if (now >= pendingBossWipeAt) {
                pendingBossWipeAt = 0;
                executeBossWipe(findActiveBoss());
            }
            return; // selama masih menunggu peringatan, jangan cek threshold lain dulu
        }

        MobInMap boss = findActiveBoss();
        if (boss == null || boss.isdie) return;

        int maxHp = boss.getMaxHP();
        if (maxHp <= 0) return;
        int hpPct = (int) (100L * boss.hp / maxHp);

        if (!bossSkill20Used && hpPct <= BOSS_WIPE_HP_PCT_1) {
            bossSkill20Used = true;
            scheduleBossWipeWarning(BOSS_WIPE_HP_PCT_1);
        } else if (!bossSkill10Used && hpPct <= BOSS_WIPE_HP_PCT_2) {
            bossSkill10Used = true;
            scheduleBossWipeWarning(BOSS_WIPE_HP_PCT_2);
        }
    }

    private MobInMap findActiveBoss() {
        for (MobInMap m : activeMobs) {
            if (!m.isdie && m.isBoss()) return m;
        }
        return null;
    }

    private void scheduleBossWipeWarning(int hpPercentTrigger) {
        pendingBossWipeAt = System.currentTimeMillis() + BOSS_WIPE_WARNING_MS;
        broadcastToMap(
            "☠ BOSS MURKA! HP tersisa " + hpPercentTrigger + "%! " +
            "Serangan Pemusnah akan menghantam SELURUH medan pertempuran dalam " +
            (BOSS_WIPE_WARNING_MS / 1000) + " detik — BERSIAP!!"
        );
    }

    /**
     * Eksekusi skill pemusnah — bunuh semua player di map event ini.
     * Menghormati medali anti-mati (NgocKhaiHoan) sama seperti skill area boss
     * premium (lihat BossHDL.PremiumBossManager.bossAreaSkill), supaya player
     * yang pakai medali tetap terlindungi.
     *
     * FIX PERFORMA + VISUAL: kill (hp/isdie) tetap SYNCHRONOUS di game tick
     * thread supaya state langsung konsisten (tidak nunggu network). Tapi
     * broadcast paket animasi op9 ke SEMUA player dipindah ke background
     * thread — karena map event ini (120) terdaftar di GameMap.is_map__load_board_player(),
     * broadcast paket per-target ini SELALU dikirim ke SEMUA player (skip cek
     * radius), jadi totalnya N x N paket (N = jumlah player di map). Kalau
     * dieksekusi synchronous di game tick thread, ini nge-block event loop
     * server sesaat -> lag/freeze (persis gejala harus keluar-masuk map dulu
     * baru status mati ke-sync). Dipindah ke background thread (pola sama
     * seperti delayed teleport di onKristalHancur()/triggerVictory() di
     * bawah) supaya tick server tetap responsif, TAPI paket tetap sampai ke
     * semua player (termasuk yang nonton dari char lain) — cuma keluar
     * beberapa milidetik lebih lambat, bukan diam sama sekali kayak fix
     * sebelumnya.
     *
     * PENTING: GameMap.players itu ArrayList biasa (bukan thread-safe), jadi
     * broadcast di background thread TIDAK BOLEH iterate map.players yang
     * masih "hidup" langsung (race kalau ada player join/leave barengan).
     * Makanya diambil snapshot (defensive copy) dulu di sini (main thread)
     * sebelum masuk background thread.
     */
    /** Durasi tampilan efek "gosong" (terbakar) di badan player yang kena Serangan Pemusnah. */
    private static final int BOSS_WIPE_BURN_EFF_MS = 3000;

    private void executeBossWipe(MobInMap boss) {
        if (kristal == null || kristal.gameMap == null) return;
        GameMap map = kristal.gameMap;
        template.MainObject caster = (boss != null) ? boss : kristal;

        broadcastToMap("💥 SERANGAN PEMUSNAH BOSS MENGHANTAM SELURUH MEDAN PERTEMPURAN!");

        // Snapshot semua player di map SEKARANG (di main thread) — dipakai
        // baik buat daftar target yang dibunuh maupun daftar penerima
        // broadcast animasi di background thread nanti.
        List<Player> mapSnapshot = new java.util.ArrayList<>(map.players);

        List<Player> targets = new java.util.ArrayList<>();
        for (Player p : mapSnapshot) {
            if (p == null || p.isdie || p.conn == null) continue;
            if (p.getEffectMedal(template.StrucEff.NgocKhaiHoan) != null) continue; // medali anti-mati tetap dihormati
            targets.add(p);
        }
        if (targets.isEmpty()) return;

        // Kill SEKARANG (synchronous) supaya state game langsung konsisten.
        // PENTING: p.setDie() SENDIRI TIDAK KIRIM PAKET APA PUN ke client — lihat
        // MainObject.setDie() (cuma `hp = 0; isdie = true;`, murni flag server-side).
        // Notifikasi "player mati" yang beneran ditampilkan ke client itu paket
        // terpisah: MapService.Player_Die() (opcode 41). Pola resminya (lihat
        // MainObject.attack(), sekitar baris 1017-1035) SELALU pasang setDie()
        // + Player_Die() berbarengan setiap kali target-nya player. Sebelumnya
        // executeBossWipe() cuma manggil setDie() tanpa Player_Die() -> state mati
        // cuma ke-update di server, client gak pernah dikasih tahu -> makanya
        // status "mati" baru kelihatan setelah keluar-masuk map (paksa resync
        // penuh). Efek visual "gosong" juga WAJIB di sini (main/tick thread),
        // BUKAN di background thread di bawah — Eff_special_skill.send_eff_Vip()
        // memanggil MapService.sendMsgPlayerInside() yang iterate gameMap.players
        // (ArrayList biasa, bukan snapshot, bukan thread-safe) secara LANGSUNG.
        // Dipanggil dari background thread bikin race sama main thread (mis. saat
        // player klik Revive / teleport lain yang mengubah gameMap.players di
        // waktu bersamaan) -> exception di tengah loop -> ke-swallow diam-diam ->
        // paket efek gagal terkirim penuh -> client stuck nunggu paket lanjutan
        // yang gak pernah datang (persis gejala freeze/no-damage sampai keluar-
        // masuk map). Makanya semua Eff_special_skill.*/MapService.sendMsgPlayerInside
        // di codebase ini SELALU dipanggil sinkron dari main thread (lihat pola
        // yang sama di PremiumBossManager.applyAreaSkillEffect()).
        for (Player p : targets) {
            try {
                p.hp = 0;
                p.setDie(map, caster);
                MapService.Player_Die(map, p, caster, true);
                Eff_special_skill.send_eff_Vip(p, 13, BOSS_WIPE_BURN_EFF_MS, false);
                p.addEffectMedal(template.StrucEff.BongLua, 0,
                        System.currentTimeMillis() + BOSS_WIPE_BURN_EFF_MS);
            } catch (Exception ignore) {}
        }

        // Broadcast animasi kematian ke SEMUA player (pakai snapshot, bukan
        // map.players langsung) di background thread — biar tidak nge-block
        // game tick thread walau player-nya banyak.
        new Thread(() -> {
            for (Player p : targets) {
                try {
                    // Opcode 9 (Fire_Mob) — animasi skill ke player yang jadi target.
                    client.io.Message m = new client.io.Message(9);
                    m.writer().writeShort(caster.objectId);
                    m.writer().writeByte(14); // index 14 = skill area attack (render animasi penuh)
                    m.writer().writeByte(1);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeInt(0);   // damage ditampilkan 0 karena ini instant-kill, bukan damage biasa
                    m.writer().writeInt(0);   // hp target setelah kena = 0 (sudah di-kill di atas)
                    m.writer().writeByte(1);
                    m.writer().writeByte(0);
                    m.writer().writeInt(0);
                    m.writer().writeInt(caster.hp);
                    m.writer().writeInt(0);
                    m.writer().writeByte(11);
                    m.writer().writeInt(0);

                    for (Player p0 : mapSnapshot) {
                        if (p0 == null || p0.conn == null || !p0.conn.connected) continue;
                        p0.conn.addmsg(m);
                    }
                    m.cleanup();
                } catch (Exception ignore) {}
            }
        }, "GerbangArwah-BossWipe-Broadcast").start();
    }

    private void rewardKillers(MobInMap mob) {
        if (mob.top_dame.isEmpty()) return;

        GameMap[] maps = GameMap.getMapById(MAP_ID);
        if (maps == null || maps.length <= ZONE_ID) return;
        GameMap map = maps[ZONE_ID];

        int min = mob.isBoss() ? KOIN_DROP_MIN_BOSS : KOIN_DROP_MIN;
        int max = mob.isBoss() ? KOIN_DROP_MAX_BOSS : KOIN_DROP_MAX;

        for (Player p : new CopyOnWriteArrayList<>(map.players)) {
            if (!mob.top_dame.containsKey(p.name)) continue;

            short qty = (short) Util.nextInt(min, max);
            p.item.add_item_bag47(MalamSuroAmbush.JIMAT_ID, qty, MalamSuroAmbush.ITEM_CAT_MATERIAL);

            try {
                // FIX: add_item_bag47() cuma update data di server, tidak otomatis
                // mengirim paket refresh inventory ke client. Tanpa charInventory(7)
                // di sini, item material baru bakal "ada" di data tapi tidak muncul
                // di UI inventory player sampai dia logout/login ulang.
                p.item.charInventory(7);
                Service.Show_open_box_notice_item(
                    p, "Kamu Mendapatkan",
                    new short[]{MalamSuroAmbush.JIMAT_ID},
                    new int[]{qty},
                    new short[]{MalamSuroAmbush.ITEM_CAT_MATERIAL}
                );
            } catch (IOException ignore) {}
        }
    }

    private void cleanup() {
        state = PersimpanganState.CLOSED;
        if (kristal != null) {
            // FIX: kristal sudah tidak pernah didaftarkan ke Boss_entrys (lihat
            // onStart()), jadi tidak perlu di-remove dari sana lagi di sini.
            for (Player p : lilinVisualSent) {
                try { kristal.despawnLilinVisual(p); } catch (IOException ignore) {}
            }
        }
        kristal = null;
        activeMobs.clear();
        lilinVisualSent.clear();
    }

    // ── Getter publik ─────────────────────────────────────────────────────────
    public PersimpanganState getState()       { return state; }
    public KristalArwahEvent      getKristal()     { return kristal; }
    public int               getCurrentWave() { return currentWaveIndex + 1; }
    public int               getTotalWaves()  { return WAVES.size(); }

    /** Dipakai MessageHandler untuk cek apakah efek cuaca map event ini harus nyala. */
    public boolean isRunning() { return state != PersimpanganState.CLOSED; }

    // ── Trigger manual GM ────────────────────────────────────────────────────
    // Selain jadwal otomatis (setiap hari 21:00-22:00 selama 25 Okt-1 Nov),
    // GM bisa mengaktifkan event ini kapan saja lewat menu GM (lihat
    // MenuManager.adminUtilityMenu()), tanpa perlu menunggu jadwal ataupun
    // periode Halloween. Event yang di-trigger manual otomatis berhenti
    // sendiri begitu wave terakhir selesai/kristal hancur (lewat cleanup()),
    // atau bisa dihentikan paksa GM kapan saja lewat adminForceEnd().

    /**
     * Coba mulai event ini secara manual sekarang juga.
     *
     * @return pesan status untuk ditampilkan ke GM.
     */
    public String adminForceStart() {
        if (isRunning() || isActive()) {
            return "Gerbang Arwah sedang berjalan (wave " + getCurrentWave()
                    + "/" + getTotalWaves() + "), tidak bisa dimulai lagi.";
        }
        boolean ok = forceStart();
        return ok
                ? "Gerbang Arwah berhasil diaktifkan manual!"
                : "Gagal mengaktifkan Gerbang Arwah.";
    }

    /**
     * Hentikan paksa event ini kalau sedang berjalan lewat trigger manual GM.
     * Tidak berpengaruh kalau event sedang berjalan lewat jadwal otomatis
     * biasa (gunakan itu untuk membiarkannya selesai secara natural).
     *
     * @return pesan status untuk ditampilkan ke GM.
     */
    public String adminForceEnd() {
        if (!isManualOverride()) {
            return "Gerbang Arwah tidak sedang berjalan secara manual.";
        }
        boolean ok = forceEnd();
        return ok
                ? "Gerbang Arwah telah dihentikan paksa."
                : "Gagal menghentikan Gerbang Arwah.";
    }

    /**
     * Kirim / matikan efek cuaca ambient map event ini (Gerbang Arwah) ke satu player.
     * Format packet sama seperti weather map 48 (lihat MessageHandler case 12).
     * type=4 -> nyala (sama seperti dungeon 48), type=0 -> matikan.
     * TODO: konfirmasi ke tim client apakah id 0 memang berarti "no weather".
     */
    private void sendWeather(client.Player p, byte type) {
        try {
            client.io.Message mw = new client.io.Message(76);
            mw.writer().writeByte(type);
            mw.writer().writeShort(-1);
            mw.writer().writeShort(-1);
            p.conn.addmsg(mw);
            mw.cleanup();
        } catch (IOException ignore) {}
    }
}