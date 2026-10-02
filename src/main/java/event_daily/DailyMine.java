package event_daily;

import ai.Clone;
import game.ai.Location;
import game.ai.Position;
import game.guild.Guild;
import core.Manager;
import core.SQL;
import client.io.Message;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import game.map.GameMap;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.Crystal;

public class DailyMine {
    private static final String GOLD = "Tambang Gold";
    private static final String GEMS = "Tambang Permata";
    private static final String EXP = "Tambang Exp";

    @Setter
    @Getter
    private boolean running;
    private CopyOnWriteArrayList<Crystal> listCrystal;
    private long timeSave;


    public void init() {
        this.running = false;
        this.listCrystal = new CopyOnWriteArrayList<>();
      
        int[] map_ = new int[]{3, 5, 8, 9, 11, 12, 15, 16, 19, 21, 22, 24, 26, 27, 37, 42};
        int[] x_ = new int[]{444, 1068, 228, 804, 516, 684, 540, 612, 1020, 444, 228, 612, 540, 492, 492, 756};
        int[] y_ = new int[]{156, 348, 516, 972, 372, 588, 588, 204, 204, 108, 372, 708, 396, 612, 420, 300};

        String[] name_ = new String[]{
                GOLD, EXP, GEMS, EXP,
                GOLD, EXP, GEMS, EXP,
                GOLD, EXP, GEMS, EXP,
                GOLD, EXP, GEMS, EXP,
        };
        for (int i = 0; i < map_.length; i++) {

            Location location = new Location(
                    (byte) map_[i],
                    new Position((short) x_[i], (short) y_[i])
            );

            this.listCrystal.add(
                    new Crystal(
                            i + 65000,
                            location,
                            50_000_000,
                            50_000_000,
                            120,
                            GameMap.getMapById(map_[i])[4],
                            name_[i]

                    )
            );
        }


        loadData();
        Manager.gI().cloneList.clear();

        timeSave = System.currentTimeMillis();
    }

    public Guild getTopGuildByMineCount() {
        java.util.Map<Guild, Integer> count = new java.util.HashMap<>();
        for (Crystal c : listCrystal) {
            if (c.guild != null) {
                count.merge(c.guild, 1, Integer::sum);
            }
        }
        Guild top = null;
        int max = 0;
        for (var entry : count.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
    }

    public int getMineCount(Guild guild) {
        if (guild == null) return 0;
        int total = 0;
        for (Crystal c : listCrystal) {
            if (c.guild == guild) total++;
        }
        return total;
    }

    public Crystal get_mob_in_map(GameMap gameMap) {
        for (Crystal crystal : listCrystal) {
            if (crystal.gameMap.equals(gameMap)) {
                return crystal;
            }
        }
        return null;
    }

    public void openAttack() {
        //resetCrystal();
        setRunning(true);
        for (Crystal crystal : listCrystal) {
            crystal.canAttack = true;
        }
    }

    public void closeAttack() {
        setRunning(false);
        for (Crystal crystal : listCrystal) {
            // try/catch PER TAMBANG: error di satu tambang (mis. player keluar map saat broadcast)
            // tidak boleh menghentikan tambang lain, apalagi membatalkan harvest() & reward di Tambang.onEnd().
            try {
                crystal.canAttack = false;
                crystal.setMaxHP(50_000_000);
                crystal.hp = crystal.getMaxHP();
                //
                Message m_hp = new Message(32);
                m_hp.writer().writeByte(1);
                m_hp.writer().writeShort(crystal.objectId);
                m_hp.writer().writeShort(-1); // id potion in bag
                m_hp.writer().writeByte(0);
                m_hp.writer().writeInt(crystal.getMaxHP()); // max hp
                m_hp.writer().writeInt(crystal.hp); // hp
                m_hp.writer().writeInt(0); // param use
                // Salinan list: gameMap.players adalah ArrayList biasa yang diubah thread lain
                // (player masuk/keluar map), iterasi size()+get(i) bisa IndexOutOfBounds.
                for (client.Player pl : new ArrayList<>(crystal.gameMap.players)) {
                    if (pl != null && pl.conn != null) {
                        pl.conn.addmsg(m_hp);
                    }
                }
                m_hp.cleanup();
            } catch (Exception e) {
                System.out.println("[DailyMine] closeAttack gagal untuk tambang id=" + crystal.objectId + ": " + e);
                e.printStackTrace();
            }
        }
    }

    public void update() {
        long now = System.currentTimeMillis();

        if (now - timeSave >= 30 * 1000) {
            saveData();
            timeSave = now;
        }
    }

    public void harvest() {

        for (Crystal crystal : listCrystal) {
            if (crystal.guild != null) {
                switch (crystal.name) {
                    case GOLD: {
                        crystal.guild.gold += 10_000_000;
                        break;
                    }
                    case GEMS: {
                        crystal.guild.gems += 1000;
                        break;
                    }
                    case EXP: {
                        crystal.guild.exp += 100_000;
                        break;
                    }
                }
            }
        }
    }

    public void resetCrystal() {
        try {
            synchronized (Collections.unmodifiableList(listCrystal)) {
                if (listCrystal != null)
                    listCrystal.clear();

                init();
            }
            // Clan.resetCrystal();
        } catch (Exception e) {
            core.Log.gI().addLogServer("ChiemMo", "Reset: " + e.getMessage());
        }
    }

    public boolean loadData() {
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM `mine` WHERE `id` = ?")) {

            for (Crystal m : listCrystal) {
                ps.setInt(1, m.objectId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) continue;


                    String nameClan = rs.getString("guild_name");
                    if (nameClan == null) continue;


                    // Link to clan
                    for (Guild c : Guild.entrys) {
                        if (c.name.equals(nameClan)) {

                            m.guild = c;
                            c.addCrystal(m);
                            System.out.println(m.objectId + " Added to " + c.name);
                            break;
                        }
                    }

                    m.isBuffHp = rs.getBoolean("buff");
                    // Load clone data
                    String nb = rs.getString("player_clone");
                    String nbs = rs.getString("player_clone_saved");

                    if (nb != null) {
                        JSONArray jar = (JSONArray) JSONValue.parse(nb);
                        if (jar != null && !jar.isEmpty()) {
                            m.guard = Clone.fromJSONArray(jar);
                            if (m.guard != null) {
                                m.guard.ownerGuild = m.guild;
                                Manager.gI().addClone(m.guard);
                            } else {
                                // fromJSONArray() gagal parse (mis. field korup) -> jangan
                                // masukin null ke cloneList, itu bisa bikin NPE di
                                // GameMap.updateClone() waktu iterasi cloneList nanti.
                                System.out.println("[DailyMine] WARNING mine id=" + m.objectId
                                        + " guild=" + nameClan + " gagal parse player_clone -> clone TIDAK muncul"
                                        + " sampai mine direbut ulang. raw=" + nb);
                            }
                        } else {
                            // Mine ini punya pemilik guild, tapi kolom player_clone kosong/"[]"
                            // (biasanya sisa dari bug lama saat toJSONArray() gagal dan nyimpen
                            // array kosong alih-alih NULL). Akibatnya m.guard tetap null dan
                            // clone-nya invisible di client walau mine-nya tetap dikuasai guild -
                            // dan gak ada exception/log apa pun sebelumnya karena kondisi ini
                            // memang silently di-skip. Sekarang di-log biar kelihatan mine mana
                            // aja yang kena, supaya bisa direbut ulang / di-fix manual di DB.
                            System.out.println("[DailyMine] WARNING mine id=" + m.objectId
                                    + " guild=" + nameClan + " player_clone kosong/korup (raw=\"" + nb
                                    + "\") -> clone invisible sampai mine direbut ulang.");
                        }
                    } else {
                        System.out.println("[DailyMine] WARNING mine id=" + m.objectId
                                + " guild=" + nameClan + " player_clone NULL -> clone invisible sampai mine direbut ulang.");
                    }

                    if (nbs != null) {
                        JSONArray jar = (JSONArray) JSONValue.parse(nbs);
                        if (jar != null && !jar.isEmpty()) {
                            m.guardSave = Clone.fromJSONArray(jar);
                            if (m.guardSave != null) {
                                m.guardSave.ownerGuild = m.guild;
                            }
                        }
                    }

                    // Load clone-clone TAMBAHAN (hasil beli lewat menu tambah clone).
                    // Kolom `extra_clones` berisi JSON array of JSON array (satu per clone).
                    // Dibungkus try-catch supaya server yang belum menjalankan migrasi
                    // "ALTER TABLE mine ADD COLUMN extra_clones TEXT" tetap bisa start
                    // normal (fitur tambah clone otomatis nonaktif sampai kolomnya ada).
                    String neb;
                    try {
                        neb = rs.getString("extra_clones");
                    } catch (Exception colMissing) {
                        neb = null;
                    }
                    if (neb != null) {
                        JSONArray extraArr = (JSONArray) JSONValue.parse(neb);
                        if (extraArr != null && !extraArr.isEmpty()) {
                            for (Object o : extraArr) {
                                JSONArray cloneJar = (JSONArray) o;
                                Clone extra = Clone.fromJSONArray(cloneJar);
                                if (extra != null) {
                                    extra.ownerGuild = m.guild;
                                    m.extraGuards.add(extra);
                                    Manager.gI().addClone(extra);
                                } else {
                                    System.out.println("[DailyMine] WARNING mine id=" + m.objectId
                                            + " guild=" + nameClan + " gagal parse salah satu extra_clones -> clone tambahan itu TIDAK muncul.");
                                }
                            }
                        }
                    }

                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void saveData() {
        try (Connection connection = SQL.gI().getConnection()) {
            connection.setAutoCommit(false);

            String updateQuery = """
                        UPDATE `mine`
                        SET `guild_name` = ?, `buff` = ?, `player_clone` = ?, `player_clone_saved` = ?, `extra_clones` = ?
                        WHERE `id` = ?;
                    """;

            String insertQuery = """
                        INSERT INTO `mine` (`id`, `guild_name`, `buff`, `player_clone`, `player_clone_saved`, `extra_clones`)
                        VALUES (?, ?, ?, ?, ?, ?);
                    """;

            try (
                    PreparedStatement selectStmt = connection.prepareStatement("SELECT `id` FROM `mine` WHERE `id` = ?");
                    PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                    PreparedStatement insertStmt = connection.prepareStatement(insertQuery)
            ) {
                for (Crystal m : listCrystal) {
                    selectStmt.setInt(1, m.objectId);

                    String extraJson = extraGuardsToJson(m);

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {

                            updateStmt.setString(1, m.guild != null ? m.guild.name : null);
                            updateStmt.setBoolean(2, m.isBuffHp);
                            updateStmt.setString(3, m.guard != null ? m.guard.toJSONArray().toJSONString() : null);
                            updateStmt.setString(4, m.guardSave != null ? m.guardSave.toJSONArray().toJSONString() : null);
                            updateStmt.setString(5, extraJson);
                            updateStmt.setInt(6, m.objectId);
                            updateStmt.executeUpdate();
                        } else {
                            // ➕ Insert manually with your own ID
                            insertStmt.setInt(1, m.objectId);
                            insertStmt.setString(2, m.guild != null ? m.guild.name : null);
                            insertStmt.setBoolean(3, m.isBuffHp);
                            insertStmt.setString(4, m.guard != null ? m.guard.toJSONArray().toJSONString() : null);
                            insertStmt.setString(5, m.guardSave != null ? m.guardSave.toJSONArray().toJSONString() : null);
                            insertStmt.setString(6, extraJson);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception e) {
            // Kalau kolom `extra_clones` belum ada di DB (migrasi belum dijalankan),
            // seluruh UPDATE/INSERT di atas bakal gagal & di-rollback (rusak sama
            // sekali fitur save mine yang sudah ada). Makanya migrasi WAJIB dijalankan
            // dulu: ALTER TABLE `mine` ADD COLUMN `extra_clones` TEXT NULL;
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private String extraGuardsToJson(Crystal m) {
        if (m.extraGuards == null || m.extraGuards.isEmpty()) return null;
        JSONArray arr = new JSONArray();
        for (Clone c : m.extraGuards) {
            if (c != null) {
                arr.add(c.toJSONArray());
            }
        }
        return arr.isEmpty() ? null : arr.toJSONString();
    }


}