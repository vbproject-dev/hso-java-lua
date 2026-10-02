package game.map;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import client.Player;
import client.io.Message;
import lombok.extern.slf4j.Slf4j;
import template.MainObject;
import template.PartData;
import template.PartDataLoader;

/**
 * Nge-spawn objek "item/effect" berdiri sendiri di map pada koordinat x,y
 * tertentu — TIDAK nempel ke mob/player manapun (beda sama Eff_special_skill
 * yang nempel ke player, dan beda sama Service.SendEffMob yang nempel ke mob).
 *
 * Cara munculin (SAMA persis dengan jalur lama Service.sendEffMap yang terbukti
 * jalan): asset part_char type 111 dibaca dari PartDataLoader sesuai zoom level
 * client (x1..x4) lalu DI-EMBED langsung ke paket. Versi sebelumnya kirim
 * payloadLength = 0 dan berharap client punya eff_{id}.png bawaan / minta
 * sendiri lewat part_char (-52) — makanya efeknya nggak muncul.
 *
 * Paket Message(-49), sub-opcode 1 (handler eq.af() di client):
 *   byte   subOpcode   = 1
 *   short  payloadLen  panjang asset (PartData.imageData), dikirim langsung
 *   byte[] payload     isi asset
 *   byte   blockW      lebar area blocking (tile), 0 = tidak nge-block
 *   byte   blockH      tinggi area blocking (tile)
 *   byte   graphicId   id part_char type 111, UNSIGNED BYTE 0-255
 *   short  x, y        posisi (pixel)
 *   byte   mode        0 = normal (list cs.b), 3 = mode khusus (list cn.V)
 *   byte   flag        byte mentah. Dulu dinamai "linkFlag" (1/0), tapi pemanggilan
 *                      yang terbukti jalan memakai 75/95/115, jadi diperlakukan
 *                      sebagai nilai byte biasa yang diatur dari luar
 *   short  key         id/kunci objek (di sendEffMap lama diisi idnpc, mis. -65)
 *   short  extra       cuma dipakai kalau mode == 3, tetap wajib ditulis
 *   byte   dedup       nilai yang terbukti jalan = 2 (client cek dulu apa sudah
 *                      ada objek dengan key yang sama sebelum nambah baru)
 *
 * Kalau asset part_char 111_{id} nggak ada di data/part_char/{x1..x4}, efek
 * di-skip dan dicatat di log (warn), bukan crash.
 */
@Slf4j
public class MapItemEffect {

    /**
     * Parameter tambahan spawn, semuanya opsional. Objek ini immutable —
     * tiap method mengembalikan salinan baru, jadi aman dipakai ulang.
     *
     * Contoh (setara Service.sendEffMap(p, -65, 60, 648, 360, 4, 2, 95)):
     *   MapItemEffect.sendTo(p, 60, 648, 360,
     *           MapItemEffect.Options.create().key(-65).block(4, 2).flag(95));
     */
    public static final class Options {
        private final int blockW, blockH, flag, key, mode, extra, dedup;

        private Options(int blockW, int blockH, int flag, int key, int mode, int extra, int dedup) {
            this.blockW = blockW;
            this.blockH = blockH;
            this.flag = flag;
            this.key = key;
            this.mode = mode;
            this.extra = extra;
            this.dedup = dedup;
        }

        /** Default: nggak nge-block, mode 0, dedup otomatis (2 kalau ada key, 0 kalau tidak). */
        public static Options create() {
            return new Options(0, 0, 0, 0, 0, 0, -1);
        }

        /** Area blocking dalam tile (0,0 = tidak nge-block). */
        public Options block(int w, int h) {
            return new Options(w, h, flag, key, mode, extra, dedup);
        }

        /** Byte "flag" setelah mode (lihat javadoc kelas). */
        public Options flag(int flag) {
            return new Options(blockW, blockH, flag, key, mode, extra, dedup);
        }

        /** Key objek (short). Isi 0 kalau nggak perlu. */
        public Options key(int key) {
            return new Options(blockW, blockH, flag, key, mode, extra, dedup);
        }

        /** mode 0 = normal, 3 = mode khusus (extra = counter "sisa loop" di client). */
        public Options mode(int mode, int extra) {
            return new Options(blockW, blockH, flag, key, mode, extra, dedup);
        }

        /** Paksa nilai dedupFlag. Default otomatis: 2 kalau key != 0, else 0. */
        public Options dedup(int dedup) {
            return new Options(blockW, blockH, flag, key, mode, extra, dedup);
        }

        private int dedupValue() {
            return dedup >= 0 ? dedup : (key != 0 ? 2 : 0);
        }
    }

    /**
     * Broadcast ke semua player yang ada di map SAAT INI. Player yang masuk
     * belakangan tidak ikut dapat — untuk efek permanen kirim per-player lewat
     * sendTo() dari GameMap.sendMapData().
     * Asset dipilih per zoom level tiap player.
     */
    public static void spawn(GameMap gameMap, int graphicId, int x, int y, Options o) throws IOException {
        checkId(graphicId);
        Map<Byte, Message> perZoom = new HashMap<>(); // 1 paket per zoom level
        try {
            for (Player p0 : gameMap.players.toArray(new Player[0])) {
                if (p0 == null || p0.conn == null || !p0.conn.connected) continue;
                byte zoom = p0.conn.zoomlv;
                if (!perZoom.containsKey(zoom)) {
                    perZoom.put(zoom, buildMessage(zoom, graphicId, x, y, o)); // bisa null kalau asset nggak ada
                }
                Message m = perZoom.get(zoom);
                if (m != null) p0.conn.addmsg(m);
            }
        } finally {
            for (Message m : perZoom.values()) {
                if (m != null) m.cleanup();
            }
        }
    }

    public static void spawn(GameMap gameMap, int graphicId, int x, int y, int linkKey) throws IOException {
        spawn(gameMap, graphicId, x, y, Options.create().key(linkKey));
    }

    public static void spawn(GameMap gameMap, int graphicId, int x, int y) throws IOException {
        spawn(gameMap, graphicId, x, y, Options.create());
    }

    /**
     * Kirim ke SATU player saja (bukan broadcast). Dipakai buat efek permanen
     * map yang harus muncul buat player yang baru join, kapan pun mereka masuk.
     */
    public static void sendTo(Player p, int graphicId, int x, int y, Options o) throws IOException {
        if (p == null || p.conn == null || !p.conn.connected) return;
        checkId(graphicId);
        Message m = buildMessage(p.conn.zoomlv, graphicId, x, y, o);
        if (m == null) return;
        p.conn.addmsg(m);
        m.cleanup();
    }

    public static void sendTo(Player p, int graphicId, int x, int y, int linkKey) throws IOException {
        sendTo(p, graphicId, x, y, Options.create().key(linkKey));
    }

    public static void sendTo(Player p, int graphicId, int x, int y) throws IOException {
        sendTo(p, graphicId, x, y, Options.create());
    }

    /**
     * Spawn efek part_char type 111 yang NEMPEL ke satu atau lebih entity
     * (player dan/atau mob) sekaligus — beda dari spawn()/sendTo() di atas
     * yang berdiri sendiri di koordinat x,y tetap. Port dari jalur lama
     * Service.send_eff_auto(conn, objects, id_eff), sub-opcode 4:
     *   byte   subOpcode   = 4
     *   short  payloadLen  + byte[] payload   asset part_char 111_{idEff}, di-embed
     *   short  idEff
     *   byte   objectCount
     *   [short objectId, byte typeObj] x objectCount   (typeObj: 0=player, 1=mob,
     *                                                    lihat MainObject.getTypeObject())
     * Dikirim SEKALI (tidak ada param durasi) — client yang nentuin panjang
     * animasinya sendiri dari asetnya, cocok buat efek "proc" sekali kedip
     * pas kena hit, bukan efek yang nempel lama kayak Eff_hit_wear22.
     *
     * Sama seperti jalur lama: asset diambil pakai zoom level SI PENYERANG
     * (attacker) lalu di-broadcast apa adanya ke SEMUA player di map, bukan
     * per-zoom-level tiap viewer. Ini sengaja disamain biar perilakunya identik
     * dengan versi lama yang terbukti jalan.
     */
    public static void spawnAuto(GameMap gameMap, Player attacker, int idEff, List<MainObject> targets)
            throws IOException {
        if (gameMap == null || attacker == null || attacker.conn == null || !attacker.conn.connected
                || targets == null || targets.isEmpty()) {
            return;
        }
        checkId(idEff);
        PartData part = PartDataLoader.getByZoom(attacker.conn.zoomlv, (byte) 111, (short) idEff);
        if (part == null || part.imageData == null) {
            log.warn("[MapItemEffect] asset part_char 111_{} (auto) nggak ditemukan (zoom x{}), efek di-skip",
                    idEff, attacker.conn.zoomlv);
            return;
        }
        byte[] data = part.imageData;

        Message m = new Message(-49);
        DataOutputStream w = m.writer();
        w.writeByte(4);                    // sub-opcode: efek "auto" nempel ke entity
        w.writeShort(data.length);         // payloadLength
        w.write(data);                     // asset di-embed
        w.writeShort(idEff);
        w.writeByte(targets.size());
        for (MainObject o : targets) {
            w.writeShort(o.objectId);
            w.writeByte(o.getTypeObject());
        }
        try {
            for (Player p0 : gameMap.players.toArray(new Player[0])) {
                if (p0 != null && p0.conn != null && p0.conn.connected) p0.conn.addmsg(m);
            }
        } finally {
            m.cleanup();
        }
    }

    /** Shortcut buat 1 target doang (kasus paling umum: efek nempel ke diri sendiri). */
    public static void spawnAuto(GameMap gameMap, Player attacker, int idEff, MainObject target) throws IOException {
        if (target == null) return;
        List<MainObject> single = new java.util.ArrayList<>();
        single.add(target);
        spawnAuto(gameMap, attacker, idEff, single);
    }

    private static void checkId(int graphicId) {
        if (graphicId < 0 || graphicId > 255) {
            throw new IllegalArgumentException("graphicId part_char 111 harus 0-255, dapat: " + graphicId);
        }
    }

    /** Bangun Message(-49) sub-opcode 1 untuk satu zoom level. Return null kalau asset nggak ada. */
    private static Message buildMessage(int zoomLv, int graphicId, int x, int y, Options o) throws IOException {
        PartData part = PartDataLoader.getByZoom(zoomLv, (byte) 111, (short) graphicId);
        if (part == null || part.imageData == null) {
            log.warn("[MapItemEffect] asset part_char 111_{} nggak ditemukan (zoom x{}), efek di-skip", graphicId, zoomLv);
            return null;
        }
        byte[] data = part.imageData;

        Message m = new Message(-49);
        DataOutputStream w = m.writer();
        w.writeByte(1);                    // sub-opcode: spawn item map
        w.writeShort(data.length);         // payloadLength
        w.write(data);                     // asset di-embed
        w.writeByte(o.blockW);
        w.writeByte(o.blockH);
        w.writeByte(graphicId);
        w.writeShort(x);
        w.writeShort(y);
        w.writeByte(o.mode);
        w.writeByte(o.flag);
        w.writeShort(o.key);
        w.writeShort(o.extra);
        w.writeByte(o.dedupValue());
        return m;
    }
}