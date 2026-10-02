package template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import client.Pet;
import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import event_daily.CastleSiegeManager;
import client.io.Message;
import game.event.GameEventManager;
import game.event.btf.BTF;
import game.event.btf.Team;
import game.event.kemerdekaan.KristalEvent;
import game.map.Eff_hit_wear22;
import game.map.Eff_special_skill;
import game.map.GameMap;
import game.map.MapItemEffect;
import game.map.MapService;
import game.map.MobInMap;
import game.pvp.PvpManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainObject {

    public String name;
    public int hp, mp;
    public int maxHp, maxMp;
    public boolean isdie, ishs = true, isATK = true;
    public int objectId;
    public short x, xOld, y, yOld;
    public short level;
    public byte map_id, zone_id;
    protected int dame, def;
    public boolean isExp = true;
    public byte color_name;
    public byte typepk;
    public MobTemplate template;
    public long exp;
    public byte clazz;
    public Kham_template kham;
    public int hieuchien;

    private static final int[] EFF_HIT_WEAR22_BY_CLAZZ = {53, 59, 49, 51};

    private static final long EFF_HIT_WEAR22_COOLDOWN_MS = 10_000L;

    private static final long EFF_HIT_WEAR22_BATCH_WINDOW_MS = 400L;

    //eff mount

    private static final int MOUNT_ATTACK_EFF_GRAPHIC_ID = 80;

    private static final long MOUNT_ATTACK_EFF_INTERVAL_MS = 1 * 10_000L;

    private static final Set<Short> MOUNT_ATTACK_EFF_ALLOWED_MOUNT_IDS = Set.of((short) 5285);
    // </editor-fold>


    public void setMaxHP(int maxHp) {
        this.maxHp = maxHp;
    }

    public void setBaseDamage(int dame) {
        this.dame = dame;
    }

    public void setBaseDefense(int def) {
        this.def = def;
    }

    protected List<EffTemplate> effectDefault;

    protected List<EffTemplate> effectMedal;

    public void updateEffect() {
        try {
            if (effectDefault != null) {
                synchronized (effectDefault) {
                    for (int i = effectDefault.size() - 1; i >= 0; i--) {
                        EffTemplate temp = effectDefault.get(i);
                        if (temp.time <= System.currentTimeMillis()) {
                            effectDefault.remove(i);
                            if (isPlayer()) {
                                if (temp.id == -125) {
                                    ((Player) this).set_x2_xp(0);
                                }
                                if (temp.id == 24 || temp.id == 23 || temp.id == 0 || temp.id == 2 || temp.id == 3
                                        || temp.id == 4) {
                                    if (temp.id == 2 && isPlayer()) {
                                        this.hp += ((Player) this).hp_restore;
                                    }
                                    Service.sendMainCharInfo((Player) this);
                                    for (int j = 0; j < ((Player) this).map.players.size(); j++) {
                                        Player p2 = ((Player) this).map.players.get(j);
                                        if (p2 != null && p2.objectId != this.objectId) {
                                            MapService.send_in4_other_char(((Player) this).map, p2, (Player) this);
                                        }
                                    }
                                }
                            }
                            continue;
                        }
                        //
                        if (temp.id == 1 && !this.isdie && isPlayer()) {
                            if (((Player) this).time_affect_special_sk < System.currentTimeMillis()
                                    && ((Player) this).dame_affect_special_sk > 0) {
                                ((Player) this).time_affect_special_sk = System.currentTimeMillis() + 1000L;
                                this.hp -= ((Player) this).dame_affect_special_sk;
                                Service.usepotion(((Player) this), 0, -((Player) this).dame_affect_special_sk);
                                if (this.hp < 0) {
                                    MapService.Player_Die(((Player) this).map, ((Player) this), ((Player) this), true);
                                }
                            }
                        }

                    }
                }
            }
            if (effectMedal != null) {
                synchronized (effectMedal) {
                    for (int i = effectMedal.size() - 1; i >= 0; i--) {
                        EffTemplate temp = effectMedal.get(i);
                        if (temp.time <= System.currentTimeMillis()) {
                            effectMedal.remove(i);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void addEffectDefault(int id, int param1, long time_end) {
        if (effectDefault == null) {
            return;
        }

        if (param1 == 0) {
            return;
        }

        // FIX: synchronize on effectDefault, same lock used by updateEffect(),
        // to avoid ConcurrentModificationException when one thread adds/removes
        // an effect while another thread iterates it (getEffectDefault/isStunes).
        synchronized (effectDefault) {
            for (int i = effectDefault.size() - 1; i >= 0; i--) {
                EffTemplate temp_test = effectDefault.get(i);
                if (temp_test != null && temp_test.id == id) {
                    effectDefault.remove(i);
                }
            }

            effectDefault.add(new EffTemplate(id, param1, time_end));
        }
    }

    public void addEffectMedal(int id, int param1, long time_end) {
        if (effectMedal == null) {
            return;
        }

        // FIX: synchronize on effectMedal so concurrent add/read cannot race,
        // mirroring the effectDefault fix above.
        synchronized (effectMedal) {
            effectMedal.add(new EffTemplate(id, param1, time_end));
        }
    }

    /**
     * Hapus paksa satu efek medali. Dipakai arena PVP untuk mencabut efek yang
     * membuat pemain tidak bisa menyerang / tidak bisa diserang (TangHinh, LuLan)
     * sebelum ronde dimulai — sebelumnya efek dari luar arena terbawa masuk dan
     * bikin salah satu pihak kebal sepanjang pertandingan.
     */
    public void removeEffectMedal(int id) {
        if (effectMedal == null) {
            return;
        }
        synchronized (effectMedal) {
            for (int i = effectMedal.size() - 1; i >= 0; i--) {
                EffTemplate e = effectMedal.get(i);
                if (e != null && e.id == id) {
                    effectMedal.remove(i);
                }
            }
        }
    }

    /** Sama seperti removeEffectMedal, untuk daftar efek default. */
    public void removeEffectDefault(int id) {
        if (effectDefault == null) {
            return;
        }
        synchronized (effectDefault) {
            for (int i = effectDefault.size() - 1; i >= 0; i--) {
                EffTemplate e = effectDefault.get(i);
                if (e != null && e.id == id) {
                    effectDefault.remove(i);
                }
            }
        }
    }

    public EffTemplate getEffectDefault(int id) {
        if (effectDefault == null) {
            return null;
        }

        long time = System.currentTimeMillis();

        synchronized (effectDefault) {
            for (EffTemplate e : effectDefault) {
                if (e.id == id && e.time > time) {
                    return e;
                }
            }
        }

        return null;
    }

    public EffTemplate getEffectMedal(int id) {
        if (effectMedal == null) {
            return null;
        }

        long time = System.currentTimeMillis();
        // FIX: same race as getEffectDefault, guard with the list's own lock.
        synchronized (effectMedal) {
            for (EffTemplate e : effectMedal) {
                if (e.id == id && e.time > time) {
                    return e;
                }
            }
        }

        return null;
    }

    public boolean isStunes(boolean isAtk) {
        if (effectDefault == null) {
            return false;
        }

        long time = System.currentTimeMillis();
        // FIX: same race as getEffectDefault; isStunes() is also called from
        // the combat path (use_skill) so it can run concurrently with
        // addEffectDefault()/updateEffect() on another thread.
        synchronized (effectDefault) {
            for (EffTemplate e : effectDefault) {
                if (e.id >= -124 && e.id <= -121 && (!isAtk || e.id != -123) && e.time > time) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getTypeObject() {
        return 1;
    }

    public int getMaxHP() {
        return maxHp;
    }

    public int getMaxMP() {
        return maxMp;
    }

    public int getHpRegen() {
        return 0;
    }

    public int getMpRegen() {
        return 0;
    }

    public int getBaseDamage() {
        return dame;
    }

    public int getDameProp(int type) {
        return 0;
    }

    public int getPercentDameProp(int type) {
        return 0;
    }

    public int getDefBase() {
        return def;
    }

    public int getPercentDefBase() {
        return 0;
    }

    public int getDefensePercent(int type) {
        return 0;
    }

    public boolean isMob() {
        return false;
    }

    public boolean isResourceLoaded() {
        return false;
    }

    /**
     * Objek yang override ini jadi false tidak bisa diserang oleh player sama
     * sekali (dame otomatis 0, tidak ada efek combat lain) — tapi tetap bisa
     * diserang oleh mob/monster lewat MainObject.attack(). Dipakai misalnya
     * oleh KristalEvent (Persimpangan Kematian) yang cuma boleh diserang
     * monster event, bukan player.
     */
    public boolean canBeAttackedByPlayer() {
        return true;
    }

    /**
     * Varian yang tahu siapa penyerangnya. Dipakai untuk kasus yang butuh
     * pengecualian tergantung siapa yang menyerang (mis. Clone penjaga
     * tambang: tidak boleh diserang player dari guild pemilik tambang itu
     * sendiri, tapi tetap boleh diserang guild lain). Default-nya cuma
     * delegasi ke canBeAttackedByPlayer() supaya objek lain yang belum
     * di-override tidak berubah perilakunya.
     */
    public boolean canBeAttackedByPlayer(MainObject attacker) {
        return canBeAttackedByPlayer();
    }

    public boolean isMobDungeon() {
        return false;
    }

    public boolean isMobDiBuon() {
        return false;
    }

    public boolean isPlayer() {
        return false;
    }

    public boolean isMobCTruongHouse() {
        return false;
    }

    public boolean isBoss() {
        return false;
    }

    public int getPierce() {// tembus armor
        return 0;
    }

    public int getCrit() {
        return 0;
    }

    public int getReflectDamage() {
        return 0;
    }

    public int getMiss() {
        return 0;
    }


    public void setDie(GameMap gameMap, MainObject mainAtk) throws IOException {
        hp = 0;
        isdie = true;

    }

    private static final java.util.concurrent.ConcurrentHashMap<Integer, Long> ARENA_LOG_THROTTLE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * DIAGNOSTIK: kalau serangan pemain vs pemain di arena (map 102) akan ditolak
     * diam-diam oleh guard PK di attack(), cetak alasannya ke console (maks. 1x per
     * 3 detik per penyerang) supaya kelihatan guard mana yang memblokir.
     */
    private static void logArenaBlock(GameMap gameMap, MainObject a, MainObject f) {
        String why = null;
        if (gameMap.ismaplang) why = "ismaplang=true (is_city map 102 di DB)";
        else if (a.level < 11 || f.level < 11) why = "level < 11";
        else if (a.typepk != 0 && a.typepk == f.typepk) why = "typepk sama (" + a.typepk + ")";
        else if (a.hieuchien > 32_000) why = "hieuchien > 32000";
        else if (a.isdie) why = "penyerang isdie";
        else if (a.isStunes(true)) why = "penyerang stun";
        else if (a.getEffectMedal(StrucEff.LuLan) != null) why = "penyerang kena LuLan";
        else if (f.getEffectMedal(StrucEff.TangHinh) != null) why = "target TangHinh";
        else if (Math.abs(a.x - f.x) > 300 || Math.abs(a.y - f.y) > 300) why = "jarak > 300px";
        if (why == null) return;
        long now = System.currentTimeMillis();
        Long last = ARENA_LOG_THROTTLE.get(a.objectId);
        if (last != null && now - last < 3000) return;
        ARENA_LOG_THROTTLE.put(a.objectId, now);
        System.out.println("[ARENA-BLOCK] zone=" + gameMap.zoneId + " " + a.name + "(pk=" + a.typepk
                + ") -> " + f.name + "(pk=" + f.typepk + "): " + why);
    }

    public static void attack(GameMap gameMap, MainObject ObjAtk, MainObject focus, int idxSkill, LvSkill temp, int type)
            throws IOException {


        // <editor-fold defaultstate="collapsed" desc="... tidak bisa menyerang ...">
        if (ObjAtk == null || focus == null || ObjAtk.equals(focus) || ObjAtk.isdie || ObjAtk.isStunes(true)) {
            return;
        }

        if (ObjAtk.isPlayer() && !focus.canBeAttackedByPlayer(ObjAtk)) {
            return; // objek ini cuma boleh diserang monster (mis. KristalEvent),
                     // atau attacker-nya termasuk yang dikecualikan (mis. clone
                     // tambang tidak boleh diserang guild pemiliknya sendiri)
        }


        if (gameMap.mapId == 102 && ObjAtk.isPlayer() && focus.isPlayer()) {
            logArenaBlock(gameMap, ObjAtk, focus);
        }
        if (ObjAtk.isPlayer() && focus.isPlayer() && !gameMap.isCastleSiegeMap()
                && (gameMap.ismaplang || ObjAtk.level < 11 || focus.level < 11
                || (ObjAtk.typepk != 0 && ObjAtk.typepk == focus.typepk) || ObjAtk.hieuchien > 32_000)) {
            return;
        }
        // FIX: kecualikan KristalEvent (Persimpangan Kematian) dari guard inti Castle
        // Siege di bawah. Guard ini seharusnya cuma melindungi objek inti Castle Siege
        // (mob_id 152), tapi KristalEvent bisa ikut kena kalau monster_template yang
        // dipakainya (mis. id 64 "Mining Crystal") kebetulan punya mob_id == 152 di DB,
        // yang bikin SEMUA serangan mob wave ke kristal selalu di-skip diam-diam.
        if (!(focus instanceof KristalEvent)
                && focus.isMob() && focus.template != null && focus.template.mob_id == 152
                && !CastleSiegeManager.isDameTruChinh(gameMap)) {
            return;
        }
        if (Math.abs(ObjAtk.x - focus.x) > 300 || Math.abs(ObjAtk.y - focus.y) > 300) {
            return;
        }
        if (ObjAtk.isStunes(true)) {
            return;
        }
        if (focus.isPlayer() && focus.getEffectMedal(StrucEff.TangHinh) != null) {
            return;
        }
        if (ObjAtk.isPlayer() && ObjAtk.getEffectMedal(StrucEff.LuLan) != null) {
            return;
        }


        if (focus.isdie || focus.hp <= 0 && ObjAtk.isPlayer()) {
            if (focus.isPlayer()) {
                MapService.Player_Die(gameMap, (Player) focus, ObjAtk, false);
            } else {
                // AFK bot: conn mungkin null, MainObj_Die tetap dijalankan tapi skip packet
                Player pAtk = ObjAtk.isPlayer() ? (Player) ObjAtk : null;
                boolean isBot = pAtk != null && pAtk.modeBot;
                // FIX: sebelumnya kalau isBot==false langsung cast ((Player) ObjAtk).conn
                // tanpa cek ulang - kalau ObjAtk BUKAN player (mob membunuh mob lain),
                // pAtk sudah null tapi baris ini tetap maksa cast ObjAtk ke Player ->
                // ClassCastException. Belum ada pemanggil attack() saat ini yang mob-vs-mob,
                // tapi ini jaga-jaga supaya tidak jadi jebakan kalau ada fitur baru nanti.
                MapService.MainObj_Die(gameMap, (isBot || pAtk == null) ? null : pAtk.conn, focus, false);
            }
            return;
        }

        if (ObjAtk.isPlayer() && focus.isPlayer() && focus.typepk == -1)// pembunuhan
        {
            if (ObjAtk.hieuchien > 1000) {
                // AFK bot: skip notice karena conn sudah ditutup
                if (!((Player) ObjAtk).modeBot) {
                    Service.send_notice_box(((Player) ObjAtk).conn,
                            "Tidak bisa membunuh terlalu banyak, perlu membersihkan poin terlebih dahulu.");
                }
                return;
            }
            if (((Player) focus).pet_follow == 4708) {
                // AFK bot: skip notice karena conn sudah ditutup
                if (!((Player) ObjAtk).modeBot) {
                    Service.send_notice_box(((Player) ObjAtk).conn, "Lawan sedang dilindungi pet");
                }
                return;
            }
        }
        // id 99 (ACCURACY): kurangi dodge rate musuh berdasarkan akurasi penyerang
        // dimiliki: Capybara, Barong
        int accuracyBonus = ObjAtk.getTotalItemParam(99);
        int effectiveMiss = Math.max(0, focus.getMiss() - accuracyBonus);
        if (effectiveMiss > Util.random(10_000)) {
            if (focus.isPlayer()) {
                Player pFocus = (Player) focus;
                if (ObjAtk.isMob()) {
                    // Mob/boss menyerang player dan player dodge → kirim ke player yang dodge
                    if (!pFocus.modeBot && pFocus.conn != null) {
                        try {
                            MapService.mob_fire(gameMap, (MobInMap) ObjAtk, pFocus, 0);
                        } catch (Exception ignored) {}
                    }
                } else if (ObjAtk.isPlayer()) {
                    // Player menyerang player dan target dodge
                    // Kirim ke PENYERANG agar animasi serangan tetap tampil (dame=0 = "avoid attack")
                    Player pAtk = (Player) ObjAtk;
                    if (!pAtk.modeBot && pAtk.conn != null) {
                        MapService.firePlayer(gameMap, pAtk.conn, idxSkill, focus.objectId, 0, focus.hp,
                                new ArrayList<>());
                    }
                }
            } else if (ObjAtk.isPlayer()) {
                // Player menyerang mob dan miss
                Player pMiss = (Player) ObjAtk;
                if (!pMiss.modeBot && pMiss.conn != null) {
                    MapService.firePlayer(gameMap, pMiss.conn, idxSkill, focus.objectId, 0, focus.hp,
                            new ArrayList<>());
                }
            }
            return;
        }
        // </editor-fold>


        Player p = ObjAtk.isPlayer() ? (Player) ObjAtk : null;
        EffTemplate ef;
        long dame = ObjAtk.getBaseDamage();
        int hutHP = 0;
        float ptCrit = 0;
        float DamePlus = 0;
        float GiamDame = 0;
        boolean xuyengiap = ObjAtk.getPierce() > Util.random(10_000);

        // <editor-fold defaultstate="collapsed" desc="Get Dame default...">
        if (type == 0) {
            int tempDameProp = ObjAtk.getDameProp(0);
            int dameProp = tempDameProp - (int) (xuyengiap ? 0 : tempDameProp * 0.0001 * focus.getDefensePercent(16));
            dame += dameProp < 0 ? 0 : dameProp;
        } else if (type == 1) {
            switch (ObjAtk.clazz) {
                case 0: {
                    int tempDameProp = ObjAtk.getDameProp(2);
                    int dameProp = tempDameProp
                            - (int) (xuyengiap ? 0 : tempDameProp * 0.0001 * focus.getDefensePercent(18));
                    dame += dameProp < 0 ? 0 : dameProp;
                    break;
                }
                case 1: {
                    int tempDameProp = ObjAtk.getDameProp(4);
                    int dameProp = tempDameProp
                            - (int) (xuyengiap ? 0 : tempDameProp * 0.0001 * focus.getDefensePercent(20));
                    dame += dameProp < 0 ? 0 : dameProp;
                    break;
                }
                case 2: {
                    int tempDameProp = ObjAtk.getDameProp(1);
                    int dameProp = tempDameProp
                            - (int) (xuyengiap ? 0 : tempDameProp * 0.0001 * focus.getDefensePercent(17));
                    dame += dameProp < 0 ? 0 : dameProp;
                    break;
                }
                case 3: {
                    int tempDameProp = ObjAtk.getDameProp(3);
                    int dameProp = tempDameProp
                            - (int) (xuyengiap ? 0 : tempDameProp * 0.0001 * focus.getDefensePercent(19));
                    dame += dameProp < 0 ? 0 : dameProp;
                    break;
                }
            }
        } else {
            dame += ObjAtk.getDameProp(0);


        }
        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Skill...">
        if (ObjAtk.isPlayer()) {
            if (idxSkill == 19 && ObjAtk.clazz == 1) {
                for (Option op : temp.minfo) {
                    if (op.id == 4) {
                        dame += op.getParam(0);
                    }
                    if (op.id == 11) {
                        dame += dame * (op.getParam(0) / 100) / 100;
                    }
                }
                // FIX: Tempel efek DoT racun ke target (mob/player) berdasarkan per_Sub_Eff & time_Sub_Eff
                if (temp.per_Sub_Eff > 0 && Util.random(100) < temp.per_Sub_Eff) {
                    long dotDuration = (long) temp.time_Sub_Eff * 1000L;
                    if (dotDuration <= 0) dotDuration = 5000L; // default 5 detik
                    int dotDamagePerTick = (int) Math.max(1, dame / 5); // 20% dame per tick
                    focus.addEffectDefault(StrucEff.BongDoc, dotDamagePerTick, System.currentTimeMillis() + dotDuration);
                }
            } else {
                for (int i = temp.minfo.length - 1; i >= 0; i--) {
                    Option op = temp.minfo[i];
                    if (type == 0) {
                        if (op.id == 0) {
                            dame += op.getParam(0);
                        }
                        if (op.id == 7) {
                            dame += dame * (op.getParam(0) / 100) / 100;
                        }
                    } else {
                        if (op.id == 1 || op.id == 2 || op.id == 3 || op.id == 4) {
                            dame += op.getParam(0);
                        }
                        if (op.id == 9 || op.id == 10 || op.id == 11 || op.id == 8) {
                            dame += dame * (op.getParam(0) / 100) / 100;
                        }
                    }
                }
            }
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="kuda...">
        if (ObjAtk.isPlayer() && p.mount != null) {
            if (p.mount.getType() == 3) {
                DamePlus += 0.2F;
            } else if (p.mount.getType() == 5) {
                DamePlus += 0.4F;
            } else if (p.mount.getType() == 11 || p.mount.getType() == 12) {
                DamePlus += 0.1F;
            } else if ((p.mount.getType() == 20 && p.mount.getPart() == 114)
                    || (p.mount.getType() == 22 && p.mount.getPart() == 117)) {
                DamePlus += 0.15F;
            } else if ((p.mount.getType() == 20 && p.mount.getPart() == 116)) {
                DamePlus += 0.35F;
            }
        }
        // </editor-fold>
        List<Float> giamdame = new ArrayList<>();
        ef = ObjAtk.getEffectDefault(3);
        if (ef != null) {
            giamdame.add((float) 0.2);
            GiamDame += 0.2F;
            // DamePlus -= 0.2;
            // dame = (dame / 10) * 8;
        }
        if (ObjAtk.isPlayer() && p.getlevelpercent() < 0) {
            giamdame.add((float) 0.5);
            GiamDame += 0.5F;
        }

        // <editor-fold defaultstate="collapsed" desc="Kekuatan sayap...">
        if (ObjAtk.isPlayer()) {
            EffTemplate temp2 = ObjAtk.getEffectDefault(StrucEff.PowerWing);
            if (temp2 == null) {
                Item3 it = p.item.wear[10];
                if (it != null) {
                    int percent = 0;
                    int time = 0;
                    for (Option op : it.op) {
                        if (op.id == 41) {
                            percent = op.getParam(it.tier);
                        } else if (op.id == 42) {
                            time = op.getParam(it.tier);
                        }
                    }
                    if (percent > Util.random(10_000)) {
                        //
                        ObjAtk.addEffectDefault(StrucEff.PowerWing, 1000, time);
                        //
                        Message mw = new Message(40);
                        mw.writer().writeByte(0);
                        mw.writer().writeByte(1);
                        mw.writer().writeShort(ObjAtk.objectId);
                        mw.writer().writeByte(21);
                        mw.writer().writeInt(time);
                        mw.writer().writeShort(ObjAtk.objectId);
                        mw.writer().writeByte(0);
                        mw.writer().writeByte(30);
                        byte[] id__ = new byte[]{7, 8, 9, 10, 11, 15, 0, 1, 2, 3, 4, 14};
                        int[] par__ = new int[]{2000, 2000, 2000, 2000, 2000, 2000,
                                2 * (ObjAtk.getStatInfo(0) / 10), 2 * (ObjAtk.getStatInfo(1) / 10),
                                2 * (ObjAtk.getStatInfo(2) / 10), 2 * (ObjAtk.getStatInfo(3) / 10),
                                2 * (ObjAtk.getStatInfo(4) / 10), 2 * (ObjAtk.getStatInfo(14) / 10)};
                        mw.writer().writeByte(id__.length);
                        //
                        for (int i = 0; i < id__.length; i++) {
                            mw.writer().writeByte(id__[i]);
                            mw.writer().writeInt(par__[i]);
                        }
                        //
                        MapService.sendMsgPlayerInside(p.map, p, mw, true);
                        mw.cleanup();
                    }
                }
            } else {
                DamePlus += 0.2;
            }
        }

        // </editor-fold>
        ef = ObjAtk.getEffectDefault(53);
        int hpmax = ObjAtk.getMaxHP();
        int HoiHP = 0;
        if (ef != null && ObjAtk.hp < hpmax) {
            HoiHP += hpmax / 100;
        }

        // <editor-fold defaultstate="collapsed" desc="Efek rami...">
        boolean isEffKhaiHoan = focus.isPlayer() && focus.getEffectMedal(StrucEff.NgocKhaiHoan) != null;
        int prMeday = 0;
        if (focus.isPlayer()) {
            giamdame.add((float) (focus.getTotalItemParam(80) * 0.0001));
        }
        // GiamDame += focus.isPlayer() ? (float) (((Player) focus).total_item_param(80)
        // * 0.0001) : 0;//armor kegelapan
        if (ObjAtk.isPlayer()) {
            if ((ef = ObjAtk.getEffectMedal(StrucEff.TangHinh)) == null
                    && ObjAtk.getTotalItemParam(82) > Util.random(10_000)) {
                ObjAtk.addEffectMedal(StrucEff.TangHinh, 0,
                        System.currentTimeMillis() + (prMeday = ObjAtk.getTotalItemParam(81)));
                Eff_special_skill.send_eff_TangHinh(p, 81, prMeday);
            } else if ((ef = ObjAtk.getEffectMedal(StrucEff.KhienMaThuat)) == null
                    && ObjAtk.getTotalItemParam(85) > Util.random(10_000)) {
                ObjAtk.addEffectMedal(StrucEff.KhienMaThuat, 0,
                        System.currentTimeMillis() + (prMeday = ObjAtk.getTotalItemParam(86)));
                Eff_special_skill.send_eff_Meday(p, 86, prMeday);
            }
        }
        if (focus.isPlayer() && !isEffKhaiHoan) {
            if (focus.getEffectMedal(StrucEff.BongLua) == null && ObjAtk.getTotalItemParam(76) > Util.random(10_000)) {
                focus.addEffectMedal(StrucEff.BongLua, 0,
                        System.currentTimeMillis() + (prMeday = ObjAtk.getTotalItemParam(77)));
                Eff_special_skill.send_eff_Meday((Player) focus, 77, prMeday);
            } else if (focus.getEffectMedal(StrucEff.BongLanh) == null
                    && ObjAtk.getTotalItemParam(78) > Util.random(10_000)) {
                focus.addEffectMedal(StrucEff.BongLanh, 0,
                        System.currentTimeMillis() + (prMeday = ObjAtk.getTotalItemParam(79)));
                Eff_special_skill.send_eff_Meday((Player) focus, 79, prMeday);
            } else if (focus.getEffectMedal(StrucEff.LuLan) == null
                    && ObjAtk.getTotalItemParam(87) > Util.random(10_000)) {
                focus.addEffectMedal(StrucEff.LuLan, 0,
                        System.currentTimeMillis() + (prMeday = ObjAtk.getTotalItemParam(88)));
                Eff_special_skill.send_eff_Meday((Player) focus, 88, prMeday);
            } else if (focus.getEffectMedal(StrucEff.BongPetir) == null
                    && ObjAtk.getTotalItemParam(184) > Util.random(10_000)) {
                // BongPetir: chance dari option item 184 ("Panggil petir") (per 10.000), durasi tetap 3 detik.
                // Visual listrik = id 6 (sudah ada di client).
                final int petirMs = 3000;
                focus.addEffectMedal(StrucEff.BongPetir, 0, System.currentTimeMillis() + petirMs);
                Eff_special_skill.send_eff_Vip((Player) focus, 6, petirMs, false);
            }
            if (focus.getEffectMedal(StrucEff.KhienMaThuat) != null) {
                GiamDame += 0.5;
                giamdame.add((float) 0.5);
            }
        }
        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Efek inlay...">
        int prKham = 0;
        if (focus.isPlayer() && (ObjAtk.isBoss() || ObjAtk.getTypeObject() == 0)) {
            if (!isEffKhaiHoan && (prKham = focus.getTotalItemParam(101)) > 0) {
                if (focus.kham.idAtk_KH == ObjAtk.objectId) {
                    focus.kham.CountAtk_KH++;
                } else {
                    focus.kham.idAtk_KH = ObjAtk.objectId;
                    focus.kham.CountAtk_KH = 1;
                }

                if (focus.kham.CountAtk_KH >= prKham) {
                    focus.kham.idAtk_KH = 0;
                    focus.kham.CountAtk_KH = 0;
                    focus.addEffectMedal(StrucEff.NgocKhaiHoan, 0, System.currentTimeMillis() + 3000);
                    Eff_special_skill.send_eff_kham((Player) focus, StrucEff.NgocKhaiHoan, 3000);
                }
            }

            if ((ef = focus.getEffectMedal(StrucEff.NgocLucBao)) != null) {
                hutHP += (int) (dame * 0.1);
            } else if ((prKham = focus.getTotalItemParam(102)) > Util.random(10000)) {
                focus.addEffectMedal(StrucEff.NgocLucBao, prKham, System.currentTimeMillis() + 3000);
                Eff_special_skill.send_eff_kham((Player) focus, StrucEff.NgocLucBao, 3000);
            }
        }

        if (ObjAtk.isPlayer()) {
            if ((focus.isBoss() || focus.getTypeObject() == 0)) {
                if (ObjAtk.getEffectMedal(StrucEff.NgocHonNguyen) != null) {
                    DamePlus += 1;
                }
            }

            double ptHP = (ObjAtk.hp / ObjAtk.getMaxHP()) * 100;
            if ((ef = ObjAtk.getEffectMedal(StrucEff.NgocPhongMa)) != null) {
                HoiHP += (int) (hpmax * ef.param * 0.0001);
            } else if (ptHP < ObjAtk.getTotalItemParam(104) / 100
                    && (prKham = ObjAtk.getTotalItemParam(103)) > Util.random(10_000)) {
                ObjAtk.addEffectMedal(StrucEff.NgocPhongMa, prKham, System.currentTimeMillis() + 5000);
                Eff_special_skill.send_eff_kham(p, StrucEff.NgocPhongMa, 5000);
            }

            if (focus.isBoss() && (ef = ObjAtk.getEffectMedal(StrucEff.NgocSinhMenh)) != null) {
                DamePlus += 0.5;
                // dame += (long)(dame * 0.5);
            } else if (focus.isBoss() && (prKham = ObjAtk.getTotalItemParam(106)) > Util.random(10_000)) {
                ObjAtk.addEffectMedal(StrucEff.NgocSinhMenh, prKham, System.currentTimeMillis() + 3000);
                Eff_special_skill.send_eff_kham(p, StrucEff.NgocSinhMenh, 3000);
            }
            ptCrit += ObjAtk.getTotalItemParam(107) * 0.0001;
        }
        // </editor-fold>

        dame += (long) (dame * DamePlus);

        // <editor-fold defaultstate="collapsed" desc="Pet Special Stats ...">
        if (ObjAtk.isPlayer() && p != null) {

            // id 113 — MAX_HP_DAMAGE%: tambah damage berdasarkan % maxHP musuh
            // dimiliki: Kelinci, Phoenix Ice, Ular
            int maxHpDmgPct = ObjAtk.getTotalItemParam(113);
            if (maxHpDmgPct > 0) {
                long refHP = Math.min(focus.getMaxHP(), 500_000L);
                dame += refHP * maxHpDmgPct / 10_000L; 
            }
            // bug EXP Turun
            // data perubaha rumus 500_000L di kali max 8% di bagi 10_000L maka hasilnya 40_000L saja

            // id 67 — FROZEN: chance membekukan musuh (mob & player) selama 3 detik
            // dimiliki: Kelinci, Phoenix Ice, Ular
            int frozenRate = ObjAtk.getTotalItemParam(67);
            if (frozenRate > 0 && Util.random(10_000) < frozenRate) {
                MapService.add_eff_stun(gameMap, ObjAtk, focus, 3, 7);
            }

            // id 47 — HP_RESTORE: pulihkan HP penyerang saat menyerang (% dari damage)
            // dimiliki: Kelalawar
            int petHpRestore = ObjAtk.getTotalItemParam(47);
            if (petHpRestore > 0) {
            //    hutHP += (int) (dame * petHpRestore * 0.0001);
                int healAmt = (int)(dame * petHpRestore * 0.0001);
                p.hp = Math.min(p.hp + healAmt, p.getMaxHP()); // langsung ke player
            }

            // id 44 — MANA_RESTORE: pulihkan MP penyerang saat menyerang (% dari damage)
            // dimiliki: Burung Hantu
            int petMpRestore = ObjAtk.getTotalItemParam(44);
            if (petMpRestore > 0 && p.mp < p.body.getMaxMP()) {
                p.mp = Math.min(p.mp + (int) (dame * petMpRestore * 0.0001), p.body.getMaxMP());
            }
        }
        // </editor-fold>

        int def = focus.getDefBase();
        // def += def * focus.get_PercentDefBase() * 0.0001;
        // if (ObjAtk.isPlayer()) {
        // System.out.println("dame: " + Util.number_format(dame)+" def: "
        // +Util.number_format(def) + " giam: "+GiamDame);
        // }
        if (dame > 2_000_000_000) {
            dame = 2_000_000_000;
        }
        dame -= (long) (dame * 0.5);
        // FIX: mob/boss menyerang player — jangan kurangi def di sini.
        // Sudah ada blok khusus di bawah (~baris 841) yang menerapkan
        // cap resistensi 80% + flat def cap 50%.
        // Kalau dikurangi dua kali, def player selalu membuat dame jatuh ke 1.
        boolean isMobVsPlayer = !ObjAtk.isPlayer() && focus.isPlayer();
        if (!isMobVsPlayer) {
            dame -= (xuyengiap ? 0 : def);
        }
        if (!giamdame.isEmpty()) {
            for (float f : giamdame) {
                dame -= (long) (dame * f);
            }
        }

        //if (ObjAtk.isPlayer() && focus.isMob() && focus.template != null) {
        //    boolean check_mob_roi_ngoc_kham = focus.template.mob_id >= 167 && focus.template.mob_id <= 172;
        //    if (check_mob_roi_ngoc_kham) {
        //        if (50 > Util.random(100)) {
        //            dame = 0;
        //        } else {
        //            dame = 1;
        //       }
        //    }
        //    boolean check = dame < 0
        //            || (focus.isBoss() && focus.template.mob_id == 174 && map.zone_id == 0 && ObjAtk.level > 89)
        //            || (focus.isBoss() && focus.template.mob_id == 174 && map.zone_id == 2
        //            && !(ObjAtk.level >= 90 && ObjAtk.level < 110))
        //            || (focus.isBoss() && focus.template.mob_id == 174 && map.zone_id == 3 && ObjAtk.level < 110);
        //    if (check) {
        //        dame = 0;
        //    }
        // }

        if (focus.isResourceLoaded() && ObjAtk.isPlayer() && focus instanceof Crystal) {
            Crystal mo = (Crystal) focus;
            if (!mo.canAttack) {
                dame = 0;
            } else if (mo.guard != null && !mo.guard.isdie) {
                // Jangan sampai guard nyerang balik player dari guild yang sama
                // dengan pemilik tambang (misal kena splash AOE gak sengaja).
                boolean sameGuild = mo.guild != null && ObjAtk.isPlayer()
                        && ((Player) ObjAtk).myclan == mo.guild;
                if (!sameGuild) {
                    mo.guard.target = (Player) ObjAtk;
                    mo.guard.isMove = false;
                }
            }
        }

        if (ObjAtk.isPlayer() && HoiHP > 0) {
            Service.usepotion(p, 0, HoiHP);
        }

        // FIX: Kemampuan Hisap HP (id 31) — lifesteal % dari dame final ke HP penyerang
        if (ObjAtk.isPlayer() && p != null && dame > 0) {
            int hisapHP = ObjAtk.getTotalItemParam(31);
            if (hisapHP > 0) {
                int healHP = (int) (dame * hisapHP * 0.0001);
                if (healHP > 0) {
                    p.hp = Math.min(p.hp + healHP, p.body.getMaxHP());
                }
            }
            // FIX: Kemampuan Hisap Mana (id 32) — lifesteal % dari dame final ke MP penyerang
            int hisapMP = ObjAtk.getTotalItemParam(32);
            if (hisapMP > 0 && p.mp < p.body.getMaxMP()) {
                int healMP = (int) (dame * hisapMP * 0.0001);
                if (healMP > 0) {
                    p.mp = Math.min(p.mp + healMP, p.body.getMaxMP());
                }
            }
        }

        if (idxSkill == 17 && ObjAtk.isPlayer() && focus.isPlayer()) {
            MapService.add_eff_skill(gameMap, p, (Player) focus, (byte) idxSkill);
        }

        // <editor-fold defaultstate="collapsed" desc="Efek Crit dll ...">
        List<Eff_TextFire> ListEf = new ArrayList<>();

        if (hutHP > 0) {
            ListEf.add(new Eff_TextFire(0, (int) dame));
            ListEf.add(new Eff_TextFire(2, hutHP));
            focus.hp += hutHP;
            if (focus.hp > focus.getMaxHP()) {
                focus.hp = focus.getMaxHP();
            }
        }
        if (xuyengiap) {
            ListEf.add(new Eff_TextFire(1, (int) dame));
        } else if (ObjAtk.getCrit() > Util.random(10_000)) {
            // dame *= 2;
            dame += dame * (ptCrit + 1);
            if (dame > 2_000_000_000) {
                dame = 2_000_000_000;
            }
            ListEf.add(new Eff_TextFire(4, (int) dame));
        }

        // </editor-fold> efek crit dll
        // <editor-fold defaultstate="collapsed" desc="Set hp ...">
        // drop item medan perang
        long time = System.currentTimeMillis();
        if (ObjAtk.isMobCTruongHouse() && gameMap.Arena != null && gameMap.Arena.timeCnNha > time) {
            dame *= 2;
        } else if (!ObjAtk.isPlayer() && ObjAtk.getTypeObject() == 0 && gameMap.Arena != null
                && gameMap.Arena.timeCnLinh > time) {
            dame *= 2;
        }
        if (dame > 2_000_000_000) {
            dame = 2_000_000_000;
        } else if (dame <= 0 && !(!ObjAtk.isPlayer() && focus.isPlayer())) {
            // Jangan clamp ke 1 dulu jika mob/boss menyerang player —
            // blok di bawah yang akan menghitung ulang dengan def yang benar
            dame = 1;
        }
        float ptHP = ((float) focus.hp / focus.getMaxHP()) * 100;
        if (focus.isMobDiBuon()) {
            dame = focus.maxHp * 5 / 100;
        }
        // FIX: untuk mob/boss menyerang player, terapkan def reduction
        if (ObjAtk.getTypeObject() == 1 && focus.isPlayer()) {
            if (ObjAtk.isBoss()) {
                // Boss: nilai dame di DB adalah PERSENTASE (%) dari maxHP player
                // Contoh: damage=20 di DB → 20% dari maxHP player per hit
                // Boss dari init() (dame = level*500, nilai besar) → pakai dame langsung tanpa % dan tanpa def
                long bossBaseDame = ObjAtk.dame; // nilai mentah dari DB
                if (bossBaseDame > 0 && bossBaseDame <= 100) {
                    // Nilai kecil (dari DB boss_event): interpretasikan sebagai %
                    dame = (long) focus.getMaxHP() * bossBaseDame / 100;
                    if (dame <= 0) dame = 1;
                }
                // boss dengan dame besar (dari init) → dame sudah di-set getBaseDamage(), pakai apa adanya
                // Boss bypass flat def & resistensi — hanya dikurangi oleh item khusus anti-boss
            } else {
                // Mob biasa: jika dame DB <= 100, interpretasikan sebagai % dari maxHP player
                // (sama seperti boss). Jika dame > 100, pakai formula resistensi + flat def.
                long mobBaseDame = ObjAtk.dame; // nilai mentah dari DB
                if (mobBaseDame > 0 && mobBaseDame <= 100) {
                    dame = (long) focus.getMaxHP() * mobBaseDame / 100;
                    if (dame <= 0) dame = 1;
                } else {
                    // Mob biasa: terapkan resistensi fisik (cap 80%) + flat def (cap 50% sisa)
                    int targetDef = focus.getDefBase();
                    EffTemplate efDef;
                    efDef = focus.getEffectDefault(14);
                    if (efDef != null) targetDef += efDef.param;
                    efDef = focus.getEffectDefault(15);
                    if (efDef != null) targetDef += (int)(targetDef * (efDef.param * 0.0001));
                    float defPct = focus.getDefensePercent(16) * 0.0001f;
                    if (defPct > 0.8f) defPct = 0.8f;
                    long reduced = dame - (long)(dame * defPct);
                    int flatDefCap = (int)(reduced / 2);
                    reduced -= Math.min(targetDef, flatDefCap);
                    dame = Math.max(1, reduced);
                }
            }
        }
        focus.hp -= dame;

        // <editor-fold defaultstate="collapsed" desc="Eff item.wear[22] saat kita nyerang musuh ...">
        // Nyalain efek part_char type 112 nempel ke MOB, dipicu KHUSUS pas kita
        // yang mukul mob (bukan pas mob yang mukul kita) dan kita pakai slot 22.
        //
        // ATURAN:
        // - Selama masih dalam BATCH WINDOW (EFF_HIT_WEAR22_BATCH_WINDOW_MS) sejak
        //   batch aktif dimulai, SEMUA serangan yang nyentuh mob dapet efek --
        //   mob sama atau beda, kena berkali-kali juga tetap efek muncul tiap kali.
        //   Ini buat nangkep skill AOE yang kena banyak mob sekaligus.
        // - Begitu window itu lewat, COOLDOWN GLOBAL (EFF_HIT_WEAR22_COOLDOWN_MS)
        //   berlaku: gak ada efek muncul lagi sampai cooldown abis, baru boleh
        //   mulai batch baru.
        //
        // Graphic id BEDA per job (clazz) - tinggal ganti angka di array
        // EFF_HIT_WEAR22_BY_CLAZZ kalau mau beda-beda per job:
        // index 0 = Warrior, 1 = Assassin, 2 = Mage, 3 = Gunner.
        if (dame > 0 && ObjAtk.isPlayer() && !focus.isPlayer()) {
            try {
                Player p22 = (Player) ObjAtk;
                if (p22.item.wear[22] != null) {
                    long now = System.currentTimeMillis();
                    boolean batchActive = (now - p22.eff_hit_wear22_batchStart) <= EFF_HIT_WEAR22_BATCH_WINDOW_MS;
                    boolean canStartNewBatch = now >= p22.eff_hit_wear22_nextAllowed;

                    if (batchActive || canStartNewBatch) {
                        if (!batchActive) {
                            // mulai batch baru & pasang cooldown global berikutnya
                            p22.eff_hit_wear22_batchStart = now;
                            p22.eff_hit_wear22_nextAllowed = now + EFF_HIT_WEAR22_COOLDOWN_MS;
                        }

                        int graphicId = (p22.clazz >= 0 && p22.clazz < EFF_HIT_WEAR22_BY_CLAZZ.length)
                                ? EFF_HIT_WEAR22_BY_CLAZZ[p22.clazz] : 51;
                        Eff_hit_wear22.send(gameMap, focus, graphicId, 1500);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // </editor-fold> Eff item.wear[22]

        // <editor-fold defaultstate="collapsed" desc="Eff mount saat kita nyerang musuh/mob ...">
        // Nyalain efek part_char type 111 (jalur "auto", nempel ke entity —
        // lihat MapItemEffect.spawnAuto) ke PLAYER (ikut mount, karena mount
        // dirender jadi satu sama entity player), dipicu tiap kali player yang
        // lagi naik mount berhasil nyerang musuh ATAU mob. Dibatasi cooldown
        // per-player MOUNT_ATTACK_EFF_INTERVAL_MS supaya nggak spam tiap hit,
        // cukup muncul sekali tiap interval selama player terus menyerang.
        // (Sama polanya dengan mount type_use_mount==10 di source lama yang
        // manggil Service.send_eff_auto(p.conn, [p], 80).)
        if (dame > 0 && ObjAtk.isPlayer()) {
            try {
                Player pMount = (Player) ObjAtk;
                Item3 wornMount = pMount.item != null ? pMount.item.wear[21] : null;
                if (wornMount != null && MOUNT_ATTACK_EFF_ALLOWED_MOUNT_IDS.contains(wornMount.id)) {
                    long now = System.currentTimeMillis();
                    if (now >= pMount.mountAttackEff_nextAllowed) {
                        pMount.mountAttackEff_nextAllowed = now + MOUNT_ATTACK_EFF_INTERVAL_MS;
                        MapItemEffect.spawnAuto(gameMap, pMount, MOUNT_ATTACK_EFF_GRAPHIC_ID, pMount);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // </editor-fold> Eff mount

        // <editor-fold defaultstate="collapsed" desc="Refleksi Damage (setelah dame final diterapkan) ...">
        // Refleksi aktif saat player/mob diserang — chance berdasarkan stat refleksi focus
        if (focus.getReflectDamage() > Util.random(10_000) && dame > 0) {
            int DAMEpst = (int) (dame * 0.5);
            DAMEpst -= ObjAtk.getDefBase();
            if (type == 1) {
                if (ObjAtk.clazz == 0) {
                    DAMEpst -= (int)(DAMEpst * 0.0001 * ObjAtk.getDefensePercent(18));
                } else if (ObjAtk.clazz == 1) {
                    DAMEpst -= (int)(DAMEpst * 0.0001 * ObjAtk.getDefensePercent(20));
                } else if (ObjAtk.clazz == 2) {
                    DAMEpst -= (int)(DAMEpst * 0.0001 * ObjAtk.getDefensePercent(17));
                } else if (ObjAtk.clazz == 3) {
                    DAMEpst -= (int)(DAMEpst * 0.0001 * ObjAtk.getDefensePercent(19));
                }
            } else {
                DAMEpst -= (int)(DAMEpst * 0.0001 * ObjAtk.getDefensePercent(16));
            }
            if (DAMEpst <= 0) {
                DAMEpst = 1;
            }
            ListEf.add(new Eff_TextFire(5, DAMEpst));
            ObjAtk.hp -= DAMEpst;
            if (ObjAtk.hp <= 0) {
                // Attacker mati dari refleksi → panggil setDie
                try {
                    ObjAtk.setDie(gameMap, focus);
                } catch (Exception ignored) {
                    ObjAtk.hp = 1;
                }
            }
        }
        // </editor-fold> Refleksi Damage

        MobInMap mob = focus.isMob() ? (MobInMap) focus : null;
        if (focus.isBoss() && mob != null && ObjAtk.isPlayer() && p != null) {
            mob.top_dame.merge(p.name, (long) dame, Long::sum);
        }

        if (focus.hp <= 0) {
            // -130: efek grace period anti-oneshot — diberikan setelah player diselamatkan di 5 HP,
            // supaya hit kecil berikutnya (termasuk pas baru pindah map) tidak langsung membunuh lagi.
            //
            // FIX BUG "HP 5 nyangkut kena boss": sebelumnya syaratnya cuma "focus.isPlayer()",
            // jadi grace ini ikut kepicu walau yang mukul itu MOB/BOSS (PvE), bukan cuma sesama
            // player (PVP) seperti niat aslinya ("anti-oneshot" dari player lain). Akibatnya
            // pemain yang harusnya mati kena boss malah nyangkut di HP 5 -- tidak terhitung
            // mati (setDie()/Player_Die() tidak pernah kepanggil), reward boss tidak masuk,
            // dan baru "resolve" (mati beneran) setelah pindah map. Ditambah syarat
            // ObjAtk.isPlayer() supaya grace ini CUMA jalan kalau yang mukul sesama pemain;
            // kena boss/mob tetap mati normal seperti biasa lewat cabang else di bawah.
            boolean inGracePeriod = focus.isPlayer() && focus.getEffectDefault(-130) != null;
            if (focus.isPlayer() && ObjAtk.isPlayer() && ptHP > 70) {
                focus.hp = 5;
                // FIX RACE CONDITION: antara "focus.hp -= dame" di atas dan baris ini,
                // Body.update() (thread tick terpisah yang jalan tiap detik untuk semua
                // pemain online) bisa saja sempat membaca hp <= 0 dan langsung
                // menge-set focus.isdie = true lewat fallback-nya sendiri, TANPA lewat
                // setDie()/Player_Die() — jadi PvpSession/round-transition tidak pernah
                // tahu, dan tidak ada yang pernah revive() pemain ini lagi. Akibatnya HP
                // sudah diselamatkan ke 5 (kelihatan hidup) tapi isdie nyangkut true
                // selamanya, dan attack() baris pertama (ObjAtk.isdie) menolak semua
                // serangan pemain ini seterusnya — persis gejala "salah satu player
                // ga bisa nyerang" padahal masih berdiri di layar.
                // Paksa isdie balik ke false di sini supaya menang race melawan tick
                // manapun yang sempat kebagian duluan.
                focus.isdie = false;
                focus.addEffectDefault(-130, 1, System.currentTimeMillis() + 1500);
            } else if (inGracePeriod) {
                focus.hp = 1;
                // FIX RACE CONDITION: sama seperti di atas, cabang grace period kedua ini
                // juga rentan disambar tick Body.update() sebelum sempat menyelamatkan HP.
                focus.isdie = false;
            } else {
                if (gameMap.isCastleSiegeMap()) {
                    CastleSiegeManager.Obj_Die(gameMap, ObjAtk, focus);
                }
                focus.setDie(gameMap, ObjAtk);
                if (!focus.isMobDiBuon() && !focus.isPlayer() && focus.template != null && focus.template.mob_id >= 89
                        && focus.template.mob_id <= 92) { // house chien truong

                    Manager.gI().chatKTGprocess(focus.template.name + " was destroyed by " + p.name);
                    BTF btf = GameEventManager.gI().getEvent(BTF.class);
                    if (btf != null) {
                        Team team = btf.getPlayerTeam(p.objectId);
                        if (team != null) {
                            // FIX: Team#addPoint() melempar IOException (kirim paket ke
                            // tiap anggota tim). Sebelumnya exception ini tidak ditangkap
                            // di sini, jadi kalau socket satu pemain saja bermasalah,
                            // destroyTower() dan grantHouseLoot() di bawah ini ikut
                            // TIDAK PERNAH dipanggil. Sekarang dibungkus try-catch,
                            // konsisten dengan pola addPoint() lain di codebase ini.
                            try {
                                team.addPoint(30);
                            } catch (IOException ignore) {
                            }
                        }
                        btf.destroyTower(focus.template.mob_id);
                    }
                    BTF.grantHouseLoot(gameMap, p, mob);
                }
                if (focus.isPlayer()) {
                    MapService.Player_Die(gameMap, focus, ObjAtk, true);
                    if (gameMap.mapId == 102) {
                        PvpManager.gI().onPlayerDie((Player) focus);
                        // King Cup: end_round() sudah me-revive pemain SEBELUM paket
                        // Player_Die (41) di atas terkirim, jadi client menganggap dia
                        // mati lagi. Kirim ulang state hidup supaya tiba paling akhir.
                        if (gameMap.kingCupRef != null) {
                            gameMap.kingCupRef.resyncAliveState();
                        }
                    }
                    if (GameMap.isBattlefieldMap(gameMap.mapId)) {
                        Manager.gI().chatKTGprocess(focus.name + " was killed by " + ObjAtk.name);
                        BTF btf = GameEventManager.gI().getEvent(BTF.class);
                        if (btf != null) {
                            // FIX: ObjAtk bisa berupa MobInMap (rumah/boss "Ular Ratu"
                            // yang membunuh pemain), bukan cuma Player. Cast langsung ke
                            // Player tanpa cek ini akan ClassCastException dan meng-crash
                            // proses attack begitu monster yang membunuh pemainnya (makin
                            // sering terjadi sekarang setelah AI "Penjaga" & buff kecepatan
                            // serang rumah aktif lagi). Poin +5 cuma relevan kalau
                            // pembunuhnya memang pemain lain (PK), bukan monster.
                            if (ObjAtk.isPlayer()) {
                                ((Player) ObjAtk).updatePointArena(5);
                            }
                            btf.onPlayerDie((Player) focus);
                        }
                    }
                } else {
                    // FIX BUG DAILY QUEST: dulu di sini ada panggilan DailyQuest.on_mob_killed()
                    // + on_party_kill() lagi, padahal focus.setDie(gameMap, ObjAtk) di atas
                    // (baris ~1161) sudah memanggil MobInMap.setDie(), yang di dalamnya SUDAH
                    // memanggil kedua hook DailyQuest itu (lihat MobInMap.setDie(), baik untuk
                    // boss maupun mob biasa). Akibatnya progress quest_daily[2] nambah +2 per
                    // kill (bukan +1) untuk setiap kill lewat attack(), jadi misi selesai dua
                    // kali lebih cepat dari target sebenarnya. Dihapus di sini, cukup dari
                    // MobInMap.setDie() saja (yang juga sudah benar-benar tertangani untuk
                    // kasus boss dengan banyak hitter).
                    MapService.MainObj_Die(gameMap, null, focus, true);
                }

            }

        }

        log.debug("FIRE PLAYER {} ", focus.objectId);
        if (ObjAtk.isPlayer() && (focus.isPlayer() || focus.getTypeObject() == 0)) {
            if (p.modeBot) {
                // Bot AFK: broadcast ke semua player di sekitar agar visual skill terlihat
                MapService.firePlayerBot(gameMap, p, idxSkill, focus.objectId, (int) dame, focus.hp, ListEf);
            } else if (p.conn != null) {
                MapService.firePlayer(gameMap, p.conn, idxSkill, focus.objectId, (int) dame, focus.hp, ListEf);
            }

        } else if (ObjAtk.isPlayer() && focus.getTypeObject() == 1) {
            if (p.modeBot) {
                // Bot AFK: broadcast ke semua player di sekitar agar visual skill terlihat
                int mobId = (focus.template != null) ? focus.template.mob_id : 0;
                MapService.Fire_Mob_Bot(gameMap, p, idxSkill, focus.objectId, (int) dame, focus.hp, ListEf, mobId);
            } else if (p.conn != null) {
                if (GameMap.isBattlefieldMap(gameMap.mapId) && focus.template != null) {
                    BTF btf = GameEventManager.gI().getEvent(BTF.class);
                    Team team;
                    if (btf != null && (team = btf.getPlayerTeam(p.objectId)) != null) {
                        if (team.getTowerId() == focus.template.mob_id) {
                            return;
                        }
                    }
                    MapService.Fire_Mob(gameMap, p.conn, idxSkill, focus.objectId, (int) dame, focus.hp, ListEf,
                            focus.template.mob_id);
                } else {
                    MapService.Fire_Mob(gameMap, p.conn, idxSkill, focus.objectId, (int) dame, focus.hp, ListEf, 0);
                }
            }
        } else if (ObjAtk.getTypeObject() == 1 && focus.isPlayer()) {
            // Mob menyerang player — kirim dengan ListEf agar efek refleksi (type=5) ikut terkirim
            MapService.mob_fire(gameMap, (MobInMap) ObjAtk, (Player) focus, (int) dame, ListEf);

        } else if (ObjAtk.getTypeObject() == 0 && focus.isPlayer()) {
            MapService.MainObj_Fire_Player(gameMap, (Player) focus, ObjAtk, idxSkill, (int) dame, ListEf);
            System.out.println("Clone Dame " + dame);
        }

        // if (ObjAtk.isPlayer()) {
        //
        // System.out.println("dame: " + Util.number_format(dame)+"def: "
        // +Util.number_format(def));
        // }

        // <editor-fold defaultstate="collapsed" desc="Pet Multi-target (id 114) ...">
        // id 114 — TOTAL_TARGET: serang beberapa mob sekaligus di sekitar target utama
        // dimiliki: Zabawiaka, Hantu, Kelinci, Phoenix Ice, Peri, Malaikat, Kucing, Ular
        if (ObjAtk.isPlayer() && p != null && focus.isMob() && !focus.isdie) {
            int totalTarget = ObjAtk.getTotalItemParam(114);
            if (totalTarget > 1 && gameMap.mobs != null) {
                int hitCount = 0;
                int splashDame = (int) Math.max(1, dame / 2); // splash = 50% dari damage utama
                for (MobInMap splashMob : gameMap.mobs) {
                    if (hitCount >= totalTarget - 1) break; // -1 karena target utama sudah kena
                    if (splashMob == null || splashMob.isdie || splashMob.equals(focus)) continue;
                    if (Math.abs(splashMob.x - focus.x) > 150 || Math.abs(splashMob.y - focus.y) > 150) continue;
                    int actualDame = Math.max(1, splashDame - splashMob.getDefBase());
                    splashMob.hp -= actualDame;
                    MapService.Fire_Mob(gameMap, p.conn, idxSkill, splashMob.objectId,
                            actualDame, splashMob.hp, new ArrayList<>(), splashMob.template != null ? splashMob.template.mob_id : 0);
                    if (splashMob.hp <= 0) {
                        splashMob.setDie(gameMap, ObjAtk);
                    }
                    hitCount++;
                }
            }
        }
        // </editor-fold> Pet Multi-target

        // </editor-fold> Set hp
        // <editor-fold defaultstate="collapsed" desc="Hitung exp ...">
        if (focus.isMobDungeon()
                && ObjAtk.isPlayer()) {
            int expup = 0;
            // FIX: sama seperti cabang exp mob biasa di bawah — clamp dame
            // ke rentang int yang aman dulu. Di cabang ini updateExp() dipanggil
            // TANPA syarat expup > 0, jadi kalau dame overflow jadi negatif,
            // player bisa-bisa malah KEHILANGAN exp alih-alih cuma dapat 0.
            long dameForExp = Math.max(0L, Math.min(dame, 2_000_000_000L));
            expup = (int) dameForExp; // tinh exp
            ef = p.getEffectDefault(-125);
            if (ef != null) {
                expup += (expup * (ef.param / 100)) / 100;
            }
            expup = (int) (expup / 3);
            // Damage kecil (dame < 3) kena bulatin ke 0 padahal player berhasil
            // ngedamage mob-nya — kasih minimal 1 exp biar nggak dapet 0xp
            if (expup <= 0 && dame > 0) {
                expup = 1;
            }
            p.updateExp(expup, true);
            // exp clan
            if (p.myclan != null) {
                int exp_clan = ((int) dame) / 10_000;
                if (exp_clan < 1 || exp_clan > 50) {
                    exp_clan = 1;
                }
                p.myclan.updateExp(exp_clan);
            }
        } else if (focus.isMob()
                && focus.isExp && ObjAtk.isPlayer()) {
            int expup = 0;
            // FIX: kalau dame kebetulan overflow int (misal dari sumber damage
            // lain yang belum di-cap di masa depan) dan jadi negatif, jangan
            // ikut dijadiin expup negatif — nanti "expup > 0" di bawah bakal
            // selalu false dan player diam-diam dapat 0xp walau berhasil
            // ngedamage mob-nya. Clamp dame ke rentang int yang aman dulu.
            long dameForExp = Math.max(0L, Math.min(dame, 2_000_000_000L));
            expup = (int) dameForExp; // tinh exp
            if (p.level <= 10) {
                expup = expup * 3;
            }
            // Calculate level difference bonus/penalty
            int level_diff = focus.level - p.level; // positive = mob higher, negative = mob lower

            if (level_diff > 0) {
                // Mob is higher level - give bonus EXP
                int bonus_percent = Math.min(level_diff * Manager.exp_bonus_per_level, 200); // cap at 200%
                expup = expup + (expup * bonus_percent / 100);
            } else if (level_diff < 0) {
                // Mob is lower level - reduce EXP
                int penalty_percent = Math.min(Math.abs(level_diff) * Manager.exp_penalty_per_level, 80); // cap at 80%
                // reduction
                expup = expup - (expup * penalty_percent / 100);
            }
            // Same level = no change
            if (p.hieuchien > 0) {
                expup /= 2;
                // Jaga minimal 1 exp kalau sebelum dibagi 2 masih > 0, biar
                // hit damage kecil pas hieuchien aktif nggak kepotong jadi 0
                if (expup <= 0 && dame > 0) {
                    expup = 1;
                }
            }
            // CATATAN: rate exp admin (Manager.gI().exp, skala persen — 100 =
            // x1 normal) diterapkan sekali di Player.updateExp() untuk SEMUA
            // sumber exp (kill mob, dungeon, daily quest, dll). Baris
            // "Manager.exp_multiplier" yang lama dihapus dari sini karena
            // bikin exp dikali 2x (double multiply) waktu admin ubah rate
            // lewat menu "Setting Exp" — exp jadi jauh lebih gede dari yang
            // diminta admin.

            if (Math.abs(focus.level - p.level) <= Manager.exp_level_difference && expup > 0) {
                // Full EXP for appropriate level difference
                if (p.party != null) {
                    for (int i = 0; i < p.party.get_mems().size(); i++) {
                        Player pm = p.party.get_mems().get(i);
                        if (pm.objectId != p.objectId && pm.map.mapId == p.map.mapId
                                && pm.map.zoneId == p.map.zoneId) {
                            // expup kecil * 30 / 100 gampang kepotong ke 0,
                            // party member jadi nggak dapet exp bagian sama sekali
                            int partyExp = (expup * 30) / 100;
                            if (partyExp <= 0 && expup > 0) {
                                partyExp = 1;
                            }
                            pm.updateExp(partyExp, true);
                        }
                    }
                }
                ef = p.getEffectDefault(-125);
                if (ef != null) {
                    expup += (expup * (ef.param / 100)) / 100;
                }

                // Apply equipment EXP boost (option ID 51)
                int equipExpBoost = p.getTotalItemParam(51);
                if (equipExpBoost > 0) {
                    expup += (expup * equipExpBoost) / 10000;
                }

                p.updateExp(expup, true);
            } else if (expup > 0) {
                // Reduced EXP for large level difference, but not just 2
                int reduced_exp = Math.max(expup / 5, 10); // At least 10 EXP, or 20% of normal
                p.updateExp(reduced_exp, false);
            }
            // exp clan
            if (p.myclan != null) {
                int exp_clan = ((int) dame) / 10_000;
                if (exp_clan < 1 || exp_clan > 50) {
                    exp_clan = 1;
                }
                p.myclan.updateExp(exp_clan);
            }

            if (p.it_wedding != null) {
                if (p.party != null && p.party.get_mems() != null) {
                    for (int i = 0; i < p.party.get_mems().size(); i++) {
                        Player pm = p.party.get_mems().get(i);
                        if (p.it_wedding.equals(pm.it_wedding)) {
                            synchronized (p.it_wedding) {
                                long expBefore = p.it_wedding.exp;
                                p.it_wedding.exp += dame / 1_000;
                                // Save ke DB setiap kenaikan 1000 exp agar tidak hilang tanpa restart server
                                if (p.it_wedding.exp / 1_000 > expBefore / 1_000) {
                                    p.it_wedding.saveItem();
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        // </editor-fold> Hitung exp

        // <editor-fold defaultstate="collapsed" desc="Pet Attack ...">
        if (ObjAtk.isPlayer()) {
            if (!focus.isdie && p.pet_follow != -1) {
                Pet my_pet = null;
                for (Pet pett : p.mypet) {
                    if (pett.is_follow) {
                        my_pet = pett;
                        break;
                    }
                }
                if (my_pet != null && my_pet.grown > 0) {
                    int a1 = 0;
                    int a2 = 0;
                    // Cari option damage pet (id 0-4 = physical/ice/fire/lightning/poison)
                    for (PetOption temp1 : my_pet.op) {
                        if (temp1.id >= 0 && temp1.id <= 4 && temp1.maxValue > 0) {
                            a1 = temp1.value;
                            a2 = temp1.maxValue;
                            break;
                        }
                    }
                    // Fallback: cari option apapun yang punya maxValue > 0 (selain id 23-26)
                    if (a2 == 0) {
                        for (PetOption temp1 : my_pet.op) {
                            if (temp1.id < 23 && temp1.maxValue > 0) {
                                a1 = temp1.value;
                                a2 = temp1.maxValue;
                                break;
                            }
                        }
                    }
                    int dame_pet = Util.random(a1, Math.max(a2, a1 + 1));
                    if (dame_pet <= 0) {
                        dame_pet = 1;
                    }
                    if (((focus.hp - dame_pet) > 0) && (p.pet_atk_speed < System.currentTimeMillis()) && (a1 > 0)) {
                        if (focus.isMob() && (my_pet.getId() == 3269 || my_pet.name.equals("Elang Besar"))) {
                            int vangjoin = Util.random(2000, 5000);
                            p.updateGold(vangjoin);
                            // AFK bot: skip notice jika conn sudah ditutup
                            if (!p.modeBot && p.conn != null) { Service.send_notice_nobox_white(p.conn, "+ " + vangjoin + " emas"); }
                        }
                        p.pet_atk_speed = System.currentTimeMillis() + 1500L;
                        Message m = new Message(84);
                        m.writer().writeByte(2);
                        m.writer().writeShort(p.objectId);
                        m.writer().writeByte(focus.getTypeObject());
                        m.writer().writeByte(1);
                        m.writer().writeShort(focus.objectId);
                        m.writer().writeInt(dame_pet);
                        focus.hp -= dame_pet;
                        m.writer().writeInt(focus.hp);
                        m.writer().writeInt(p.hp);
                        m.writer().writeInt(p.body.getMaxHP());
                        // AFK bot: skip packet jika conn sudah ditutup
                        if (!p.modeBot && p.conn != null) { p.conn.addmsg(m); }
                        m.cleanup();
                    }
                }
            }
        }
        // </editor-fold> Pet Attack

    }

    public int getStatInfo(int type) {
        return switch (type) {
            case 0, 1, 2, 3, 4 -> getDameProp(type);
            case 7, 8, 9, 10, 11 -> getPercentDameProp(type);
            case 14 -> getDefBase();
            case 15 -> getPercentDefBase();
            case 33 -> getCrit();
            case 34 -> getMiss();
            case 35 -> getReflectDamage();
            case 36 -> getPierce();
            case 16, 17, 18, 19, 20, 21, 27, 28, 29, 30, 22 -> (getTotalItemParam(type) + getTotalSkillParam(type));
            case 112 -> getArenaPoints();
            case 55 -> getMerchantPoints();
            case 181 -> getGemRechargePoints(); 
            case 182 -> getGemUsagePoints();
            case 183 -> getGoldRechargePoints();
            default -> getTotalItemParam(type);
        };
    }

    public int getArenaPoints() {
        return 0;
    }

    public int getMerchantPoints() {
        return 0;
    }

    /**
     * StatType.GEM_RECHARGE_POINTS (id 181) - "Poin isi ulang permata".
     * Akumulasi jumlah permata dari user_topup (topup.TopupController#find), bukan dari stat item.
     */
    public int getGemRechargePoints() {
        return 0;
    }

    /**
     * StatType.GEM_USAGE_POINTS (id 182) - "Poin penggunaan permata".
     * Belum ada sistem pemakaian permata yang tercatat, disediakan hook-nya saja.
     */
    public int getGemUsagePoints() {
        return 0;
    }

    public int getGoldRechargePoints() {
        return 0;
    }

    public int getTotalSkillParam(int id) {
        return 0;
    }

    public int getTotalItemParam(int id) {
        return 0;
    }

    public void update(GameMap gameMap) {

    }
}