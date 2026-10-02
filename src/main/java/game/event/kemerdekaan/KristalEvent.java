package game.event.kemerdekaan;

import client.Player;
import client.io.Message;
import client.io.Session;
import game.map.GameMap;
import game.map.MapItemEffect;
import game.map.MobInMap;
import template.MainObject;
import template.MobTemplate;

import java.io.IOException;

/**
 * KristalEvent — Kristal yang harus dilindungi player di Persimpangan Kematian.
 *
 * Berbeda dari Crystal.java (Guild War), kristal ini:
 *  - tidak bisa direbut guild
 *  - hanya bisa diserang oleh monster event (bukan player)
 *  - hancur = DEFEAT, selamat sampai wave terakhir = VICTORY
 *
 * Extends MobInMap (bukan MainObject langsung) supaya bisa didaftarkan ke
 * gameMap.Boss_entrys dan otomatis ikut pipeline spawn/visibility yang sama
 * dipakai mob & kristal tambang (lihat MapService.update_inside_player()):
 *  - Client tahu keberadaan objek ini via packet "spawn" standar (byte 1,
 *    template.mob_id, objectId, x, y) — dirender pakai sprite monster_template
 *    id 64 ("Mining Crystal"), persis seperti kristal tambang.
 *  - Damage dari mob wave ditangani OTOMATIS oleh sistem combat lewat
 *    MainObject.attack(gameMap, mob, kristal, ...):
 *      * HP berkurang otomatis (focus.hp -= dame di dalam attack())
 *      * Saat HP <= 0, setDie() dipanggil otomatis — di sinilah logika
 *        "kristal hancur" (trigger DEFEAT) dijalankan.
 *    Tidak ada lagi takeDamage() manual.
 *  - update() di-override kosong supaya kristal TIDAK ikut AI MobInMap
 *    (tidak jalan, tidak menyerang player, tidak auto-respawn).
 *
 * PENTING: agar objek ini benar-benar terlihat client, map tempat event ini
 * berjalan (PersimpanganKematian.MAP_ID) sudah didaftarkan di
 * GameMap.is_map__load_board_player() supaya tidak dibatasi radius 200 seperti
 * map biasa.
 *
 * FIX: instance ini SENGAJA TIDAK di-add ke gameMap.Boss_entrys. Kalau
 * di-add, MapService.update_inside_player() otomatis mengirim spawn +
 * nameplate/HP bar bawaan objek ini ("Mining Crystal Lv80") ke semua
 * player — NUMPUK dengan visual meteor manual (sendMeteorVisual/
 * sendMeteorInfo) yang dikirim PersimpanganKematian di koordinat yang
 * sama, sehingga muncul dobel di client. Serangan mob ke kristal
 * (MainObject.attack()) tidak butuh keanggotaan Boss_entrys sama sekali
 * karena dipanggil lewat referensi objek langsung.
 */
public class KristalEvent extends MobInMap {

    /** ID object, harus unik dan tidak bentrok dengan MainObject/MobInMap lain */
    public static final int OBJECT_ID = 30_000;

    /** ID di tabel monster_template yang dipakai sebagai model/sprite kristal
     *  (id 64 = "Mining Crystal", typeMove 7 = statis/diam, cocok untuk objek kristal) */
    public static final int TEMPLATE_ID = 64;

    /** Koordinat kristal di map (tile * 24) — tengah-tengah map 40x40 (tile 20,20) */
    public static final short KRISTAL_X = (short) (20 * 24);
    public static final short KRISTAL_Y = (short) (20 * 24);

    /**
     * Visual "meteor jatuh" — objek dekoratif terpisah dari kristal aslinya,
     * dirender pakai graphic id 25 (persis sama seperti objek "Thiên thạch"
     * di Dungeon 48). Murni tampilan; combat/damage tetap lewat objek
     * KristalEvent (this) seperti biasa via MainObject.attack(), tidak
     * disentuh sama sekali di sini.
     */
    public static final int METEOR_VISUAL_ID = 31_000; // beda dari OBJECT_ID (30000), jangan sampai bentrok
    public static final short METEOR_GRAPHIC_ID = 26;  // cS -> sprite meteor batu

    /**
     * ID asset part_char type 111 untuk visual tambahan (0-255), dikirim lewat
     * MapItemEffect (Message -49 sub 1) — asset di-embed dari
     * data/part_char/x1..x4/111_{id}. Ini BEDA dari METEOR_GRAPHIC_ID di atas.
     *
     * Nilai -1 = fitur dimatikan (tidak ada efek 111 yang dikirim, perilaku
     * sama seperti sebelumnya). Isi dengan id asset yang benar untuk mengaktifkan.
     */
    public static int METEOR_EFF_111_ID = -1;         // TODO isi id part_char 111
    public static final int METEOR_EFF_KEY  = -71;    // key objek efek (short), harus unik; beda dari lilin Halloween (-70) & jalur lama (-65)
    public static final int METEOR_EFF_FLAG = 95;     // nilai flag yang terbukti jalan di sendEffMap lama

    /**
     * Sprite pengganti untuk objek lama (id 31000) saat efek 111 aktif. Objek lama
     * TETAP di-spawn (Message 4 + -44) karena dia yang membawa nama "Meteor
     * Kemerdekaan", HP bar, dan jadi target animasi serangan mob. Yang diganti
     * cuma sprite-nya, supaya tidak numpuk dengan efek 111.
     *
     * Isi dengan id sprite yang KOSONG/transparan di client (0-32767). Nilai -1 =
     * tidak diganti (sprite METEOR_GRAPHIC_ID tetap tampil bareng efek 111).
     * Cuma berlaku kalau METEOR_EFF_111_ID >= 0.
     */
    public static int METEOR_BLANK_GRAPHIC_ID = -1;   // TODO isi id sprite kosong

    private short infoGraphicId() {
        return (METEOR_EFF_111_ID >= 0 && METEOR_BLANK_GRAPHIC_ID >= 0)
                ? (short) METEOR_BLANK_GRAPHIC_ID : METEOR_GRAPHIC_ID;
    }

    /** Kirim paket spawn visual meteor (type=2, sama seperti Dungeon.send_map_data) */
    public void sendMeteorVisual(Player p) throws IOException {
        Message m = new Message(4);
        m.writer().writeByte(2);
        m.writer().writeShort(0);
        m.writer().writeShort(METEOR_VISUAL_ID);
        m.writer().writeShort(this.x);
        m.writer().writeShort(this.y);
        m.writer().writeByte(-1);
        p.conn.addmsg(m);
        m.cleanup();

        // Visual dari asset part_char 111 (dekoratif, berdiri sendiri di x,y kristal).
        // Kalau asset tidak ada di data/part_char, MapItemEffect hanya log warn (tidak crash).
        if (METEOR_EFF_111_ID >= 0) {
            MapItemEffect.sendTo(p, METEOR_EFF_111_ID, this.x, this.y,
                    MapItemEffect.Options.create().key(METEOR_EFF_KEY).flag(METEOR_EFF_FLAG).dedup(2));
        }
    }

    /** Kirim info visual meteor (nama + HP bar, sinkron dengan HP kristal asli) */
    public void sendMeteorInfo(Session conn) throws IOException {
        Message m = new Message(-44);
        m.writer().writeShort(METEOR_VISUAL_ID);
        m.writer().writeUTF("Meteor Kemerdekaan");
        m.writer().writeInt(this.hp);
        m.writer().writeInt(this.maxHp);
        m.writer().writeShort(0);
        m.writer().writeShort(this.x);
        m.writer().writeShort(this.y);
        m.writer().writeByte(2);
        m.writer().writeByte(2);
        m.writer().writeUTF("");
        m.writer().writeShort(infoGraphicId());
        m.writer().writeByte(1);
        m.writer().writeShort(17);
        conn.addmsg(m);
        m.cleanup();
    }

    /** Hapus visual meteor dari client (dipanggil saat event selesai/kristal hancur) */
    public void despawnMeteorVisual(Player p) throws IOException {
        Message m = new Message(8);
        m.writer().writeShort(METEOR_VISUAL_ID);
        p.conn.addmsg(m);
        m.cleanup();
    }

    /** Referensi ke event induk untuk trigger DEFEAT saat HP habis */
    private final PersimpanganKematian event;

    public GameMap gameMap;

    public KristalEvent(PersimpanganKematian event, GameMap gameMap, int maxHp) {
        this.event     = event;
        this.gameMap   = gameMap;
        this.objectId  = OBJECT_ID;
        this.template  = MobTemplate.getMob(TEMPLATE_ID); // load dari monster_template id 64
        this.name      = (this.template != null) ? this.template.name : "Kristal Kehidupan";
        this.x         = KRISTAL_X;
        this.y         = KRISTAL_Y;
        this.maxHp     = maxHp;
        this.hp        = maxHp;
        this.level     = (this.template != null) ? this.template.level : 1;
        this.map_id    = (byte) gameMap.mapId;
        this.zone_id   = (byte) gameMap.zoneId;
        // Pastikan tidak ikut AI bawaan MobInMap (tidak menyerang, tidak respawn sendiri)
        this.setBoss(false);
        this.is_boss_active = false;

        if (this.template == null) {
            // Template belum ada di DB — kristal tetap jalan pakai fallback di atas,
            // tapi cek lagi id 64 di tabel monster_template.
            System.out.println("[KristalEvent] WARNING: monster_template id " + TEMPLATE_ID + " (Mining Crystal) tidak ditemukan, pakai fallback.");
        }
    }

    /**
     * Kristal HANYA boleh diserang monster event, tidak oleh player sama sekali —
     * MainObject.attack() akan skip total (dame=0, tanpa efek) kalau ObjAtk player.
     */
    @Override
    public boolean canBeAttackedByPlayer() {
        return false;
    }

    @Override
    public int getDefBase() {
        return 0; // Kristal tidak punya defense — semua damage langsung masuk
    }

    @Override
    public int getMiss() {
        return 0; // Kristal tidak bisa dodge (MobInMap default 500 / 5%)
    }

    /**
     * Override kosong — kristal TIDAK boleh ikut AI MobInMap.update() (jalan,
     * menyerang player, auto-respawn). Kristal sepenuhnya pasif; damage yang
     * masuk selalu berasal dari MainObject.attack() yang dipanggil manual oleh
     * PersimpanganKematian di wave loop.
     */
    @Override
    public void update(GameMap gameMap) {
        // no-op
    }

    /** Kirim paket update HP kristal ke semua player di map (format sama seperti
     *  sync HP Crystal.java di guild war — lihat DailyMine.closeAttack()) */
    public void broadcastHp() {
        try {
            Message m = new Message(32);
            m.writer().writeByte(1);
            m.writer().writeShort(this.objectId);
            m.writer().writeShort(-1); // id item bag — tidak dipakai untuk kristal
            m.writer().writeByte(0);
            m.writer().writeInt(this.maxHp);
            m.writer().writeInt(this.hp);
            m.writer().writeInt(0); // param use
            for (Player p : gameMap.players) {
                p.conn.addmsg(m);
                sendMeteorInfo(p.conn); // sinkronkan HP bar visual meteor juga
            }
            m.cleanup();
        } catch (IOException ignore) {}
    }

    /** Reset HP kristal untuk ronde baru */
    public void reset(int newMaxHp) {
        this.maxHp = newMaxHp;
        this.hp    = newMaxHp;
        this.isdie = false;
        broadcastHp();
    }

    /**
     * Dipanggil OTOMATIS oleh MainObject.attack() saat hp kristal mencapai 0
     * akibat serangan monster event — tidak perlu dipanggil manual.
     */
    @Override
    public void setDie(GameMap map, MainObject attacker) {
        if (this.isdie) return; // sudah hancur sebelumnya, cegah trigger dobel

        this.hp    = 0;
        this.isdie = true;

        // Broadcast HP final (0) ke semua player di map
        broadcastHp();

        // Kristal hancur → trigger DEFEAT di event induk
        try {
            event.onKristalHancur();
        } catch (IOException ignore) {}
    }
}