package ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import game.guild.Guild;
import client.Player;
import core.Manager;
import core.Service;
import client.io.Message;
import template.Item3;
import template.MainObject;
import template.PlayerPart;

public class Clone extends MainObject {

    // Multiplier attack & def clone penjaga tambang relatif terhadap stat player
    // yang merebutnya. 1.5 = +50%.
    private static final double GUARD_STAT_MULTIPLIER = 1.5;

    public List<PlayerPart> wearing = new ArrayList<>();//
    private byte head;//
    private byte eye;//
    private byte hair;//
    private int pointpk;//
    private short clan_icon = -1;
    private int clan_id = -1;
    private String clan_name_clan_shorted;
    private byte clan_mem_type;
    private int[] fashion = new int[0];//
    private short mat_na;//
    private short phi_phong;//
    private short danh_hieu;
    private short weapon;//
    private short id_horse;//
    private short id_hair;//
    private short id_wing;//
    private byte type_use_mount;//
    public long timeAction;
    public boolean isMove;//
    public Player target;
    public int skillId;//

    public long timeATK;
    public long time_hp_buff;
    private int pierce;
    private int crit;

    // Guild pemilik tambang yang menjaga clone ini. Dipakai supaya guild
    // pemilik sendiri tidak bisa menyerang clone-nya sendiri (lihat
    // canBeAttackedByPlayer di bawah). Tidak ikut disimpan ke JSON
    // (toJSONArray/fromJSONArray) - di-set ulang tiap kali clone dibuat atau
    // di-load dari DB (lihat Crystal.setDie() dan DailyMine.loadData()).
    public transient Guild ownerGuild;

    public Clone() {
    }

    /**
     * Clone penjaga tambang tidak boleh diserang player dari guild yang
     * sama dengan pemilik tambang - guild sendiri dianggap kawan, cuma
     * guild lain (atau player tanpa guild) yang boleh menyerang.
     */
    @Override
    public boolean canBeAttackedByPlayer(MainObject attacker) {
        if (ownerGuild != null && attacker != null && attacker.isPlayer()) {
            Guild atkGuild = ((Player) attacker).myclan;
            if (atkGuild != null && atkGuild == ownerGuild) {
                return false;
            }
        }
        return super.canBeAttackedByPlayer(attacker);
    }


    public static Clone fromJSONArray(JSONArray jar) {
        if (jar == null || jar.isEmpty()) return null;

        try {
            Clone clone = new Clone();

            int i = 0;
            clone.map_id = ((Long) jar.get(i++)).byteValue();
            clone.objectId = ((Long) jar.get(i++)).intValue();
            clone.x = ((Long) jar.get(i++)).shortValue();
            clone.y = ((Long) jar.get(i++)).shortValue();
            clone.name = (String) jar.get(i++);

            JSONArray jar2 = (JSONArray) jar.get(i++);
            List<PlayerPart> parts = new ArrayList<>();
            if (jar2 != null && !jar2.isEmpty()) {
                for (Object obj : jar2) {
                    JSONArray partArr = (JSONArray) obj;
                    if (partArr.size() >= 2) {
                        PlayerPart part = new PlayerPart();
                        part.type = ((Long) partArr.get(0)).byteValue();
                        part.part = ((Long) partArr.get(1)).byteValue();
                        parts.add(part);
                    }
                }
            }
            // Selalu di-set (list kosong kalau memang tidak ada data), JANGAN dibiarkan null.
            // send_in4() memanggil this.wearing.size() tanpa null-check, jadi kalau wearing
            // null (kejadian waktu clone di-load dari DB dgn data equipment kosong), lempar
            // NPE yang ditelan diam-diam di MessageHandler -> clone jadi tidak pernah
            // ke-render di client (invisible), walau tetap hidup & bisa diserang di server.
            clone.wearing = parts;

            clone.clazz = ((Long) jar.get(i++)).byteValue();
            clone.head = ((Long) jar.get(i++)).byteValue();
            clone.eye = ((Long) jar.get(i++)).byteValue();
            clone.hair = ((Long) jar.get(i++)).byteValue();
            clone.level = ((Long) jar.get(i++)).byteValue();
            clone.hp = ((Long) jar.get(i++)).intValue();
            clone.maxHp = ((Long) jar.get(i++)).intValue();
            clone.pointpk = ((Long) jar.get(i++)).intValue();
            clone.clan_icon = ((Long) jar.get(i++)).shortValue();
            clone.clan_id = ((Long) jar.get(i++)).intValue();
            clone.clan_name_clan_shorted = (String) jar.get(i++);
            clone.clan_mem_type = ((Long) jar.get(i++)).byteValue();

            // fashion
            JSONArray fashionArr = (JSONArray) jar.get(i++);
            int[] fashion = new int[0];
            if (fashionArr != null && !fashionArr.isEmpty()) {
                fashion = new int[fashionArr.size()];
                for (int j = 0; j < fashionArr.size(); j++) {
                    fashion[j] = ((Long) fashionArr.get(j)).byteValue();
                }
            }
            // Sama seperti wearing di atas: send_in4() memanggil this.fashion.length
            // tanpa null-check, jadi harus di-set ke array kosong, bukan null.
            clone.fashion = fashion;

            clone.mat_na = ((Long) jar.get(i++)).shortValue();
            clone.phi_phong = ((Long) jar.get(i++)).shortValue();
            clone.weapon = ((Long) jar.get(i++)).shortValue();
            clone.id_horse = ((Long) jar.get(i++)).shortValue();
            clone.id_hair = ((Long) jar.get(i++)).shortValue();
            clone.id_wing = ((Long) jar.get(i++)).shortValue();
            clone.danh_hieu = ((Long) jar.get(i++)).shortValue();
            clone.type_use_mount = ((Long) jar.get(i++)).byteValue();
            clone.dame = ((Long) jar.get(i++)).intValue();
            clone.timeAction = (Long) jar.get(i++);
            clone.isMove = (Boolean) jar.get(i++);
            clone.skillId = ((Long) jar.get(i++)).intValue();
            clone.crit = ((Long) jar.get(i++)).intValue();
            clone.time_hp_buff = (Long) jar.get(i++);
            clone.def = ((Long) jar.get(i++)).intValue();
            clone.pierce = ((Long) jar.get(i++)).intValue();

            return clone;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public JSONArray toJSONArray() {
        JSONArray jar = new JSONArray();
        try {
            JSONArray jar2 = new JSONArray();
            jar.add(map_id);
            jar.add(objectId);
            jar.add(x);
            jar.add(y);
            jar.add(name);
            if (wearing != null && !wearing.isEmpty()) {
                for (PlayerPart pr : wearing) {
                    JSONArray jar3 = new JSONArray();
                    jar3.add(pr.type);
                    jar3.add(pr.part);
                    jar2.add(jar3);
                }
            }
            jar.add(jar2);
            //jar2.clear();
            jar.add(clazz);
            jar.add(head);
            jar.add(eye);
            jar.add(hair);
            jar.add(level);
            jar.add(hp);
            jar.add(maxHp);
            jar.add(pointpk);
            jar.add(clan_icon);
            jar.add(clan_id);
            jar.add(clan_name_clan_shorted);
            jar.add(clan_mem_type);
            JSONArray jar4 = new JSONArray();
            if (fashion != null) {
                for (int b : fashion) {
                    jar4.add(b);
                }
            }
            jar.add(jar4);
            //jar2.clear();
            jar.add(mat_na);
            jar.add(phi_phong);
            jar.add(weapon);
            jar.add(id_horse);
            jar.add(id_hair);
            jar.add(id_wing);
            jar.add(danh_hieu);
            jar.add(type_use_mount);
            jar.add(dame);
            jar.add(timeAction);
            jar.add(isMove);

            jar.add(skillId);
            jar.add(crit);
            jar.add(time_hp_buff);
            jar.add(def);
            jar.add(pierce);
        } catch (Exception e) {
            jar.clear();
            e.printStackTrace();
            core.Log.gI().addLogServer("ChiemMo", "Save NhanBan: " + e.getMessage());
        }

        return jar;

    }

    public void create(Player player) {
        this.objectId = Short.toUnsignedInt((short) Manager.gI().get_index_mob_new());
        this.x = player.x;
        this.y = player.y;
        this.wearing = new ArrayList<>();
        for (int i = 0; i < player.item.wear.length; i++) {
            PlayerPart temp_add = new PlayerPart();
            // REVERT: slot 22 (amulet/cincin) dibalik jadi tidak ikut ditampilkan di
            // clone - sama seperti di MapService.send_in4_other_char, mengirim type
            // item ini ke client lain bikin body-nya bolong.
            if (i != 0 && i != 1 && i != 6 && i != 7 && i != 10) {
                continue;
            }
            Item3 temp = player.item.wear[i];
            if (temp != null) {
                temp_add.type = temp.type;
                if (i == 10 && player.item.wear[14] != null && (player.item.wear[14].id >= 4638 && player.item.wear[14].id <= 4648)) {
                    temp_add.part = player.item.wear[14].part;
                } else {
                    temp_add.part = temp.part;
                }
                this.wearing.add(temp_add);
            }
        }
        this.name = "Clone - " + player.name;
        this.ownerGuild = player.myclan;
        this.clazz = player.clazz;
        this.head = player.head;
        this.eye = player.eye;
        this.hair = player.hair;
        this.level = player.level;
        this.hp = player.hp;
        this.maxHp = player.body.getMaxHP();
        this.pointpk = player.pointpk;
        
        // Set guild info - handle null case if player has no guild
        if (player.myclan != null) {
            this.clan_icon = player.myclan.icon;
            this.clan_id = Guild.get_id_clan(player.myclan);
            this.clan_name_clan_shorted = player.myclan.shortName;
            this.clan_mem_type = player.myclan.get_mem_type(player.name);
        } else {
            this.clan_icon = -1;
            this.clan_id = -1;
            this.clan_name_clan_shorted = "";
            this.clan_mem_type = 0;
        }
        this.fashion = player.fashion;
        this.mat_na = Service.getMaskId(player);
        this.phi_phong = Service.getCloakId(player);
        this.weapon = Service.getWeaponId(player);
        this.id_horse = player.mount != null ? player.mount.getPart() : -1;
        this.id_hair = Service.getHairId(player);
        this.id_wing = Service.getWingId(player);
        this.danh_hieu = Service.getTitleId(player);
        this.type_use_mount = player.mount != null ? player.mount.getType() : -1;;
        int baseDame = (player.body.getDameProp(0) + player.body.getDameProp(1) + player.body.getDameProp(2)
                + player.body.getDameProp(3) + player.body.getDameProp(4) );
        int baseDef = player.body.getDefBase();
        // Clone penjaga tambang dibuat lebih kuat dari player aslinya: +50% attack & +50% def,
        // supaya guard tambang lebih tangguh dibanding player yang merebutnya.
        this.dame = (int) Math.round(baseDame * GUARD_STAT_MULTIPLIER);
        this.map_id = player.map.mapId;
        this.crit = player.body.getCrit();
        this.def = (int) Math.round(baseDef * GUARD_STAT_MULTIPLIER);
        this.pierce = player.body.getPierce();
        if (this.pierce > 5000) {
            this.pierce = 5000;
        }
        this.isMove = true;
    }

    public void send_in4(Player p) throws IOException {
        // Defensive: jangan sampai NPE di sini bikin clone invisible secara diam-diam.
        // (fromJSONArray() sekarang sudah tidak pernah kirim null, tapi tetap dijaga
        // untuk jalur lain yang mungkin membuat Clone tanpa lewat fromJSONArray.)
        if (this.wearing == null) {
            this.wearing = new ArrayList<>();
        }
        if (this.fashion == null) {
            this.fashion = new int[0];
        }

        Message m = new Message(5);
        m.writer().writeShort(this.objectId);
        m.writer().writeUTF(this.name);
        m.writer().writeShort(this.x);
        m.writer().writeShort(this.y);
        m.writer().writeByte(this.clazz);
        m.writer().writeByte(-1);
        m.writer().writeByte(this.head);
        m.writer().writeByte(this.eye);
        m.writer().writeByte(this.hair);
        m.writer().writeShort(this.level);
        m.writer().writeInt(this.hp);
        m.writer().writeInt(this.maxHp);
        m.writer().writeByte(0); // type pk
        m.writer().writeShort(this.pointpk);
        m.writer().writeByte(this.wearing.size());
        //
        for (int i = 0; i < this.wearing.size(); i++) {
            m.writer().writeByte(this.wearing.get(i).type);
            m.writer().writeByte(this.wearing.get(i).part);
            m.writer().writeByte(3);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1); // eff
        }
        //
        m.writer().writeShort(this.clan_icon);
        if (clan_icon > -1) {
            m.writer().writeInt(this.clan_id);
            m.writer().writeUTF(this.clan_name_clan_shorted);
            m.writer().writeByte(this.clan_mem_type);
        }
        m.writer().writeByte(-1); // pet
        m.writer().writeByte(this.fashion.length);
        for (int i = 0; i < this.fashion.length; i++) {
            // Format fashion array (byte vs short per elemen) tergantung versi client
            // si PENERIMA paket (p), sama persis kayak MapService.send_in4_other_char().
            // Sebelumnya selalu ditulis sebagai byte, padahal client versi >= 280
            // expect short per elemen. Kalau fashion.length > 0 & client >= 280,
            // semua byte SETELAH bagian ini (mount/mask/cloak/weapon/wing/hair/title,
            // dll -- alias seluruh data yg nentuin badan/kostum clone) jadi kebaca
            // geser oleh client. Nama & posisi tetap kebaca normal karena ada di awal
            // paket (sebelum titik geser ini) -- makanya gejalanya "cuma nickname yang
            // muncul, badannya kosong/invisible".
            if (p != null && p.conn != null && p.conn.version < 280) {
                m.writer().writeByte(this.fashion[i]);
            } else {
                m.writer().writeShort(this.fashion[i]);
            }
        }
        //
        m.writer().writeShort(-1);//id_img_mob
        m.writer().writeByte(this.type_use_mount);
        m.writer().writeBoolean(false);
        m.writer().writeByte(1);
        m.writer().writeByte(0);
        m.writer().writeShort(this.mat_na); // mat na
        m.writer().writeByte(1); // paint mat na trc sau
        m.writer().writeShort(this.phi_phong); // phi phong
        m.writer().writeShort(this.weapon); // weapon
        m.writer().writeShort(this.id_horse);
        m.writer().writeShort(this.id_hair); // hair
        m.writer().writeShort(this.id_wing); // wing
        m.writer().writeShort(-1); // body
        m.writer().writeShort(-1); // leg
        m.writer().writeShort(-1); // bienhinh
        p.conn.addmsg(m);
        m.cleanup();
    }

    @Override
    public int getTypeObject() {
        return 0;
    }


}