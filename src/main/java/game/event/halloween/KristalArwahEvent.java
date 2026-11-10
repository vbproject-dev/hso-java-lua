package game.event.halloween;

import client.Player;
import client.io.Message;
import client.io.Session;
import game.map.GameMap;
import game.map.MapItemEffect;
import game.map.MobInMap;
import template.MainObject;
import template.MobTemplate;

import java.io.IOException;

public class KristalArwahEvent extends MobInMap {

    public static final int OBJECT_ID = 0; // 33_000 ga ngaruh

    public static final int TEMPLATE_ID = 64; // ga ngaruh

    public static final short KRISTAL_X = (short) (20 * 24); // lokasi kristal
    public static final short KRISTAL_Y = (short) (20 * 24); // lokasi kristal

    public static final int LILIN_VISUAL_ID = 0;
    public static final short LILIN_GRAPHIC_ID = 69;

    public static int LILIN_EFF_111_ID = 55;
    public static final int LILIN_EFF_KEY  = -75;    // key objek efek (short), harus unik; beda dari lilin Halloween (-70) & jalur lama (-65)
    public static final int LILIN_EFF_FLAG = 105;     // send mapffe

    public static int LILIN_BLANK_GRAPHIC_ID = -1;   // TODO isi id sprite kosong


    public static final int FALL_HEIGHT_PX     = 1000; // ga ngaruh
    public static final int FALL_STEPS         = 16;   // jumlah paket posisi selama jatuh
    public static final long FALL_STEP_DELAY_MS = 10; // jeda antar paket posisi (ms)

    private short infoGraphicId() {
        return (LILIN_EFF_111_ID >= 0 && LILIN_BLANK_GRAPHIC_ID >= 0)
                ? (short) LILIN_BLANK_GRAPHIC_ID : LILIN_GRAPHIC_ID;
    }

    private void sendObjectPosition(Player p, short x, short y) throws IOException {
        Message m = new Message(4);
        m.writer().writeByte(2);
        m.writer().writeShort(0);
        m.writer().writeShort(LILIN_VISUAL_ID);
        m.writer().writeShort(x);
        m.writer().writeShort(y);
        m.writer().writeByte(-1);
        p.conn.addmsg(m);
        m.cleanup();
    }

    public void sendLilinVisual(Player p) throws IOException {
        sendObjectPosition(p, this.x, this.y);

        if (LILIN_EFF_111_ID >= 0) {
            MapItemEffect.sendTo(p, LILIN_EFF_111_ID, this.x, this.y,
                    MapItemEffect.Options.create().key(LILIN_EFF_KEY).flag(LILIN_EFF_FLAG).dedup(2));
        }
    }

    public void sendLilinFall(Player p) throws IOException {
        short targetY = this.y;
        short startY  = (short) Math.max(0, targetY - FALL_HEIGHT_PX);

        sendObjectPosition(p, this.x, startY);
        sendLilinInfoAt(p.conn, this.x, startY);

        for (int i = 1; i <= FALL_STEPS; i++) {
            short stepY = (short) (startY + (targetY - startY) * i / FALL_STEPS);
            boolean isLast = (i == FALL_STEPS);
            utils.Timer.schedule(() -> {
                if (p.conn == null || !p.conn.connected) return;
                try {
                    sendObjectPosition(p, this.x, stepY);
                    if (isLast) {
                        sendLilinInfoAt(p.conn, this.x, targetY); // sinkronkan ulang HP/nama di posisi final
                        if (LILIN_EFF_111_ID >= 0) {
                            MapItemEffect.sendTo(p, LILIN_EFF_111_ID, this.x, targetY,
                                    MapItemEffect.Options.create().key(LILIN_EFF_KEY).flag(LILIN_EFF_FLAG).dedup(2));
                        }
                    }
                } catch (IOException ignore) {}
            }, FALL_STEP_DELAY_MS * i);
        }
    }

    public void sendLilinInfo(Session conn) throws IOException {
        sendLilinInfoAt(conn, this.x, this.y);
    }

    private void sendLilinInfoAt(Session conn, short x, short y) throws IOException {
        Message m = new Message(-44);
        m.writer().writeShort(LILIN_VISUAL_ID);
        m.writer().writeUTF("Api Ritual Kejawen");
        m.writer().writeInt(this.hp);
        m.writer().writeInt(this.maxHp);
        m.writer().writeShort(0);
        m.writer().writeShort(x);
        m.writer().writeShort(y);
        m.writer().writeByte(2);
        m.writer().writeByte(2);
        m.writer().writeUTF("");
        m.writer().writeShort(infoGraphicId());
        m.writer().writeByte(1);
        m.writer().writeShort(17);
        conn.addmsg(m);
        m.cleanup();
    }

    public void despawnLilinVisual(Player p) throws IOException {
        Message m = new Message(8);
        m.writer().writeShort(LILIN_VISUAL_ID);
        p.conn.addmsg(m);
        m.cleanup();
    }

    private final GerbangArwah event;

    public GameMap gameMap;

    public KristalArwahEvent(GerbangArwah event, GameMap gameMap, int maxHp) {
        this.event     = event;
        this.gameMap   = gameMap;
        this.objectId  = OBJECT_ID;
        this.template  = MobTemplate.getMob(TEMPLATE_ID); // load dari monster_template id 64
        this.name      = (this.template != null) ? this.template.name : "Api Ritual"; // fallback kalau template belum ada di DB
        this.x         = KRISTAL_X;
        this.y         = KRISTAL_Y;
        this.maxHp     = maxHp;
        this.hp        = maxHp;
        this.level     = (this.template != null) ? this.template.level : 1;
        this.map_id    = (byte) gameMap.mapId;
        this.zone_id   = (byte) gameMap.zoneId;
        this.setBoss(false);
        this.is_boss_active = false;

        if (this.template == null) {
            System.out.println("[KristalArwahEvent] WARNING: monster_template id " + TEMPLATE_ID + " (Mining Crystal) tidak ditemukan, pakai fallback.");
        }
    }

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
                sendLilinInfoAt(p.conn, this.x, this.y); // sinkronkan HP bar visual lilin juga
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