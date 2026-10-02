package template;

import ai.Clone;
import game.ai.Location;
import game.guild.Guild;
import client.Player;
import core.Manager;
import client.io.Message;

import java.io.IOException;

import game.map.Eff_player_in_map;
import game.map.GameMap;
import game.map.MapService;

import java.util.ArrayList;
import java.util.List;

public class Crystal extends MainObject {
    // Jumlah maksimum clone penjaga (guard utama + clone tambahan) yang
    // boleh menjaga satu tambang sekaligus. Lihat menu "Tambah Clone
    // Penjaga" di MapService (interaksi serang tambang milik sendiri).
    public static final int MAX_GUARDS = 3;
    // Biaya gems untuk menambah 1 clone penjaga tambahan.
    public static final int ADD_GUARD_GEM_COST = 1000;

    public GameMap gameMap;
    public Clone guard;
    public Clone guardSave;
    // Clone penjaga TAMBAHAN (di luar guard utama) yang dibeli lewat menu
    // tambah clone. Total guard aktif (guard + extraGuards yang belum mati)
    // dibatasi MAX_GUARDS.
    public List<Clone> extraGuards = new ArrayList<>();
    public Guild guild;
    public boolean canAttack;
    public boolean isBuffHp;
    public long timeBuffHp;
    public Location location;

    /**
     * Jumlah clone penjaga yang masih hidup saat ini (guard utama +
     * extraGuards), dipakai untuk mengecek batas MAX_GUARDS sebelum
     * mengizinkan pembelian clone baru.
     */
    public int activeGuardCount() {
        int count = (guard != null && !guard.isdie) ? 1 : 0;
        for (Clone c : extraGuards) {
            if (c != null && !c.isdie) {
                count++;
            }
        }
        return count;
    }

    /**
     * Cari clone TAMBAHAN yang lagi mati (isdie) untuk di-revive. Dicek
     * sebelum mengizinkan beli clone baru, supaya leader revive dulu (100
     * gems) daripada langsung beli baru (1000 gems) padahal slotnya masih ada.
     */
    public Clone findDeadExtraGuard() {
        for (Clone c : extraGuards) {
            if (c != null && c.isdie) {
                return c;
            }
        }
        return null;
    }

    public Crystal(int index, Location location, int hp, int hp_max, int level, GameMap gameMap, String name) {
        this.objectId = Short.toUnsignedInt((short) index);
        this.x = location.getPosition().getX();
        this.y = location.getPosition().getY();
        this.hp = hp;
        this.maxHp = hp_max;
        this.level = (short) level;
        this.gameMap = gameMap;
        this.name = name;
        this.canAttack = false;
        this.isBuffHp = false;
        this.location = location;
    }


    @Override
    public boolean isResourceLoaded() {
        return true;
    }

    @Override
    public int getDefBase() {
        int baseValue = 1000;
        return guild != null ? (baseValue * guild.level) + (500 * guild.members.size()) : baseValue;
    }

    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk) {
        if (hp > 0 || !mainAtk.isPlayer())
            return;
        try {
            this.hp = 0;
            Manager.gI()
                    .chatKTGprocess("@Server : @" + mainAtk.name + " dari guild "
                            + ((Player) mainAtk).myclan.shortName.toUpperCase() + " berhasil merebut "
                            + this.name + " di " + gameMap.name);
            ((Player) mainAtk).myclan.addCrystal(this);
            if (this.guild != null) {
                this.guild.removeCrystal(this);
            }
            this.guild = ((Player) mainAtk).myclan;
            if (this.guard != null) {
                Message m13 = new Message(8);
                m13.writer().writeShort(this.guard.objectId);
                for (Player pl : new ArrayList<>(gameMap.players)) {
                    if (pl != null && pl.conn != null) {
                        pl.conn.addmsg(m13);
                    }
                }
                m13.cleanup();
                Manager.gI().removeClone(this.guard);
            }
            // Tambang direbut guild baru: semua clone TAMBAHAN milik guild
            // lama ikut hilang (bukan cuma guard utama), supaya guild baru
            // mulai dari 1 guard (bisa beli clone tambahan lagi lewat menu).
            if (!this.extraGuards.isEmpty()) {
                for (Clone extra : this.extraGuards) {
                    if (extra == null) continue;
                    Message m13b = new Message(8);
                    m13b.writer().writeShort(extra.objectId);
                    for (Player pl : new ArrayList<>(gameMap.players)) {
                        if (pl != null && pl.conn != null) {
                            pl.conn.addmsg(m13b);
                        }
                    }
                    m13b.cleanup();
                    Manager.gI().removeClone(extra);
                }
                this.extraGuards.clear();
            }
            this.guard = new Clone();
            this.guardSave = this.guard;
            this.guard.create((Player) mainAtk);
            this.guard.skillId = 1;
            Manager.gI().addClone(this.guard);

            Message m12 = new Message(4);
            m12.writer().writeByte(0);
            m12.writer().writeShort(0);
            m12.writer().writeShort(this.guard.objectId);
            m12.writer().writeShort(this.guard.x);
            m12.writer().writeShort(this.guard.y);
            m12.writer().writeByte(-1);
            MapService.sendMsgPlayerInside(gameMap, this, m12, true);
            m12.cleanup();

            // Message(4) di atas cuma ngasih tau client "ada object id X di posisi Y,Z",
            // itu bukan paket render karakter. Model clone (baju/wajah/senjata/dll) baru
            // muncul kalau kita kirim juga paket Message(5) lewat send_in4() -- sama kayak
            // yang dipakai buat spawn mob/NPC/other-player. Tanpa ini clone cuma "ada"
            // secara logic (bisa diserang, punya HP) tapi invisible di layar client.
            for (Player p0 : new ArrayList<>(gameMap.players)) {
                if (p0 != null && p0.conn != null && p0.conn.connected) {
                    this.guard.send_in4(p0);
                }
            }

            this.hp = this.maxHp = 50_000_000;
            Message mm = new Message(7);
            mm.writer().writeShort(this.objectId);
            mm.writer().writeByte((byte) this.level);
            mm.writer().writeShort(this.x);
            mm.writer().writeShort(this.y);
            mm.writer().writeInt(this.hp);
            mm.writer().writeInt(this.maxHp);
            mm.writer().writeByte(0);
            mm.writer().writeInt(4);
            if (this.guild != null) {
                mm.writer().writeShort(this.guild.icon);
                mm.writer().writeInt(Guild.get_id_clan(this.guild));
                mm.writer().writeUTF(this.guild.shortName);
                mm.writer().writeByte(122);
            } else {
                mm.writer().writeShort(-1);
            }
            mm.writer().writeUTF(this.name);
            mm.writer().writeByte(0);
            mm.writer().writeByte(2);
            mm.writer().writeByte(0);
            mm.writer().writeUTF("");
            mm.writer().writeLong(-11111);
            mm.writer().writeByte(4);
            final int a = this.objectId;
            new Thread(() -> {
                try {
                    Thread.sleep(5500L);
                    MapService.sendMsgPlayerInside(gameMap, this, mm, true);
                    mm.cleanup();
                    if (mainAtk.isPlayer())
                        Eff_player_in_map.add((Player) mainAtk, a);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            // Dulu ditelan diam-diam: kalau perebutan tambang gagal di tengah jalan, tidak ada jejak sama sekali.
            System.out.println("[Crystal] setDie gagal pada " + this.name + ": " + e);
            e.printStackTrace();
        }
    }


}