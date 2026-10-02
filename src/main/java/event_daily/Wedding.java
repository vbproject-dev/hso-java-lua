package event_daily;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import client.Player;
import client.io.Message;
import core.Manager;
import core.SQL;
import core.Service;
import core.Util;
import game.map.MapService;
import template.Item3;
import template.ItemTemplate3;
import template.Option;

public class Wedding {

    // FIX (root cause of the mass "Lock wait timeout" / HikariCP pool exhaustion
    // incident): saveItem() used to be called synchronously from
    // MainObject.attack() on every combat/session thread every time a
    // wedding ring crossed a 1000-exp threshold. Under normal play load that
    // meant dozens of blocking JDBC UPDATE calls per second issued directly
    // from combat processing; any DB slowness (lock contention, replication
    // lag, etc.) then stalled combat threads and starved the HikariCP pool,
    // which cascaded into failures across unrelated features (selling items,
    // quests, logins). Ring-exp persistence is not latency sensitive, so it
    // is moved onto a small dedicated background pool instead.
    private static final AtomicInteger SAVE_THREAD_COUNT = new AtomicInteger(1);
    private static final ThreadFactory SAVE_THREAD_FACTORY = r -> {
        Thread t = new Thread(r, "wedding-save-" + SAVE_THREAD_COUNT.getAndIncrement());
        t.setDaemon(true);
        return t;
    };
    private static final ExecutorService SAVE_EXECUTOR =
            Executors.newFixedThreadPool(2, SAVE_THREAD_FACTORY);

    public static List<Wedding> list = new ArrayList<>();
    public int id;
    public String name_1;
    public String name_2;
    public Item3 it;
    public long exp;
    public Item3[] weddingChest = new Item3[30]; // 30 slot gudang bersama
    public long activityScore = 0; // skor keaktifan pasangan (runtime, tidak disimpan ke DB)

    // Icon & properti tetap cincin pasangan — tidak boleh diambil dari equipment DB
    private static final short WEDDING_ICON  = 14101;
    private static final byte  WEDDING_TYPE  = 103;
    private static final byte  WEDDING_CLAZZ = 4;
    private static final short WEDDING_LEVEL = 60;

    /**
     * Hitung skor keaktifan pasangan:
     * - exp grinding bersama (dari MainObject saat lawan monster dalam party)
     * - tier cincin * 1.000 (bonus tier yang lebih tinggi)
     * - kedua pasangan online sekarang → bonus 5
     */
    public long getActivityScore() {
        long score = exp;
        score += (long) it.tier * 1_000;
        boolean p1Online = game.map.GameMap.get_player_by_name(name_1) != null;
        boolean p2Online = game.map.GameMap.get_player_by_name(name_2) != null;
        if (p1Online && p2Online) score += 5;
        return score;
    }

    // ===================== BUAT ITEM3 CINCIN =====================

    /**
     * Buat objek Item3 cincin dari field primitif.
     * Dipakai oleh add_new dan loadAllFromDB agar properti selalu konsisten.
     * FIX BUG 1: icon, type, clazz, level selalu dari konstanta, bukan dari DB equipment.
     */
    private static Item3 buildRingItem(byte color, byte tier, long exp,
                                       List<Option> ops, String name1, String name2) {
        Item3 ring    = new Item3();
        ring.id       = 0;
        ring.clazz    = WEDDING_CLAZZ;
        ring.type     = WEDDING_TYPE;
        ring.level    = WEDDING_LEVEL;
        ring.icon     = WEDDING_ICON;   // FIX: selalu 14101, bukan lookup ke equipment
        ring.color    = color;
        ring.tier     = tier;
        ring.part     = 0;
        ring.islock   = true;
        ring.time_use = 0;
        ring.name     = "Cincin Pasangan " + name2 + " dan " + name1;
        ring.op       = ops != null ? ops : new ArrayList<>();
        return ring;
    }

    // ===================== ADD NEW =====================

    @SuppressWarnings("unchecked")
    public synchronized static void add_new(int quant, Player p, Player p0) throws IOException {
        Wedding temp   = new Wedding();
        temp.name_1    = p.name;
        temp.name_2    = p0.name;
        temp.exp       = 1;

        byte color = (byte) (quant - 1);
        byte tier  = 0;

        int[] dame     = {100, 200, 300, 400};
        int[] dame_per = {500, 700, 900, 1200};
        int[] point    = {10,  15,  20,  25};
        int[] resis    = {500, 700, 900, 1200};

        List<Option> ops = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            ops.add(new Option(i, Util.random(100, dame[color])));
        for (int i = 7; i < 12; i++)
            ops.add(new Option(i, Util.random(500, dame_per[color])));
        for (int i = 23; i < 27; i++)
            ops.add(new Option(i, Util.random(10, point[color])));
        for (int i = 16; i < 21; i++)
            ops.add(new Option(i, Util.random(500, resis[color])));

        temp.it = buildRingItem(color, tier, temp.exp, ops, temp.name_1, temp.name_2);

        // Simpan ke DB
        try (Connection connection = SQL.gI().getConnection();
             Statement st = connection.createStatement()) {
            JSONArray jsName = new JSONArray();
            jsName.add(temp.name_1);
            jsName.add(temp.name_2);

            String itemJson = serializeRingItem(temp.exp, temp.it);

            String query = "INSERT INTO `wedding` (`name`, `item`, `item_wedding`) VALUES (%s, %s, '[]')";
            st.execute(String.format(query,
                    "'" + jsName.toJSONString() + "'",
                    "'" + itemJson + "'"));
            connection.commit();
            ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()");
            if (rs.next()) temp.id = rs.getInt(1);
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            p.conn.close();
            return;
        }

        Wedding.list.add(temp);

        p.item.wear[23] = temp.it;
        Service.sendPlayerWear(p);
        Service.sendMainCharInfo(p);
        MapService.broadcastMainCharInfo(p.map, p);
        p.it_wedding = temp;

        p0.item.wear[23] = temp.it;
        Service.sendPlayerWear(p0);
        Service.sendMainCharInfo(p0);
        MapService.broadcastMainCharInfo(p0.map, p0);
        p0.it_wedding = temp;

        String[] ringNames = {
            "Cincin Pernikahan 1gram", "Cincin Pernikahan 2gram",
            "Cincin Pernikahan 3gram", "Cincin Pernikahan 4gram"
        };
        Manager.gI().chatKTGprocess(
            "💍 Selamat! " + p.name + " dan " + p0.name
            + " resmi menikah dengan " + ringNames[color]
            + "! Semoga bahagia selalu 💕, uhuuyyy ciee"
        );
    }

    // ===================== LOAD & SAVE ITEM CINCIN =====================

    /**
     * FIX BUG 1 & BUG 2: Load semua wedding dari DB saat startup server.
     * Reconstruct Item3 cincin dari kolom `item`, sehingga:
     * - icon selalu 14101 (bukan Pedang Batu)
     * - stat dari DB tidak di-overwrite oleh equipment template
     * Panggil method ini di startup server setelah DB siap.
     */
    public synchronized static void loadAllFromDB() {
        list.clear();
        try (Connection connection = SQL.gI().getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `wedding`")) {

            while (rs.next()) {
                Wedding temp = new Wedding();
                temp.id = rs.getInt("id");

                JSONArray names = (JSONArray) JSONValue.parse(rs.getString("name"));
                if (names == null || names.size() < 2) continue;
                temp.name_1 = names.get(0).toString();
                temp.name_2 = names.get(1).toString();

                // Parse kolom item → [exp, color, tier, [[opId, opVal], ...]]
                String itemStr = rs.getString("item");
                if (itemStr == null || itemStr.isEmpty() || itemStr.equals("[]")) continue;
                JSONArray js2 = (JSONArray) JSONValue.parse(itemStr);
                if (js2 == null || js2.size() < 4) continue;

                temp.exp    = Long.parseLong(js2.get(0).toString());
                byte color  = Byte.parseByte(js2.get(1).toString());
                byte tier   = Byte.parseByte(js2.get(2).toString());

                // FIX BUG 2: baca stat dari DB, bukan dari equipment template
                List<Option> ops = new ArrayList<>();
                JSONArray stats  = (JSONArray) JSONValue.parse(js2.get(3).toString());
                if (stats != null) {
                    for (Object o : stats) {
                        JSONArray entry = (JSONArray) JSONValue.parse(o.toString());
                        if (entry == null || entry.size() < 2) continue;
                        ops.add(new Option(
                            Byte.parseByte(entry.get(0).toString()),
                            Integer.parseInt(entry.get(1).toString())));
                    }
                }

                // FIX BUG 1: buildRingItem menjamin icon=14101, bukan dari equipment DB
                temp.it = buildRingItem(color, tier, temp.exp, ops, temp.name_1, temp.name_2);

                // Load gudang bersama
                temp.loadChest(rs.getString("item_wedding"));

                list.add(temp);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * FIX BUG 2: Simpan stat cincin (kolom `item`) ke DB.
     * Panggil ini setiap kali stat, tier, atau exp cincin berubah.
     * Tanpa method ini perubahan stat tidak pernah tersimpan.
     *
     * FIX (perf/incident): dulu method ini langsung melakukan JDBC UPDATE
     * secara sinkron di thread pemanggil. Karena dipanggil dari
     * MainObject.attack() (thread combat/session), setiap kali server DB
     * lambat (lock contention dsb.) thread combat ikut tertahan dan pool
     * koneksi (HikariCP) bisa habis, yang berdampak ke seluruh fitur lain
     * (jual item, quest, dsb). Sekarang saveItem() hanya menjadwalkan
     * pekerjaan simpan ke background executor dan langsung kembali,
     * sehingga thread combat tidak pernah menunggu I/O database.
     */
    public void saveItem() {
        // Snapshot data yang dibutuhkan sebelum lompat ke thread lain,
        // supaya perubahan exp/it berikutnya tidak memengaruhi save ini.
        final long expSnapshot = this.exp;
        final Item3 itSnapshot = this.it;
        final int idSnapshot = this.id;
        SAVE_EXECUTOR.execute(() -> saveItemBlocking(idSnapshot, expSnapshot, itSnapshot));
    }

    @SuppressWarnings("unchecked")
    private synchronized void saveItemBlocking(int id, long exp, Item3 it) {
        String itemJson = serializeRingItem(exp, it);
        boolean wasInterrupted = Thread.interrupted();
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `wedding` SET `item` = ? WHERE `id` = ?")) {
            ps.setString(1, itemJson);
            ps.setInt(2, id);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wasInterrupted) Thread.currentThread().interrupt();
        }
    }

    /**
     * Serialize cincin ke JSON string untuk kolom `item`.
     * Format: [exp, color, tier, [[opId, opVal], ...]]
     */
    @SuppressWarnings("unchecked")
    public static String serializeRingItem(long exp, Item3 ring) {
        JSONArray js = new JSONArray();
        js.add(exp);
        js.add(ring.color);
        js.add(ring.tier);
        JSONArray stats = new JSONArray();
        for (Option op : ring.op) {
            JSONArray entry = new JSONArray();
            entry.add(op.id);
            entry.add(op.getParam(0));
            stats.add(entry);
        }
        js.add(stats);
        return js.toJSONString();
    }

    // ===================== HELPER =====================

    public synchronized static Wedding get_obj(String name) {
        for (Wedding temp : Wedding.list)
            if (temp.name_1.equals(name) || temp.name_2.equals(name)) return temp;
        return null;
    }

    public synchronized static void remove_wed(Wedding temp) {
        Wedding.list.remove(temp);
        // Hapus dari DB agar tidak muncul lagi setelah server restart
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "DELETE FROM `wedding` WHERE `id` = ?")) {
            ps.setInt(1, temp.id);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== WEDDING CHEST =====================

    /**
     * Kirim paket pembuka UI chest (Message 23) lalu isi gudang (Message 65).
     * Dipanggil dari Menu_Wedding case 4.
     */
    public synchronized void openChestFor(Player p) throws IOException {
        p.wedding_chest_open = true;

        Message open = new Message(23);
        open.writer().writeUTF("Gudang Pasangan");
        open.writer().writeByte(3);
        open.writer().writeShort(0);
        p.conn.addmsg(open);
        open.cleanup();

        sendChestTo(p);
    }

    /** Kirim isi gudang bersama ke player (Message 65, format char_chest type 3) */
    public synchronized void sendChestTo(Player p) throws IOException {
        Message m = new Message(65);
        m.writer().writeByte(30);
        m.writer().writeByte(0);
        m.writer().writeByte(3);
        m.writer().writeByte(3);

        int count = 0;
        for (Item3 it3 : weddingChest) if (it3 != null) count++;
        m.writer().writeByte(count);

        for (int i = 0; i < weddingChest.length; i++) {
            Item3 it3 = weddingChest[i];
            if (it3 == null) continue;
            m.writer().writeUTF(it3.name);
            m.writer().writeByte(it3.clazz);
            m.writer().writeShort(i);
            m.writer().writeByte(it3.type);
            m.writer().writeShort(it3.icon);
            m.writer().writeByte(it3.tier);
            m.writer().writeShort(it3.level);
            m.writer().writeByte(it3.color);
            m.writer().writeByte(1);
            m.writer().writeByte(it3.islock ? 0 : 1);
            m.writer().writeByte(it3.op.size());
            for (int j = 0; j < it3.op.size(); j++) {
                m.writer().writeByte(it3.op.get(j).id);
                m.writer().writeInt(it3.op.get(j).getParam(it3.tier));
            }
            m.writer().writeInt(0);
            m.writer().writeByte(it3.islock ? (byte) 1 : (byte) 0);
            m.writer().writeByte(0);
            m.writer().writeByte(0);
        }
        p.conn.addmsg(m);
        m.cleanup();
    }

    /** Deposit: ambil item dari bag[bagIndex] player → simpan ke weddingChest */
    public synchronized void depositItem(Player p, short bagIndex) throws IOException {
        if (bagIndex < 0 || bagIndex >= p.item.bag3.length) {
            Service.send_notice_box(p.conn, "Index item tidak valid!");
            return;
        }
        Item3 it3 = p.item.bag3[bagIndex];
        if (it3 == null) {
            Service.send_notice_box(p.conn, "Item tidak ditemukan di tas!");
            return;
        }
        int emptySlot = -1;
        for (int i = 0; i < weddingChest.length; i++) {
            if (weddingChest[i] == null) { emptySlot = i; break; }
        }
        if (emptySlot == -1) {
            Service.send_notice_box(p.conn, "Gudang pasangan penuh! (maks 30 slot)");
            return;
        }
        weddingChest[emptySlot] = it3;
        p.item.bag3[bagIndex] = null;
        p.item.charInventory(3);
        sendChestTo(p);
        saveChest();
    }

    /** Withdraw: ambil item dari weddingChest[chestIndex] → taruh di bag player */
    public synchronized void withdrawItem(Player p, short chestIndex) throws IOException {
        if (chestIndex < 0 || chestIndex >= weddingChest.length) {
            Service.send_notice_box(p.conn, "Index gudang tidak valid!");
            return;
        }
        Item3 it3 = weddingChest[chestIndex];
        if (it3 == null) {
            Service.send_notice_box(p.conn, "Slot gudang kosong!");
            return;
        }
        int emptyBag = -1;
        for (int i = 0; i < p.item.bag3.length; i++) {
            if (p.item.bag3[i] == null) { emptyBag = i; break; }
        }
        if (emptyBag == -1) {
            Service.send_notice_box(p.conn, "Tas kamu penuh!");
            return;
        }
        p.item.bag3[emptyBag] = it3;
        weddingChest[chestIndex] = null;
        p.item.charInventory(3);
        sendChestTo(p);
        saveChest();
    }

    /** Simpan weddingChest ke DB kolom item_wedding */
    @SuppressWarnings("unchecked")
    public synchronized void saveChest() {
        JSONArray jsar = new JSONArray();
        for (int i = 0; i < weddingChest.length; i++) {
            Item3 temp = weddingChest[i];
            if (temp == null) continue;
            JSONArray js2 = new JSONArray();
            js2.add(i);
            js2.add(temp.id);
            js2.add(temp.clazz);
            js2.add(temp.type);
            js2.add(temp.level);
            js2.add(temp.icon);
            js2.add(temp.color);
            js2.add(temp.part);
            js2.add(temp.islock ? 1 : 0);
            js2.add(temp.tier);
            JSONArray js3 = new JSONArray();
            for (int j = 0; j < temp.op.size(); j++) {
                JSONArray js4 = new JSONArray();
                js4.add(temp.op.get(j).id);
                js4.add(temp.op.get(j).getParam(0));
                js3.add(js4);
            }
            js2.add(js3);
            jsar.add(js2);
        }
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `wedding` SET `item_wedding` = ? WHERE `id` = ?")) {
            ps.setString(1, jsar.toJSONString());
            ps.setInt(2, this.id);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Load weddingChest dari JSON string saat startup server */
    public synchronized void loadChest(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) return;
        try {
            JSONArray jsar = (JSONArray) JSONValue.parse(json);
            if (jsar == null) return;
            for (Object obj : jsar) {
                JSONArray js2 = (JSONArray) JSONValue.parse(obj.toString());
                if (js2 == null) continue;
                int slot = Integer.parseInt(js2.get(0).toString());
                if (slot < 0 || slot >= weddingChest.length) continue;
                Item3 temp  = new Item3();
                temp.id     = Short.parseShort(js2.get(1).toString());
                temp.clazz  = Byte.parseByte(js2.get(2).toString());
                temp.type   = Byte.parseByte(js2.get(3).toString());
                temp.level  = Short.parseShort(js2.get(4).toString());
                temp.icon   = Short.parseShort(js2.get(5).toString());
                temp.color  = Byte.parseByte(js2.get(6).toString());
                temp.part   = Byte.parseByte(js2.get(7).toString());
                temp.islock = Byte.parseByte(js2.get(8).toString()) == 1;
                temp.tier   = Byte.parseByte(js2.get(9).toString());
                ItemTemplate3 template = ItemTemplate3.item.stream()
                    .filter(t -> t.getId() == temp.id)
                    .findFirst()
                    .orElse(null);
                temp.name = template != null ? template.getName() : "Item #" + temp.id;
                if (temp.islock) temp.name += " [Terkunci]";
                JSONArray js3 = (JSONArray) JSONValue.parse(js2.get(10).toString());
                temp.op = new ArrayList<>();
                for (Object o3 : js3) {
                    JSONArray js4 = (JSONArray) JSONValue.parse(o3.toString());
                    temp.op.add(new Option(
                        Byte.parseByte(js4.get(0).toString()),
                        Integer.parseInt(js4.get(1).toString()),
                        temp.id));
                }
                temp.time_use = 0;
                temp.updateName();
                weddingChest[slot] = temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}