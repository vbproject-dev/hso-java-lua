package game.event;

import game.event.btf.BTF;
import game.event.kemerdekaan.KemerdekaanEvent;
import game.event.kemerdekaan.KemerdekaanGemsPagi;
import game.event.kemerdekaan.PersimpanganKematian;
import game.event.halloween.HalloweenEvent;
import game.event.halloween.MalamSuroAmbush;
import game.event.halloween.GerbangArwah;
import game.event.natal.ChristmasEvent;
import game.event.spin.GemSpin;
import game.event.spin.GoldSpin;
import core.Manager;
import lombok.extern.slf4j.Slf4j;
import model.event.GlobalEvent;
import utils.SQLHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GameEventManager {
    private static GameEventManager instance;
    private final List<GameEvent> events = new ArrayList<>();
    private final Map<Integer, GlobalEvent> globalEventMap = new HashMap<>();

    public static GameEventManager gI() {
        if (instance == null) instance = new GameEventManager();
        return instance;
    }

    public void createEvent() {
        Manager m = Manager.gI();

        // Setiap event di bawah ini bisa di-on/off lewat hso.conf (bagian [Event]).
        // Event yang ada di package event_daily (Battlefield, CastleSiegeManager,
        // KingCup, DailyQuest, Wedding, dll) TIDAK diatur di sini.
        if (m.event_tambang) addEvent(new Tambang());
        if (m.event_world_boss) addEvent(new WorldBoss());
        if (m.event_premium_boss) addEvent(new BossHDL.PremiumBossManager());
        if (m.event_btf) addEvent(new BTF());
        if (m.event_gold_spin) addEvent(new GoldSpin());
        if (m.event_gem_spin) addEvent(new GemSpin());
        if (m.event_christmas) addEvent(new ChristmasEvent());
        if (m.event_halloween) addEvent(new HalloweenEvent());
        // FIX: dua sub-event Halloween ini sudah ditulis lengkap (extends
        // GameEvent) dan sudah punya flag sendiri di hso.conf, tapi tidak
        // pernah didaftarkan di sini sebelumnya -> tidak akan pernah jalan
        // sama sekali walau flag-nya true. Lihat javadoc di masing-masing
        // class (bagian "Daftarkan di GameEventManager").
        if (m.event_malam_suro_ambush) addEvent(new MalamSuroAmbush());
        if (m.event_gerbang_arwah) addEvent(new GerbangArwah());
        if (m.event_kemerdekaan) addEvent(new KemerdekaanEvent());
        if (m.event_persimpangan_kematian) addEvent(new PersimpanganKematian());
        if (m.event_kemerdekaan_gems_pagi) addEvent(new KemerdekaanGemsPagi());
    }

    public void addEvent(GameEvent event) {
        events.add(event);
    }

    public void update() {
        for (GameEvent event : events) {
            // Isolasi per event: satu event yang error tidak boleh membuat event lain
            // di daftar ini ikut dilewati pada tick yang sama.
            try {
                event.update();
            } catch (Exception e) {
                System.out.println("[GameEvent] update gagal untuk event " + event.getClass().getSimpleName() + ": " + e);
                e.printStackTrace();
            }
        }
    }

    public <T extends GameEvent> T getEvent(Class<T> clazz) {
        for (GameEvent e : events) {
            if (clazz.isInstance(e)) {
                return (T) e;
            }
        }
        return null;
    }

    public GlobalEvent getGlobalEvent(int npcId) {
        return globalEventMap.get(npcId);
    }

    /**
     * Dipakai GameMap.sendNpcData() untuk menentukan apakah sebuah NPC statis
     * (dari tabel `npc` di map) boleh ditampilkan ke player. NPC yang "milik"
     * sebuah event (Santa, Paskibraka, Pak Lurah, Nenek Merdeka, Persimpangan,
     * dst) hanya ditampilkan selama event tersebut aktif — artinya event-nya
     * enabled di hso.conf DAN sedang dalam jendela tanggal event
     * (shouldBeRemoved() == false). NPC yang bukan milik event apa pun
     * (default) selalu tampil seperti biasa.
     */
    public boolean isEventNpcVisible(int npcId) {
        return switch (npcId) {
            case -120 -> isSeasonActive(ChristmasEvent.class); // Santa
            case -121, -122, -123, -126 -> isSeasonActive(KemerdekaanEvent.class);
            case -131, -132, -133, -134 -> isSeasonActive(HalloweenEvent.class); // Dukun, Banaspati, Pocong, Gerbang Arwah
            default -> true;
        };
    }

    private boolean isSeasonActive(Class<? extends GameEvent> clazz) {
        GameEvent event = getEvent(clazz);
        return event != null && !event.shouldBeRemoved();
    }

    public List<GlobalEvent> getAllEvents() {
      return globalEventMap.values().stream().filter(this::isActive).toList();
    }

    public boolean isActive(GlobalEvent event) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(event.getStartTime()) && now.isBefore(event.getEndTime());
    }

    public void loadGlobalEvent() {
        globalEventMap.clear();

        SQLHelper.selectFrom("global_event")
                .getAsModel(GlobalEvent.class)
                .stream()
                .filter(this::isActive)
                .forEach(event -> globalEventMap.put(event.getNpcId(), event));
    }

}