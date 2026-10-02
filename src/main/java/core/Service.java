package core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import game.items.ItemManager;
import game.map.*;
import game.shop.ShopManager;
import lombok.extern.slf4j.Slf4j;
import menu.Dialog;
import menu.InputDialog;
import menu.Menu;
import game.shop.TokenShop;
import feature.admin.AdminMenuController;
import game.guild.Guild;
import history.His_DelItem;
import client.Pet;
import client.Player;
import client.io.Message;
import client.io.Session;
import model.UpgradeData;
import model.UpgradeLevel;
import model.item.Item;
import model.item.Reward;
import model.map.MapName;
import template.*;
import utils.IconHelper;

@Slf4j
public class Service {


    public static void send_msg_data(Session conn, int cmd, String name) throws IOException {
        Message m = new Message(cmd);
        m.writer().write(Util.loadfile("data/msg/" + name));
        conn.addmsg(m);
        m.cleanup();
    }

    public static void sendNameServer(Session s) throws IOException {
        Message m = new Message(61);
        m.writer().writeByte(MapManager.getInstance().getMapName().size());
        for (MapName name : MapManager.getInstance().getMapName().values()) {
            m.writer().writeUTF(name.getName());
        }

        // Quest
        m.writer().writeByte(1);
        m.writer().writeUTF("test");

        UpgradeData upgradeData = MapManager.getInstance().getUpgradeData();
        m.writer().writeByte(upgradeData.getMaterials().length);
        for (short id : upgradeData.getMaterials()) {
            m.writer().writeShort(id);
        }

        m.writer().writeByte(upgradeData.getLevels().size());
        for (UpgradeLevel level : upgradeData.getLevels()) {
            m.writer().writeByte(level.getLevel());
            m.writer().writeInt(level.getGold());
            m.writer().writeShort(level.getGem());
            for (int i=0; i<4; i++) {
                m.writer().writeByte(level.getValue()[i]);
            }
        }

        // m.writer().write(Manager.gI().msg_61);
        s.addmsg(m);
        m.cleanup();
    }

    public static void send_msg_data(Session conn, int cmd, byte[] data) throws IOException {
        Message m = new Message(cmd);
        m.writer().write(data);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_item_template(Session conn) throws IOException {
        Message m = new Message(25);
        m.writer().writeShort(ItemTemplate4.item.size());
        for (ItemTemplate4 temp : ItemTemplate4.item) {
            m.writer().writeShort(temp.getId());
            m.writer().writeShort(temp.getIcon());
            m.writer().writeLong(temp.getPrice());
            m.writer().writeUTF(temp.getName());
            m.writer().writeUTF(temp.getContent());
            m.writer().writeByte(temp.getType());
            m.writer().writeByte(temp.getPricetype());
            m.writer().writeByte(temp.getSell());
            m.writer().writeShort(temp.getValue());
            m.writer().writeBoolean(temp.getTrade() == 1);
        }
        //
        m.writer().writeByte(OptionItem.entrys.size());
        for (OptionItem temp : OptionItem.entrys) {
            m.writer().writeUTF(temp.getName());
            m.writer().writeByte(temp.getColor());
            m.writer().writeByte(temp.getIspercent());
        }
        //
        if (conn.zoomlv > 1) {
            m.writer().writeShort(ItemTemplate7.item.size());
            for (ItemTemplate7 temp : ItemTemplate7.item) {
                m.writer().writeShort(temp.getId());
                m.writer().writeShort(temp.getIcon());
                m.writer().writeLong(temp.getPrice());
                m.writer().writeUTF(temp.getName());
                m.writer().writeUTF(temp.getContent());
                m.writer().writeByte(temp.getType());
                m.writer().writeByte(temp.getPricetype());
                m.writer().writeByte(temp.getSell());
                m.writer().writeShort(temp.getValue());
                m.writer().writeByte(temp.getTrade());
                m.writer().writeByte(temp.getColor());
            }
        } else {
            m.writer().writeShort(0);
        }

        // PRICE SETTINGS
        SvConfig sv = Manager.svConfig;

        m.writer().writeShort(sv.getPriceSellPotion());
        m.writer().writeShort(sv.getPriceSellItem());
        m.writer().writeShort(sv.getHesoLevel());
        m.writer().writeShort(sv.getHesoColor());
        m.writer().writeShort(sv.getPriceSellQuest());
        m.writer().writeShort(sv.getMaxPriceItem());
        m.writer().writeShort(sv.getPriceClanIcon());
        m.writer().writeByte(sv.getPriceChatWorld());

        // PET TEMPLATE
        m.writer().writeByte(sv.getPetTemplate().size());
        for (PetType pet : sv.getPetTemplate()) {

            m.writer().writeShort(pet.getId());
            m.writer().writeByte(pet.getType());

        }

        //CRAFT MATERIAL
        if (sv.getCraftMaterial() != null && sv.getCraftMaterial().length > 0) {
            m.writer().writeByte(sv.getCraftMaterial().length);
            for (int id : sv.getCraftMaterial()) {
                m.writer().writeShort(id);
            }
        }

        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_notice_box(Session conn, String s) throws IOException {
        Message m2 = new Message(37);
        m2.writer().writeUTF(s);
        m2.writer().writeUTF("");
        m2.writer().writeByte(15);
        conn.addmsg(m2);
        m2.cleanup();
    }

    public static void send_quest(Session conn) throws IOException {
        Message m = new Message(52);
        m.writer().writeByte(10);
        m.writer().writeByte(10);
        m.writer().writeByte(10);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_auto_atk(Session conn) throws IOException {
        Message m = new Message(-108);
        m.writer().writeByte(5);
        m.writer().writeByte(0);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void sendMainCharInfo(Player p) throws IOException {
        // try{
        int hpMax = p.body.getMaxHP();
        int mpMax = p.body.getMaxMP();
        if (p.hp > hpMax) {
            p.hp = hpMax;
        }
        if (p.mp > mpMax) {
            p.mp = mpMax;
        }
        Message m = new Message(3);
        m.writer().writeShort(p.objectId);
        m.writer().writeUTF(p.getDisplayName());
        m.writer().writeInt(p.hp);
        m.writer().writeInt(hpMax);
        m.writer().writeInt(p.mp);
        m.writer().writeInt(mpMax);
        m.writer().writeByte(p.head);
        m.writer().writeByte(p.clazz);
        m.writer().writeByte(p.eye);
        m.writer().writeByte(p.hair);
        //
        byte[] i1 = new byte[]{0, 1, 2, 3, 4, 53, 54, 55, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19, 20, 27, 28, 29, 30, 33, 34,
                35, 36, 40, 112, -75, -74, -73};
        m.writer().writeByte(i1.length);
        for (int i = 0; i < i1.length; i++) {
            m.writer().writeByte(i1[i]);
            m.writer().writeInt(p.body.getStatInfo(i1[i] & 0xFF));
        }
        ///
        m.writer().writeShort(p.level); // lv
        m.writer().writeShort(p.getlevelpercent()); // lv percent
        m.writer().writeShort(p.tiemnang); // tiem nang
        m.writer().writeShort(p.kynang); // ky nang
        ///
        m.writer().writeShort(p.point1); // tiem nang goc
        m.writer().writeShort(p.point2);
        m.writer().writeShort(p.point3);
        m.writer().writeShort(p.point4);
        ///
        m.writer().writeShort(p.body.getPlusPoint(23)); // tiem nang them
        m.writer().writeShort(p.body.getPlusPoint(24));
        m.writer().writeShort(p.body.getPlusPoint(25));
        m.writer().writeShort(p.body.getPlusPoint(26));
        ///// skill point
        for (int i = 0; i < 21; i++) {
            m.writer().writeByte(p.skillPoint[i]);
        }
        // skill plus point
        for (int i = 0; i < 21; i++) {
            int pointP = p.body.getSkillPointPlus(i);
            // if(pointP>0){
            // LvSkill temp = p.skills[i].mLvSkill[p.skill_point[i] + (pointP -1)];
            // while(pointP > 0 && temp.LvRe > p.level){
            // temp = p.skills[i].mLvSkill[p.skill_point[i] + (pointP -1)];
            // pointP --;
            // }
            // }

            m.writer().writeByte(pointP);
        }
        m.writer().writeByte(p.typepk);
        m.writer().writeShort(p.pointpk);
        m.writer().writeByte(p.maxbag); // max bag
        if (p.myclan != null) {
            m.writer().writeShort(p.myclan.icon);
            m.writer().writeInt(Guild.get_id_clan(p.myclan));
            m.writer().writeUTF(p.myclan.shortName);
            m.writer().writeByte(p.myclan.get_mem_type(p.name));
        } else {
            m.writer().writeShort(-1); // clan
        }
        m.writer().writeUTF("k2: ");
        m.writer().writeLong(0);
        m.writer().writeByte(p.fashion.length);
        for (int i = 0; i < p.fashion.length; i++) {
            if (p.conn.version < 280) {
                m.writer().writeByte(p.fashion[i]);
            } else {
                m.writer().writeShort(p.fashion[i]);
            }
        }
        m.writer().writeByte(3); // top-up money?
        m.writer().writeShort(getMaskId(p)); // mask id
        m.writer().writeByte(1); // render mask (front/back)
        m.writer().writeShort(getCloakId(p)); // cloak id
        m.writer().writeShort(getWeaponId(p)); // weapon id
        m.writer().writeShort(p.mount != null ? p.mount.getPart() : -1); // horse id
        m.writer().writeShort(getHairId(p)); // hair id
        m.writer().writeShort(getWingId(p)); // wing id
        m.writer().writeShort(getTitleId(p)); // title id

        m.writer().writeShort(-1); // name id

        m.writer().writeShort(p.bodyEffectId); // body effect id (112_xx.png)
        m.writer().writeShort(p.legEffectId);  // leg effect id  (112_xx.png)
        m.writer().writeShort(-1); // idBienhinh
        //
        p.conn.addmsg(m);
        m.cleanup();
        // }catch(Exception e){
        // e.printStackTrace();
        // }
    }

    public static short getHairId(Player p) {
        if (p.fashionSetting.isHair()) return -1;

        short result = -1;
        if (p.item.wear[16] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[16].id);
            if (result == -1) {
                result = (short) (p.item.wear[16].part + 41);
            }
        }
        return result;
    }

    public static short getWeaponId(Player p) {
        if (p.fashionSetting.isWeapon()) return -1;
        short result = -1;
        if (p.item.wear[17] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[17].id);
            if (result == -1) {
                result = (short) (p.item.wear[17].part + 41);
            }
        }
        return result;
    }

    public static short getCloakId(Player p) {
        if (p.fashionSetting.isCloak()) return -1;

        short result = -1;
        if (p.item.wear[15] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[15].id);
            if (result == -1) {
                result = (short) (p.item.wear[15].part + 41);
            }
        }

        return result;
    }

    public static short getTitleId(Player p) {
        if (p.fashionSetting.isTitle()) return -1;

        short result = -1;
        if (p.item.wear[19] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[19].id);
            if (result == -1) {
                result = (short) (p.item.wear[19].part + 41);
            }
        }
        return result;
    }

    public static short getWingId(Player p) {
        if (p.fashionSetting.isWing()) return -1;

        short result = -1;
        if (p.item.wear[14] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[14].id);
            if (result == -1) {
                result = (short) (p.item.wear[14].part + 41);
            }
        }

        return result;
    }


    public static short getMaskId(Player p) {
        if (p.fashionSetting.isMask()) return -1;

        short result = -1;
        if (p.item.wear[13] != null) {
            result = ItemManager.getInstance().getFashionPart(p.item.wear[13].id);
            if (result == -1) {
                result = (short) (p.item.wear[13].part + 41);
            }
        }
        return result;
    }

//    public static void send_skill(Player p) throws IOException {
//        Message m = new Message(29);
//        switch (p.clazz) {
//            case 0: { // chien binh
//                m.writer().write(Util.loadfile("data/msg/msg_29_0"));
//                break;
//            }
//            case 1: { // sat thu
//                m.writer().write(Util.loadfile("data/msg/msg_29_1"));
//                break;
//            }
//            case 2: { // phap su
//                m.writer().write(Util.loadfile("data/msg/msg_29_2"));
//                break;
//            }
//            case 3: { // xa thu
//                m.writer().write(Util.loadfile("data/msg/msg_29_3"));
//                break;
//            }
//        }
//        p.conn.addmsg(m);
//        m.cleanup();
//    }


    public static void send_skill(Player p) throws IOException {
        var skills = Manager.gI().getSkillByClazz(p.clazz);
        if (skills == null) {
            return;
        }

        Message m = new Message(29);

            m.writer().writeByte(skills.length);
            for (Skill skill : skills) {
                m.writer().writeByte(skill.id);
                m.writer().writeByte(skill.iconid);
                m.writer().writeUTF(skill.name);
                m.writer().writeByte(skill.type);
                m.writer().writeShort(skill.range);
                m.writer().writeUTF(skill.detail);
                m.writer().writeByte(skill.typeBuff);
                m.writer().writeByte(skill.subEff);
                m.writer().writeByte(skill.mLvSkill.length);
                for (LvSkill lv : skill.mLvSkill) {
                    m.writer().writeShort(lv.mpLost);
                    m.writer().writeShort(lv.LvRe);
                    m.writer().writeInt(lv.delay);
                    m.writer().writeInt(lv.timeBuff);
                    m.writer().writeByte(lv.per_Sub_Eff);
                    m.writer().writeShort(lv.time_Sub_Eff);
                    m.writer().writeShort(lv.plus_Hp);
                    m.writer().writeShort(lv.plus_Mp);

                    m.writer().writeByte(lv.minfo.length);
                    for (Option op : lv.minfo) {
                        m.writer().writeByte(op.getId());
                        m.writer().writeInt(op.getParam());
                    }

                    m.writer().writeByte(lv.nTarget);
                    m.writer().writeShort(lv.range_lan);
                }

                m.writer().writeShort(skill.performDur);
                m.writer().writeByte(skill.typePaint);
                p.conn.addmsg(m);
            }

    }

    public static void send_login_rms(Session conn) throws IOException {
        // id 1
        Message m = new Message(55);
        m.writer().writeByte(1);
        m.writer().writeShort(2);
        m.writer().writeByte(-1);
        m.writer().writeByte(0);
        conn.addmsg(m);
        m.cleanup();
        // id 2
        m = new Message(55);
        m.writer().writeByte(2);
        if (conn.p.map.mapId == 0 && conn.p.level < 2) { // is new begin
            m.writer().writeShort(0);
        } else {
            m.writer().writeShort(1);
            m.writer().writeByte(0);
        }
        conn.addmsg(m);
        m.cleanup();
        //
        if (conn.p.rms_save[0].length > 0) {
            m = new Message(55);
            m.writer().writeByte(0);
            m.writer().writeShort(conn.p.rms_save[0].length);
            m.writer().write(conn.p.rms_save[0]);
            conn.addmsg(m);
            m.cleanup();
        }
        if (conn.p.rms_save[1].length > 0) {
            m = new Message(55);
            m.writer().writeByte(3);
            m.writer().writeShort(conn.p.rms_save[1].length);
            m.writer().write(conn.p.rms_save[1]);
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_notice_nobox_yellow(Session conn, String s) throws IOException {
        Message m = new Message(53);
        m.writer().writeUTF(s);
        m.writer().writeByte(1);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_combo(Session conn) throws IOException {
        Message m = new Message(-108);
        m.writer().writeByte(3);
        m.writer().writeInt(0);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_point_pk(Player p) throws IOException {
        Message m = new Message(59);
        m.writer().writeInt(p.suckhoe);
        m.writer().writeInt(p.pointarena);
        p.conn.addmsg(m);
        m.cleanup();
    }

    public static void send_health(Player p) throws IOException {
        Message m = new Message(59);
        m.writer().writeInt(p.suckhoe);
        m.writer().writeInt(p.pointarena);
        p.conn.addmsg(m);
        m.cleanup();
    }

    public static void sendPlayerWear(Player p) throws IOException {
        Message m = new Message(15);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(p.item.wear.length);
        for (int i = 0; i < p.item.wear.length; i++) {
            Item3 temp = p.item.wear[i];
            if (temp != null) {
                m.writer().writeByte(i);
                m.writer().writeUTF(temp.name);
                m.writer().writeByte(temp.clazz);
                m.writer().writeByte(temp.type);
                m.writer().writeShort(temp.icon);
                if (i == 10 && p.item.wear[14] != null && (p.item.wear[14].id >= 4638 && p.item.wear[14].id <= 4648)) {
                    m.writer().writeByte(p.item.wear[14].part);
                } else {
                    m.writer().writeByte(temp.part);
                }
                m.writer().writeByte(temp.tier); // plus item (tier)
                m.writer().writeShort(temp.level);
                m.writer().writeByte(temp.color);
                m.writer().writeByte(temp.op.size());
                for (int j = 0; j < temp.op.size(); j++) {
                    m.writer().writeByte(temp.op.get(j).id);
                    m.writer().writeInt(temp.op.get(j).getParam(temp.tier));
                }
                m.writer().writeByte(1); // islock
            } else {
                m.writer().writeByte(-1);
            }
        }
        if (p.pet_follow != -1) {
            for (Pet temp : p.mypet) {
                if (temp.is_follow) {
                    m.writer().writeByte(5);
                    m.writer().writeUTF(temp.name != null ? temp.name : "Pet");
                    m.writer().writeByte(4);
                    m.writer().writeShort(temp.level);
                    m.writer().writeShort(temp.getLevelPercent());
                    m.writer().writeByte(temp.type);
                    m.writer().writeByte(temp.spriteImage);
                    m.writer().writeByte(temp.nframe);
                    m.writer().writeByte(temp.color);
                    m.writer().writeInt(temp.get_age());
                    m.writer().writeShort(temp.grown);
                    m.writer().writeShort(temp.maxgrown);
                    m.writer().writeShort(temp.point1);
                    m.writer().writeShort(temp.point2);
                    m.writer().writeShort(temp.point3);
                    m.writer().writeShort(temp.point4);
                    m.writer().writeShort(temp.maxpoint);
                    m.writer().writeByte(temp.op.size());
                    for (int i12 = 0; i12 < temp.op.size(); i12++) {
                        m.writer().writeByte(temp.op.get(i12).id);
                        m.writer().writeInt(temp.op.get(i12).value);
                        m.writer().writeInt(temp.op.get(i12).maxValue);
                    }
                    if (temp.expiry_date <= 0) {
                        m.writer().writeByte(0);
                    } else { // hạn sử dụng
                        m.writer().writeByte(1);
                        m.writer().writeInt(43200);
                        m.writer().writeUTF("" + temp.expiry_date);
                    }
                    break;
                }
            }
        } else {
            m.writer().writeByte(-1); // pet
        }
        m.writer().writeByte(p.fashion.length);
        for (int i = 0; i < p.fashion.length; i++) {
            if (p.conn.version < 280) {
                m.writer().writeByte(p.fashion[i]);
            } else {
                m.writer().writeShort(p.fashion[i]);
            }
        }
        // === PART CHAR EFFECT (data/part_char/x4/img 112_xx) ===
        m.writer().writeShort(p.bodyEffectId); // body effect id
        m.writer().writeShort(p.legEffectId);  // leg effect id
        p.conn.addmsg(m);
        m.cleanup();
    }

    public static void save_rms(Session conn, Message m) throws IOException {
        m.reader().readByte();
        byte id = m.reader().readByte();
        byte[] num = null;
        try {
            // FIX: readShort() returns a signed short; a client-supplied
            // length of 32768+ comes back as a negative number (e.g. -112),
            // which throws NegativeArraySizeException when used directly as
            // an array size. Treat the length as unsigned like elsewhere in
            // the codebase (see Short.toUnsignedInt usage in use_skill).
            int len = Short.toUnsignedInt(m.reader().readShort());
            num = new byte[len];
            for (int i = 0; i < num.length; i++) {
                num[i] = m.reader().readByte();
            }
        } catch (IOException e) {
        }
        if (num != null && num.length > 0) {
            if (id == 0) {
                conn.p.rms_save[0] = new byte[num.length];
                conn.p.rms_save[0] = num;
            } else if (id == 3 && num.length == 11) {
                conn.p.rms_save[1] = new byte[num.length];
                conn.p.rms_save[1] = num;
                conn.p.load_in4_autoplayer(num);
            }
        }
    }

    public static void send_icon(Session conn, Message m) throws IOException {

        short id = m.reader().readShort();
        byte[] data = IconHelper.getIcon(conn.zoomlv, id);

        if (data == null) {
            log.info("No icon found for id: {}", id);
            return;
        }

        try {
            Message m2 = new Message(-51);
            m2.writer().writeShort(id);
            m2.writer().write(data);

            conn.addmsg(m2);
            m2.cleanup();

        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("No icon found for id: {}", id);
    }

    public static void SendEffMob(Session conn, MobInMap mob, int type) throws IOException {
        // System.out.println("core.Service.SendEffMob()"+Manager.gI().msg_eff_70.length);
        byte[] b = null;
        if (type == 70) {
            b = Manager.gI().msg_eff_70;
        } else if (type == 71) {
            b = Manager.gI().msg_eff_71;
        } else {
            return;
        }

        Message m = new Message(-49);
        // m.writer().writeByte(1);
        //
        //
        // m.writer().writeShort(b.length);
        // m.writer().write(b);
        //
        // m.writer().writeByte(50);
        // m.writer().writeByte(50);
        // m.writer().writeByte(type);
        //
        // m.writer().writeShort(mob.x);
        // m.writer().writeShort(mob.y);
        // m.writer().writeByte(3);
        // m.writer().writeByte(2);
        // m.writer().writeShort(mob.index);
        // m.writer().writeShort(8000);
        // m.writer().writeByte(1);

        // m.writer().writeByte(4);
        //
        //
        // m.writer().writeShort(b.length);
        // m.writer().write(b);
        // m.writer().writeShort(type);
        // m.writer().writeByte(1);
        // m.writer().writeShort(mob.index);
        // m.writer().writeByte(1);
        // m.writer().writeByte(0);
        // m.writer().writeShort(b.length);
        // m.writer().write(b);
        // m.writer().writeByte(0);
        // m.writer().writeByte(0);
        // m.writer().writeByte(type);
        // m.writer().writeShort(mob.index);
        // m.writer().writeByte(1);
        // m.writer().writeByte(0);
        // m.writer().writeShort(10000);
        // m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeShort(b.length);
        m.writer().write(b);

        m.writer().writeByte(0);
        m.writer().writeByte(1);
        m.writer().writeByte(type);

        m.writer().writeShort(mob.objectId);
        m.writer().writeByte(1);// tem mob
        m.writer().writeByte(0);
        m.writer().writeShort(8000);
        m.writer().writeByte(0);

        conn.addmsg(m);
        m.cleanup();
    }

    public static int idxDame;

    public static void mob_in4(Player p, int n) throws IOException {
        MobInMap temp = MapService.get_mob_by_index(p.map, n);
        if (temp != null) {
            Message m = new Message(7);
            m.writer().writeShort(n);
            m.writer().writeByte((byte) temp.level);
            m.writer().writeShort(temp.x);
            m.writer().writeShort(temp.y);
            m.writer().writeInt(temp.hp);
            m.writer().writeInt(temp.getMaxHP());
            // m.writer().writeByte(20); // id skill monster (Spec: 32, ...)
            if (temp.template.mob_id >= 89 && temp.template.mob_id <= 92) {
                m.writer().writeByte(temp.template.mob_id - 43); // 46 set
            } else if (temp.template.mob_id == 151) {
                m.writer().writeByte(65);
            } else if (temp.template.mob_id == 152) {
                m.writer().writeByte(66);
            } else if (temp.template.mob_id == 154) {
                m.writer().writeByte(64);
            } else {
                m.writer().writeByte(20);
            }
            // m.writer().writeByte(idxDame);
            m.writer().writeInt(temp.time_refresh);
            m.writer().writeShort(-1); // clan monster
            m.writer().writeByte(0);
            m.writer().writeByte(2); // speed
            m.writer().writeByte(0);
            m.writer().writeUTF("");
            m.writer().writeLong(-11111);
            m.writer().writeByte(temp.color_name); // color name 1: blue, 2: yellow
            p.conn.addmsg(m);
            m.cleanup();
            if (temp.template.mob_id == 151 || temp.template.mob_id == 152) {
                SendEffMob(p.conn, temp, temp.template.mob_id - 81);
            }
        } else if (p.map.zoneId == p.map.maxzone) {
            Pet_di_buon temp2 = Pet_di_buon_manager.check(n);
            if (temp2 != null) {
                Message mm = new Message(7);
                mm.writer().writeShort(n);
                mm.writer().writeByte((byte) 120);
                mm.writer().writeShort(temp2.x);
                mm.writer().writeShort(temp2.y);
                mm.writer().writeInt(temp2.hp);
                mm.writer().writeInt(temp2.getMaxHP());
                mm.writer().writeByte(0);
                mm.writer().writeInt(-1);
                mm.writer().writeShort(-1);
                mm.writer().writeByte(1);
                mm.writer().writeByte(temp2.speed);
                mm.writer().writeByte(0);
                mm.writer().writeUTF(temp2.name);
                mm.writer().writeLong(-11111);
                mm.writer().writeByte(4);
                p.conn.addmsg(mm);
                mm.cleanup();
            }
        } else if (GameMap.isSummonMap(p.map, true)) {
            Crystal temp2 = Manager.gI().mine.get_mob_in_map(p.map);
            if (temp2 != null && temp2.objectId == n) {
                Message mm = new Message(7);
                mm.writer().writeShort(n);
                mm.writer().writeByte((byte) temp2.level);
                mm.writer().writeShort(temp2.x);
                mm.writer().writeShort(temp2.y);
                mm.writer().writeInt(temp2.hp);
                mm.writer().writeInt(temp2.getMaxHP());
                mm.writer().writeByte(0);
                mm.writer().writeInt(4);
                if (temp2.guild != null) {
                    mm.writer().writeShort(temp2.guild.icon);
                    mm.writer().writeInt(Guild.get_id_clan(temp2.guild));
                    mm.writer().writeUTF(temp2.guild.shortName);
                    mm.writer().writeByte(122);
                } else {
                    mm.writer().writeShort(-1);
                }
                mm.writer().writeUTF(temp2.name);
                mm.writer().writeByte(0);
                mm.writer().writeByte(2);
                mm.writer().writeByte(0);
                mm.writer().writeUTF("");
                mm.writer().writeLong(-11111);
                mm.writer().writeByte(4);
                p.conn.addmsg(mm);
                mm.cleanup();
                //
                Eff_player_in_map.add(p, temp2.objectId);
            }
        }
    }

    public static void send_notice_nobox_white(Session conn, String s) throws IOException {
        Message m = new Message(53);
        m.writer().writeUTF(s);
        m.writer().writeByte(0);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_box_input_yesno(Session conn, int type, String s) throws IOException {
        Message m2 = new Message(-32);
        m2.writer().writeShort(conn.p.objectId);
        m2.writer().writeByte(type);
        m2.writer().writeUTF(s);
        conn.addmsg(m2);
        m2.cleanup();
    }

    public static void sendInputDialog(Session s, InputDialog box) {
        Message m = new Message(-31);
        try {
            m.writer().writeShort(box.getNpcId());
            m.writer().writeByte(0);
            m.writer().writeUTF(box.getTitle());
            m.writer().writeByte(box.getFields().size());
            for (String field : box.getFields()) {
                m.writer().writeUTF(field);
                m.writer().writeByte(0);
            }
            for (String ignored : box.getFields()) { //Ignore
                m.writer().writeUTF("");
                m.writer().writeByte(0);
            }
            s.addmsg(m);
        } catch (IOException e) {
            log.error("Unhandled Exception", e);
        }
    }

    public static void sendConfirmDialog(Session s, Dialog dialog) {
        Message m = new Message(-32);
        try {
            m.writer().writeShort(s.p.objectId);
            m.writer().writeByte(dialog.getId());
            m.writer().writeUTF(dialog.getText());
            s.addmsg(m);
        } catch (IOException e) {
            log.error("Unhandled exception", e);
        }

    }


    public static void sendMenu(Session s, Menu menu) {

        Message m = new Message(-30);
        try {
            m.writer().writeShort(menu.getNpc());
            m.writer().writeByte(menu.getId()); // Index
            m.writer().writeByte(menu.getMenus().size());
            for (Menu item : menu.getMenus()) {
                m.writer().writeUTF(item.getName());
            }
            m.writer().writeUTF(menu.getTitle());
            s.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
        }


    }

    public static void usepotion(Player p, int type, long param) throws IOException {
        if (p.isdie) {
            return;
        }
        Message m = new Message(32);
        switch (type) {
            case 0: { // use hp potion

                long par_can_add = 2_000_000_000 - p.hp;
                if (param > par_can_add) {
                    param = par_can_add;
                }

                p.hp += param;
                int maxhp = p.body.getMaxHP();
                if (p.hp > maxhp) {
                    p.hp = maxhp;
                }
                m.writer().writeByte(0);
                m.writer().writeShort(p.objectId);
                m.writer().writeShort(-1); // id potion in bag
                m.writer().writeByte(type);
                m.writer().writeInt(maxhp); // max hp
                m.writer().writeInt(p.hp); // hp
                m.writer().writeInt((int) param); // param use
                break;
            }
            case 1: { // use mp potion
                long par_can_add = 2_000_000_000 - p.mp;
                if (param > par_can_add) {
                    param = par_can_add;
                }
                p.mp += param;
                int maxmp = p.body.getMaxMP();
                if (p.mp > maxmp) {
                    p.mp = maxmp;
                }
                m.writer().writeByte(0);
                m.writer().writeShort(p.objectId);
                m.writer().writeShort(-1); // id potion in bag
                m.writer().writeByte(type);
                m.writer().writeInt(maxmp); // max hp
                m.writer().writeInt(p.mp); // hp
                m.writer().writeInt((int) param); // param use
                break;
            }
        }
        MapService.sendMsgPlayerInside(p.map, p, m, true);
        m.cleanup();
    }

    public static void chat_KTG(Session conn, Message m2) throws IOException {
        if (conn.p.getGem() < 5) {
            send_notice_box(conn, "Tidak cukup permata untuk melakukan");
            return;
        }
        if (conn.p.timeBlockCTG > utils._Time.timeDay) {
            send_notice_box(conn, "Anda telah diblokir dari chat global");
            return;
        }
        // if (!conn.user.equals("ad1") && conn.p.time_chat_ktg >
        // System.currentTimeMillis()) {
        // send_box_notice(conn, "Sau " + (conn.p.time_chat_ktg -
        // System.currentTimeMillis()) / 1000
        // + "s nữa mới có thể tiếp tục chat KTG");
        // return;
        // }
        // conn.p.time_chat_ktg = System.currentTimeMillis() + 1000L * 60 * 5;
        conn.p.updateGem(-5);
        conn.p.item.charInventory(5);
        String text = m2.reader().readUTF();

        if (text != null) {
            String text2 = text.toLowerCase();
            if (text2 != null
                    && (text2.indexOf("reset") >= 0 || text2.indexOf("open") >= 0 || text2.indexOf("open") >= 0)) {
                send_notice_box(conn, "Pesan tidak pantas terdeteksi, akun akan diblokir.");
                return;
            } else {
                Manager.gI().chatKTGprocess("@" + conn.p.name + " : " + text);
            }
        }

    }

    public static void send_view_other_player_in4(Session conn, Message m) throws IOException {
        // FIX: nama target ini datang dari klik nametag pemain lain di client. Kalau target-nya
        // sedang Top 1-3 Level, nametag-nya sudah dapat prefix "#1 - " dst (lihat
        // Player.getDisplayName() & MapService.send_in4_other_char), dan client mengirim balik
        // string itu APA ADANYA. Tanpa strip ini, p01.name.equals(name) di bawah selalu gagal
        // match -> pemain Top 1-3 dikira offline/tersembunyi persis kayak proteksi info GM,
        // padahal mereka online biasa. Lihat catatan lengkap di Player.stripDisplayNamePrefix().
        String name = Player.stripDisplayNamePrefix(m.reader().readUTF());
        byte type = m.reader().readByte();
        if (type == 0) { // xem thong tin other
            Player p0 = null;
            for (GameMap[] gameMap : GameMap.entrys) {
                for (GameMap gameMap0 : gameMap) {
                    for (int i = 0; i < gameMap0.players.size(); i++) {
                        Player p01 = gameMap0.players.get(i);
                        if (p01.name.equals(name)) {

                            p0 = p01;
                            break;
                        }
                    }
                }
            }
            if (p0 != null) {
                if (p0.conn.ac_admin > 1) {
                    send_notice_box(conn, "Pemain sedang offline");
                    return;
                }
                send_notice_nobox_white(p0.conn, conn.p.name + " sedang mengintip perlengkapanmu");
                Message m2 = new Message(49);
                m2.writer().writeShort(p0.objectId);
                m2.writer().writeUTF(name);
                m2.writer().writeByte(p0.clazz);
                m2.writer().writeByte(p0.head);
                m2.writer().writeByte(p0.eye);
                m2.writer().writeByte(p0.hair);
                m2.writer().writeShort(p0.level);
                m2.writer().writeInt(p0.hp);
                m2.writer().writeInt(p0.body.getMaxHP());
                m2.writer().writeByte(p0.typepk);
                m2.writer().writeShort(p0.pointpk);
                m2.writer().writeByte(p0.item.wear.length);
                for (int i = 0; i < p0.item.wear.length; i++) {
                    Item3 temp = p0.item.wear[i];
                    if (temp != null) {
                        m2.writer().writeByte(i);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeByte(temp.clazz);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeShort(temp.icon);
                        m2.writer().writeByte(temp.part); // show part char
                        m2.writer().writeByte(temp.tier); // plus item = tier
                        m2.writer().writeShort(temp.level);
                        m2.writer().writeByte(temp.color);
                        m2.writer().writeByte(temp.op.size());
                        for (int j = 0; j < temp.op.size(); j++) {
                            m2.writer().writeByte(temp.op.get(j).id);
                            m2.writer().writeInt(temp.op.get(j).getParam(temp.tier));
                        }
                        m2.writer().writeByte(0); // can sell
                    } else {
                        m2.writer().writeByte(-1);
                    }
                }
                if (p0.myclan != null) {
                    m2.writer().writeShort(p0.myclan.icon);
                    m2.writer().writeUTF(p0.myclan.shortName);
                    m2.writer().writeByte(p0.myclan.get_mem_type(p0.name));
                    m2.writer().writeUTF(p0.myclan.name);
                } else {
                    m2.writer().writeShort(-1); // clan
                }
                if (p0.pet_follow != -1) {
                    for (Pet temp : p0.mypet) {
                        if (temp.is_follow) {
                            m2.writer().writeByte(5);
                            m2.writer().writeUTF(temp.name != null ? temp.name : "Pet");
                            m2.writer().writeByte(4);
                            m2.writer().writeShort(temp.level);
                            m2.writer().writeShort(temp.getLevelPercent());
                            m2.writer().writeByte(temp.type);
                            m2.writer().writeByte(temp.spriteImage);
                            m2.writer().writeByte(temp.nframe);
                            m2.writer().writeByte(temp.color);
                            m2.writer().writeInt(temp.get_age());
                            m2.writer().writeShort(temp.grown);
                            m2.writer().writeShort(temp.maxgrown);
                            m2.writer().writeShort(temp.point1);
                            m2.writer().writeShort(temp.point2);
                            m2.writer().writeShort(temp.point3);
                            m2.writer().writeShort(temp.point4);
                            m2.writer().writeShort(temp.maxpoint);
                            m2.writer().writeByte(temp.op.size());
                            for (int i12 = 0; i12 < temp.op.size(); i12++) {
                                m2.writer().writeByte(temp.op.get(i12).id);
                                m2.writer().writeInt(temp.op.get(i12).value);
                                m2.writer().writeInt(temp.op.get(i12).maxValue);
                            }
                            // m.writer().writeByte(0);
                            break;
                        }
                    }
                } else {
                    m2.writer().writeByte(-1); // pet
                }
                m2.writer().writeByte(0);
                conn.addmsg(m2);
                m2.cleanup();
            } else {
                send_notice_nobox_white(conn, ("pemain " + name + " sedang offline"));
            }
        }
    }

    public static void send_param_item_wear(Session conn, Message m2) throws IOException {
        @SuppressWarnings("unused")
        byte invenid = m2.reader().readByte();
        byte id = m2.reader().readByte();
        if (id >= conn.p.item.bag3.length) {
            return;
        }
        Item3 temp = conn.p.item.bag3[id];
        if (temp != null) {
            Message m = new Message(21);
            m.writer().writeByte(temp.op.size());
            for (int i = 0; i < temp.op.size(); i++) {
                m.writer().writeByte(temp.op.get(i).id);
                m.writer().writeInt(temp.op.get(i).getParam(temp.tier));
            }
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_box_UI(Session conn, int type) throws IOException {
        Message m = new Message(23);
        switch (type) {
            case 33: {
                if (conn.p.isCreateItemStar) {
                    m.writer().writeUTF("Upgrade Peralatan Bintang");
                } else {
                    m.writer().writeUTF("Upgrade Peralatan Biasa");
                }
                m.writer().writeByte(20);
                m.writer().writeShort(0);
                break;
            }
            case 0: { // cua hang poition
                m.writer().writeUTF("Toko Ramuan");
                m.writer().writeByte(0);
                m.writer().writeShort(Manager.gI().itempoitionsell.length);
                for (int i = 0; i < Manager.gI().itempoitionsell.length; i++) {
                    m.writer().writeShort(Manager.gI().itempoitionsell[i]);
                }
                break;
            }
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16: {// case 22:{
                m.writer().writeUTF("Toko Peralatan");
                m.writer().writeByte(1);
                m.writer().writeShort(Manager.gI().itemsellTB.get(type - 1).length);
                for (int i = 0; i < Manager.gI().itemsellTB.get(type - 1).length; i++) {
                    ItemSell3 temp = Manager.gI().itemsellTB.get(type - 1)[i];
                    m.writer().writeShort(temp.id);
                    m.writer().writeUTF(ItemTemplate3.item.get(temp.id).getName());
                    m.writer().writeByte(temp.clazz);
                    m.writer().writeByte(temp.type);
                    m.writer().writeShort(ItemTemplate3.item.get(temp.id).getIcon());
                    m.writer().writeLong(temp.price);
                    m.writer().writeShort(temp.level);
                    m.writer().writeByte(temp.color);
                    m.writer().writeByte(temp.option.size());
                    for (int j = 0; j < temp.option.size(); j++) {
                        m.writer().writeByte(temp.option.get(j).id);
                        m.writer().writeInt(temp.option.get(j).getParam(0));
                    }
                    m.writer().writeByte(temp.pricetype);
                }
                break;
            }

            case 18: {
                m.writer().writeUTF("Penguatan Peralatan");
                m.writer().writeByte(5);
                m.writer().writeShort(0);
                break;
            }
            case 17: {
                // m.cleanup();
                // send_notice_box(conn, "thử nh"); return;
                m.writer().writeUTF("Batu Material");
                m.writer().writeByte(4);
                m.writer().writeShort(Manager.gI().item7sell.length);
                for (int i = 0; i < Manager.gI().item7sell.length; i++) {
                    m.writer().writeShort(Manager.gI().item7sell[i]);
                }
                break;
            }
            case 51: { // Toko Material Medal V2 — skema persis case 17 (V1), list item beda (template.Medal_Material_V2)
                short[] materialIdsV2 = template.Medal_Material_V2.getAllMaterialIds();
                m.writer().writeUTF("Batu Material V2");
                m.writer().writeByte(4);
                m.writer().writeShort(materialIdsV2.length);
                for (short id : materialIdsV2) {
                    m.writer().writeShort(id);
                }
                break;
            }
            case 19: {
                m.writer().writeUTF("Transformasi Peralatan");
                m.writer().writeByte(9);
                m.writer().writeShort(0);
                break;
            }
            case 20: {
                m.writer().writeUTF("Icon Clan");
                m.writer().writeByte(6);
                m.writer().writeShort(31); // 31 in team server
                for (int i = 0; i < 31; i++) {
                    m.writer().writeShort(i);
                }
                break;
            }
            case 21: {
                m.writer().writeUTF("Hewan Peliharaan");
                m.writer().writeByte(11);
                m.writer().writeShort(0);
                break;
            }
            case 22: {
                m.writer().writeUTF("Makanan Hewan Peliharaan");
                m.writer().writeByte(0);
                m.writer().writeShort(4);
                for (int i = 48; i < 52; i++) {
                    m.writer().writeShort(i);
                }
                break;
            }
            case 23: {
                m.writer().writeUTF("Toko Telur");
                m.writer().writeByte(1);
                short[] id_egg = new short[]{2943, 2944};

                long[] price_egg = new long[]{150, 150};
                m.writer().writeShort(id_egg.length);
                for (int i = 0; i < id_egg.length; i++) {
                    ItemTemplate3 temp = ItemTemplate3.item.get(id_egg[i]);
                    m.writer().writeShort(temp.getId());
                    m.writer().writeUTF(temp.getName());
                    m.writer().writeByte(temp.getClazz());
                    m.writer().writeByte(temp.getType());
                    m.writer().writeShort(temp.getIcon());
                    m.writer().writeLong(price_egg[i]); // 150 ngoc
                    m.writer().writeShort(temp.getLevel());
                    m.writer().writeByte(temp.getColor());
                    m.writer().writeByte(0); // op size
                    m.writer().writeByte(1); // pricetype
                }
                break;
            }
            case 24: {
                m.writer().writeUTF("Gabung Material Biasa");
                m.writer().writeByte(18);
                m.writer().writeShort(0);
                break;
            }
            case 25:
            case 26:
            case 27:
            case 28: 
            case 29: {
                m.writer().writeUTF("Guild Icon");
                m.writer().writeByte(6);
                m.writer().writeShort(358); // 31 in team server
                for (int i = 0; i < 31; i++) {
                    m.writer().writeShort(i);
                }
                for (int i = 500; i < 827; i++) {
                    m.writer().writeShort(i);
                }
                break;
            }
            case 30: { // cua hang shop bang
                m.writer().writeUTF("Guild Shop");
                m.writer().writeByte(8);
                m.writer().writeShort(Guild.item_shop.length);
                for (int i = 0; i < Guild.item_shop.length; i++) {
                    m.writer().writeShort(Guild.item_shop[i]);
                }
                break;
            }
            case 31: {
                m.writer().writeUTF("Toko Permata");
                m.writer().writeByte(1);
                short[] id_case_31 = new short[]{3590, 3591, 3592};
                m.writer().writeShort(id_case_31.length);
                for (int i = 0; i < id_case_31.length; i++) {
                    ItemTemplate3 temp = ItemTemplate3.item.get(id_case_31[i]);
                    m.writer().writeShort(temp.getId());
                    m.writer().writeUTF(temp.getName());
                    m.writer().writeByte(temp.getClazz());
                    m.writer().writeByte(temp.getType());
                    m.writer().writeShort(temp.getIcon());
                    m.writer().writeLong(100_000 + i * 50_000); // price
                    m.writer().writeShort(10); // level
                    m.writer().writeByte(temp.getColor());
                    m.writer().writeByte(0); // option
                    m.writer().writeByte(0); // type money
                }
                break;
            }
            case 32: {
                m.writer().writeUTF("Pet thương nhân");
                m.writer().writeByte(0);
                m.writer().writeShort(1);
                m.writer().writeShort(84);
                break;
            }
            case 39: {
                m.writer().writeUTF("Pet Cướp");
                m.writer().writeByte(0);
                m.writer().writeShort(1);
                m.writer().writeShort(86);
                break;
            }
            case 34: {
                m.writer().writeUTF("Gabung Permata");
                m.writer().writeByte(15);
                m.writer().writeShort(0);
                break;
            }
            case 35: {
                m.writer().writeUTF("Pasang Permata");
                m.writer().writeByte(14);
                m.writer().writeShort(0);
                break;
            }
            case 36: {
                m.writer().writeUTF("Lubangi Equipment");
                m.writer().writeByte(16);
                m.writer().writeShort(0);
                break;
            }
            case 38: {
                m.writer().writeUTF("Pet Shop");
                m.writer().writeByte(1);
                short[] id_egg = Manager.gI().event == 2 ? new short[]{2943, 2944, 4762}
                        : new short[]{2943, 2944};
                long[] price_egg = Manager.gI().event == 2 ? new long[]{150, 150, 500} : new long[]{150, 150};
                m.writer().writeShort(id_egg.length);
                for (int i = 0; i < id_egg.length; i++) {
                    ItemTemplate3 temp = ItemTemplate3.item.get(id_egg[i]);
                    m.writer().writeShort(temp.getId());
                    m.writer().writeUTF(temp.getName());
                    m.writer().writeByte(temp.getClazz());
                    m.writer().writeByte(temp.getType());
                    m.writer().writeShort(temp.getIcon());
                    m.writer().writeLong(price_egg[i]); // 150 ngoc
                    m.writer().writeShort(temp.getLevel());
                    m.writer().writeByte(temp.getColor());
                    m.writer().writeByte(0); // op size
                    m.writer().writeByte(1); // pricetype
                }
                break;
            }
            case 37: {
                TokenShop.sendItemShop(conn, "Fashion Shop");
                break;
            }
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47: {
                m.writer().writeUTF("Buat Peralatan Bintang");
                m.writer().writeByte(19);
                m.writer().writeShort(0);
                m.writer().writeByte(5);
                for (int i = conn.p.TypeItemStarCreate * 5; i < conn.p.TypeItemStarCreate * 5 + 5; i++) {
                    m.writer().writeShort(conn.p.MaterialItemStar[i]);
                    m.writer().writeByte(1);
                }
                break;
            }
            case 48: {
                TokenShop.sendItemShop(conn, "Starter Pack");
                break;
            }

            case 50: { // Toko Misteri Box (item4), daftar id diatur lewat tabel `itemsell` id=19
                m.writer().writeUTF("Misteri Box");
                m.writer().writeByte(0);
                m.writer().writeShort(Manager.gI().itemMisteriBoxSell.length);
                for (int i = 0; i < Manager.gI().itemMisteriBoxSell.length; i++) {
                    m.writer().writeShort(Manager.gI().itemMisteriBoxSell[i]);
                }
                break;
            }
            case 49: { // Medal MBG (index 4)
                // Expand array jika player lama masih punya panjang 20
                if (conn.p.medal_create_material == null || conn.p.medal_create_material.length < 25) {
                    short[] newArr = new short[25];
                    if (conn.p.medal_create_material != null) {
                        System.arraycopy(conn.p.medal_create_material, 0, newArr, 0, Math.min(conn.p.medal_create_material.length, 25));
                    }
                    conn.p.medal_create_material = newArr;
                }
                GameSrc.initMedalMaterial(conn.p, 4, (byte) 0);
                if (conn.version > 250) {
                    m.writer().writeUTF("Medal Crafting");
                    m.writer().writeByte(19);
                    m.writer().writeShort(0);
                    m.writer().writeByte(5);
                    m.writer().writeShort(conn.p.medal_create_material[20]);
                    m.writer().writeShort(1);
                    m.writer().writeShort(conn.p.medal_create_material[21]);
                    m.writer().writeShort(1);
                    m.writer().writeShort(conn.p.medal_create_material[22]);
                    m.writer().writeShort(1);
                    m.writer().writeShort(conn.p.medal_create_material[23]);
                    m.writer().writeShort(1);
                    m.writer().writeShort(conn.p.medal_create_material[24]);
                    m.writer().writeShort(1);
                } else {
                    m.writer().writeUTF("Buat Peralatan Biasa");
                    m.writer().writeByte(19);
                    m.writer().writeShort(0);
                    m.writer().writeByte(5);
                    m.writer().writeShort(conn.p.medal_create_material[20]);
                    m.writer().writeByte(1);
                    m.writer().writeShort(conn.p.medal_create_material[21]);
                    m.writer().writeByte(1);
                    m.writer().writeShort(conn.p.medal_create_material[22]);
                    m.writer().writeByte(1);
                    m.writer().writeShort(conn.p.medal_create_material[23]);
                    m.writer().writeByte(1);
                    m.writer().writeShort(conn.p.medal_create_material[24]);
                    m.writer().writeByte(1);
                }
                break;
            }

            default: {
                AdminMenuController.handleBoxUI(conn, type, m);
                break;
            }
        }
        conn.addmsg(m);
        m.cleanup();
    }

    public static void revenge(Session conn, byte index) throws IOException {
        if (conn.p.getGem() < 2) {
            send_notice_box(conn, "2 permata saja tidak punya, mau balas dendam apa???");
            return;
        }
        String name = conn.p.list_enemies.get(conn.p.list_enemies.size() - index - 1);
        Player p0 = null;
        for (GameMap[] gameMap : GameMap.entrys) {
            for (GameMap gameMap0 : gameMap) {
                for (int i = 0; i < gameMap0.players.size(); i++) {
                    Player p2 = gameMap0.players.get(i);
                    if (p2.name.equals(name)) {
                        p0 = p2;
                        break;
                    }
                }
                if (p0 != null) {
                    break;
                }
            }
            if (p0 != null) {
                break;
            }
        }
        if (p0 == null) {
            send_notice_box(conn, "Kẻ thù đang offline");
        } else {
            EffTemplate ef = p0.getEffectDefault(-125);
            if (ef == null && p0.map.mapId != 0 && !p0.map.ismaplang) {
                conn.p.updateGem(-2);
                conn.p.item.charInventory(5);
                conn.p.isChangemap = false;
                GameMap mbuffer2 = p0.map;
                if (mbuffer2 != null) {
                    if (conn.p.isdie) {
                        return;
                    }
                    MapService.leave(conn.p.map, conn.p);
                    conn.p.map = mbuffer2;
                    conn.p.x = p0.x;
                    conn.p.y = p0.y;
                    MapService.enter(conn.p.map, conn.p);
                    Message m = new Message(4);
                    m.writer().writeByte(0);
                    m.writer().writeShort(0);
                    m.writer().writeShort(p0.objectId);
                    m.writer().writeShort(p0.x);
                    m.writer().writeShort(p0.y);
                    m.writer().writeByte(-1);
                    conn.addmsg(m);
                    m.cleanup();
                    //
                    m = new Message(4);
                    m.writer().writeByte(0);
                    m.writer().writeShort(0);
                    m.writer().writeShort(conn.p.objectId);
                    m.writer().writeShort(conn.p.x);
                    m.writer().writeShort(conn.p.y);
                    m.writer().writeByte(-1);
                    p0.conn.addmsg(m);
                    m.cleanup();
                } else {
                    send_notice_box(conn, "Terjadi error saat pindah map");
                }
            } else {
                send_notice_box(conn, "Kẻ thù đang trong khu vực không thể pk");
            }
        }
    }

    public static void sendBoxInputText(Session conn, int type, String text, String[] in4) throws IOException {
        Message m = new Message(-31);
        m.writer().writeShort(type);
        m.writer().writeByte(0);
        m.writer().writeUTF(text);
        m.writer().writeByte(in4.length);
        for (int i = 0; i < in4.length; i++) {
            m.writer().writeUTF(in4[i]);
            m.writer().writeByte(0);
        }
        for (int i = 0; i < in4.length; i++) {
            m.writer().writeUTF("");
            m.writer().writeByte(0);
        }
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_in4_item(Session conn, Message m) throws IOException {
        short id = m.reader().readShort();
        // for (int i = 0; i < conn.p.item.bag3.length; i++) {
        // Item3 temp = conn.p.item.bag3[i];
        // if (temp != null && temp.id == 9) {
        Message m2 = new Message(28);
        m2.writer().writeShort(id); // index?
        m2.writer().writeUTF("Vật phẩm hiển thị lỗi, hãy thoát game vào lại để reset");
        m2.writer().writeByte(8); // type item
        m2.writer().writeByte(0); // id part
        m2.writer().writeByte(0); // class item
        m2.writer().writeShort(0); // icon id
        m2.writer().writeByte(1);// size
        for (int i2 = 0; i2 < 1; i2++) {
            m2.writer().writeByte(69);
            m2.writer().writeInt(99);
        }
        conn.addmsg(m2);
        m2.cleanup();
        // }
        // }
    }

    public static void send_item7_template(Player p, Message m2) throws IOException {
        short id = m2.reader().readShort();
        if (id < 0 || id >= ItemTemplate7.item.size()) {
            return;
        }
        ItemTemplate7 it7 = ItemTemplate7.item.get(id);
        Message m = new Message(-106);
        m.writer().writeShort(it7.getId());
        m.writer().writeShort(it7.getIcon());
        m.writer().writeLong(it7.getPrice());
        m.writer().writeUTF(it7.getName());
        m.writer().writeUTF(it7.getContent());
        m.writer().writeByte(it7.getType());
        m.writer().writeByte(it7.getPricetype());
        m.writer().writeByte(it7.getSell());
        m.writer().writeShort(it7.getValue());
        m.writer().writeByte(it7.getTrade());
        m.writer().writeByte(it7.getColor());
        p.conn.addmsg(m);
        m.cleanup();
    }

    public static void chatClan(Guild guild, String text) throws IOException {
        Message m = new Message(34);
        m.writer().writeUTF("Guild");
        m.writer().writeUTF("@System : " + text);
        for (int i = 0; i < guild.members.size(); i++) {
            String name2 = guild.members.get(i).name;
            for (GameMap[] gameMaps : GameMap.entrys) {
                for (GameMap gameMap : gameMaps) {
                    synchronized (gameMap) {
                        for (Player p0 : gameMap.players) {
                            if (p0.name.equals(name2)) {
                                p0.conn.addmsg(m);
                            }
                        }
                    }
                }
            }
        }
        m.cleanup();
    }

    public static void chatTab(Session conn, Message m2) throws IOException {
        String name = m2.reader().readUTF();
        String chat = m2.reader().readUTF();
        // FIX: kalau ini whisper tab (bukan "Team"/"Guild"), `name` adalah nama TARGET yang
        // dikirim balik oleh client persis seperti nametag yang dia lihat di layar. Untuk pemain
        // Top 1-3 Level nametag-nya bawa prefix "#1 - " dst (lihat Player.getDisplayName()), jadi
        // tanpa strip ini, name.equals(...) di loop paling bawah selalu gagal match -> whisper ke
        // pemain Top 1-3 selalu bilang "Pemain sedang offline!" walau dia online. Lihat catatan
        // lengkap di Player.stripDisplayNamePrefix(). Aman: no-op utk "Team"/"Guild"/nama biasa.
        if (!name.equals("Team") && !name.equals("Guild")) {
            name = Player.stripDisplayNamePrefix(name);
        }

        if (name.equals("Team") && conn.p.party != null && conn.p.party.get_mems().contains(conn.p)) {
            Message m = new Message(34);
            m.writer().writeUTF("Team");
            m.writer().writeUTF(conn.p.name + " : " + chat);
            for (int i = 0; i < conn.p.party.get_mems().size(); i++) {
                if (conn.p.party.get_mems().get(i).objectId != conn.p.objectId) {
                    conn.p.party.get_mems().get(i).conn.addmsg(m);
                }
            }

            m.cleanup();
        } else if (name.equals("Guild") && conn.p.myclan != null) {
            Message m = new Message(34);
            m.writer().writeUTF("Guild");
            m.writer().writeUTF(conn.p.name + " : " + chat);

            for (int i = 0; i < conn.p.myclan.members.size(); i++) {
                String name2 = conn.p.myclan.members.get(i).name;
                if (!name2.equals(conn.p.name)) {
                    for (GameMap[] gameMap : GameMap.entrys) {
                        for (GameMap gameMap2 : gameMap) {
                            synchronized (gameMap2) {
                                for (Player p0 : gameMap2.players) {
                                    if (p0.name.equals(name2)) {
                                        p0.conn.addmsg(m);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            m.cleanup();
        } else {
            Player p0 = null;
            for (GameMap[] gameMap : GameMap.entrys) {
                for (GameMap gameMap0 : gameMap) {
                    for (int i = 0; i < gameMap0.players.size(); i++) {
                        if (gameMap0.players.get(i).name.equals(name)) {
                            p0 = gameMap0.players.get(i);
                        }
                    }
                }
            }
            if (p0 == null) {
                send_notice_box(conn, "Pemain sedang offline!");
            } else {
                if (p0.name.equals(conn.p.name)) {
                    send_notice_box(conn, "Are you crazy or what, talking to yourself like that?");
                    return;
                }
                Message m = new Message(34);
                m.writer().writeUTF(conn.p.name);
                m.writer().writeUTF(chat);
                p0.conn.addmsg(m);
                m.cleanup();
            }
        }
    }

    public static void sendChatTab(Session conn, String from, String chat) throws IOException {
        Message m = new Message(34);
        m.writer().writeUTF(from);
        m.writer().writeUTF(chat);
        conn.addmsg(m);
        m.cleanup();
    }

    public static void handlePetEat(Session conn, Message m2) throws IOException {
        short petId = m2.reader().readShort();
        short itemId = m2.reader().readShort();
        byte category = m2.reader().readByte();
        byte type = m2.reader().readByte();


        if (category != 3 && category != 4) {
            send_notice_box(conn, "Aku nak permen atau equipment");
            return;
        }


        if ((category == 4 && itemId != 48 && itemId != 49 && itemId != 50 && itemId != 51)) {
            send_notice_box(conn, "Aku nak permen atau equipment");
            return;
        }


        if (category == 4 && conn.p.item.getPotion(itemId) == null) {
            return;
        }

        // FIX: itemId comes straight from the client packet and was used to
        // index conn.p.item.bag3[] with no upper-bound check, mirroring the
        // same class of bug as sell_item()/Item.remove(). An out-of-range
        // itemId (or negative) threw ArrayIndexOutOfBoundsException.
        if (category == 3 && (itemId < 0 || itemId >= conn.p.item.bag3.length || conn.p.item.bag3[itemId] == null)) {
            return;
        }

        int index_pet = petId;
        if (type == 1) {
            for (int i = 0; i < conn.p.mypet.size(); i++) {
                if (conn.p.mypet.get(i).is_follow) {
                    index_pet = i;
                    break;
                }
            }
        }

        // FIX (root cause of the recurring "Index N out of bounds for
        // length 0" crash, repeatedly triggered from the same IP —
        // consistent with a modified client/exploit attempt): petId is
        // read straight from the client packet and used as a List index
        // without an upper-bound check. If the player has no/fewer pets
        // than the supplied index, this threw IndexOutOfBoundsException.
        if (index_pet < 0 || index_pet >= conn.p.mypet.size()) {
            return;
        }

        Pet _ppp = conn.p.mypet.get(index_pet);
        if (_ppp.grown < _ppp.maxgrown) {
            _ppp.grown += 5;
        }
        if ((_ppp.level == 9 || _ppp.level == 19 || _ppp.level == 29)
                && Math.abs(Level.entrys.get(_ppp.level - 1).exp - _ppp.exp) < 10) {
            send_notice_box(conn, "You need to use a growth potion");
            return;
        }

        // Pet level 30+ hanya menambah grown (feed point) agar pet mau menyerang,
        // tidak perlu menambah point atau exp karena sudah mencapai level maksimal
        boolean isMaxLevel = _ppp.level >= 30;

        if (category == 4) {
            if (isMaxLevel) {
                // Level 30+: item dikonsumsi untuk grown saja, tidak tambah point/exp
                send_notice_box(conn, "+5 Feed Point (Pet sudah level maksimal)");
            } else if (itemId == 51 && conn.p.mypet.get(index_pet).point1 < _ppp.maxpoint) {
                conn.p.mypet.get(index_pet).point1 += 10;
                send_notice_box(conn, "+10 points to Strength group");
            } else if (itemId == 49 && conn.p.mypet.get(index_pet).point4 < _ppp.maxpoint) {
                conn.p.mypet.get(index_pet).point4 += 10;
                send_notice_box(conn, "+10 points to Spirit group");
            } else if (itemId == 50 && conn.p.mypet.get(index_pet).point3 < _ppp.maxpoint) {
                conn.p.mypet.get(index_pet).point3 += 10;
                send_notice_box(conn, "+10 points to Stamina group");
            } else if (itemId == 48 && conn.p.mypet.get(index_pet).point2 < _ppp.maxpoint) {
                conn.p.mypet.get(index_pet).point2 += 10;
                send_notice_box(conn, "+10 points to Agility group");
            } else {
                send_notice_box(conn, "Cannot feed this equipment type");
                return;
            }

            if (!isMaxLevel) {
                conn.p.mypet.get(index_pet).update_exp(3250);
            }
            conn.p.item.remove(4, itemId, 1);
            conn.p.item.charInventory(4);
        } else {
            if (conn.p.item.bag3[itemId] != null) {
                int type_ = conn.p.item.bag3[itemId].type;
                if (isMaxLevel) {
                    // Level 30+: item equipment dikonsumsi untuk grown saja
                    send_notice_box(conn, "+5 Feed Point (Pet sudah level maksimal)");
                } else if ((type_ == 8 || type_ == 9) && _ppp.point1 < _ppp.maxpoint) {
                    conn.p.mypet.get(index_pet).point1 += 10;
                    send_notice_box(conn, "+10 points to Strength group");
                } else if ((type_ == 10 || type_ == 11) && _ppp.point4 < _ppp.maxpoint) {
                    conn.p.mypet.get(index_pet).point4 += 10;
                    send_notice_box(conn, "+10 points to Spirit group");
                } else if ((type_ == 0 || type_ == 1 || type_ == 2 || type_ == 3 || type_ == 6)
                        && _ppp.point3 < _ppp.maxpoint) {
                    conn.p.mypet.get(index_pet).point3 += 10;
                    send_notice_box(conn, "+10 points to Stamina group");
                } else if ((type_ == 4 || type_ == 5) && _ppp.point2 < _ppp.maxpoint) {
                    conn.p.mypet.get(index_pet).point2 += 10;
                    send_notice_box(conn, "+10 points to Agility group");
                } else {
                    send_notice_box(conn, "Cannot feed");
                    return;
                }

                His_DelItem hist = new His_DelItem(conn.p.name);
                hist.Logger = "cho pet ăn";
                hist.tem3 = conn.p.item.bag3[itemId];
                hist.Flus();
                if (!isMaxLevel) {
                    conn.p.mypet.get(index_pet).update_exp(3250);
                }
                conn.p.item.bag3[itemId] = null;
                conn.p.item.charInventory(3);
            } else {
                send_notice_box(conn, "Terjadi kesalahan");
                return;
            }
        }

        if (type == 1) {
            sendPlayerWear(conn.p);
            sendMainCharInfo(conn.p);
        } else if (type == 0) {
            Message m = new Message(44);
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(2);
            m.writer().writeByte(9);
            m.writer().writeByte(9);
            m.writer().writeShort(index_pet);
            conn.addmsg(m);
            m.cleanup();
            //
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(1);
            m.writer().writeByte(9);
            m.writer().writeByte(9);
            m.writer().writeUTF(conn.p.mypet.get(index_pet).name);
            m.writer().writeByte(conn.p.mypet.get(index_pet).type);
            m.writer().writeShort(index_pet); // id
            m.writer().writeShort(conn.p.mypet.get(index_pet).level);
            m.writer().writeShort(conn.p.mypet.get(index_pet).getLevelPercent()); // exp
            m.writer().writeByte(conn.p.mypet.get(index_pet).type);
            m.writer().writeByte(conn.p.mypet.get(index_pet).spriteImage);
            m.writer().writeByte(conn.p.mypet.get(index_pet).nframe);
            m.writer().writeByte(conn.p.mypet.get(index_pet).color);
            m.writer().writeInt(conn.p.mypet.get(index_pet).get_age());
            m.writer().writeShort(conn.p.mypet.get(index_pet).grown);
            m.writer().writeShort(conn.p.mypet.get(index_pet).maxgrown);
            m.writer().writeShort(conn.p.mypet.get(index_pet).point1);
            m.writer().writeShort(conn.p.mypet.get(index_pet).point2);
            m.writer().writeShort(conn.p.mypet.get(index_pet).point3);
            m.writer().writeShort(conn.p.mypet.get(index_pet).point4);
            m.writer().writeShort(conn.p.mypet.get(index_pet).maxpoint);
            m.writer().writeByte(conn.p.mypet.get(index_pet).op.size());
            for (int i2 = 0; i2 < conn.p.mypet.get(index_pet).op.size(); i2++) {
                PetOption temp2 = conn.p.mypet.get(index_pet).op.get(i2);
                m.writer().writeByte(temp2.id);
                m.writer().writeInt(temp2.value);
                m.writer().writeInt(temp2.maxValue);
            }
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public static void pet_process(Session conn, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();

        if (type == 1) {
            boolean duplicated = false;
            for (Pet temp : conn.p.mypet) {
                if (temp.time_born > System.currentTimeMillis()) {
                    duplicated = true;
                    break;
                }
            }
            if (duplicated) {
                send_notice_box(conn, "Di dalam kandang sudah ada telur yang sedang dierami, silakan coba lagi nanti");
                return;
            }
            Item3 it = conn.p.item.bag3[id];
            if (it != null) {
                Pet temp = Pet.getPet(it.id, it.expiry_date);
                if (temp == null) {
                    send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi nanti");
                    return;
                }
                conn.p.mypet.add(temp);
                //
                Message m = new Message(44);
                m.writer().writeByte(28);
                m.writer().writeByte(1);
                m.writer().writeByte(3);
                m.writer().writeByte(3);
                m.writer().writeUTF(it.name);
                m.writer().writeByte(it.clazz);
                m.writer().writeShort(it.id);
                m.writer().writeByte(it.type);
                m.writer().writeShort(it.icon);
                m.writer().writeByte(it.tier);
                m.writer().writeShort(it.level);
                m.writer().writeByte(it.color);
                it.islock = false;
                m.writer().writeByte(0);
                m.writer().writeByte(0);
                m.writer().writeByte(0); // op size
                m.writer().writeInt((int) ((temp.time_born - System.currentTimeMillis()) / 60000));
                m.writer().writeByte(0);
                conn.addmsg(m);
                m.cleanup();
                conn.p.item.bag3[id] = null;
                conn.p.item.charInventory(3);
            }
        } else if (type == 0) {
            Message m = null;
            if (conn.p.pet_follow != -1) {
                for (Pet temp : conn.p.mypet) {
                    if (temp.is_follow) {
                        temp.is_follow = false;
                        m = new Message(44);
                        m.writer().writeByte(28);
                        m.writer().writeByte(1);
                        m.writer().writeByte(9);
                        m.writer().writeByte(9);
                        m.writer().writeUTF(temp.name != null ? temp.name : "Pet");
                        m.writer().writeByte(temp.type);
                        m.writer().writeShort(conn.p.mypet.indexOf(temp)); // id
                        m.writer().writeShort(temp.level);
                        m.writer().writeShort(temp.getLevelPercent()); // exp
                        m.writer().writeByte(temp.type);
                        m.writer().writeByte(temp.spriteImage);
                        m.writer().writeByte(temp.nframe);
                        m.writer().writeByte(temp.color);
                        m.writer().writeInt(temp.get_age());
                        m.writer().writeShort(temp.grown);
                        m.writer().writeShort(temp.maxgrown);
                        m.writer().writeShort(temp.point1);
                        m.writer().writeShort(temp.point2);
                        m.writer().writeShort(temp.point3);
                        m.writer().writeShort(temp.point4);
                        m.writer().writeShort(temp.maxpoint);
                        m.writer().writeByte(temp.op.size());
                        for (int i2 = 0; i2 < temp.op.size(); i2++) {
                            PetOption temp2 = temp.op.get(i2);
                            m.writer().writeByte(temp2.id);
                            m.writer().writeInt(temp2.value);
                            m.writer().writeInt(temp2.maxValue);
                        }
                        conn.p.conn.addmsg(m);
                        m.cleanup();
                        break;
                    }
                }
            }
            if (id < 0 || id >= conn.p.mypet.size()) return;
            conn.p.mypet.get(id).is_follow = true;
            conn.p.pet_follow = conn.p.mypet.get(id).getId();
            m = new Message(44);
            m.writer().writeByte(28);
            m.writer().writeByte(2);
            m.writer().writeByte(9);
            m.writer().writeByte(9);
            m.writer().writeShort(id);
            conn.addmsg(m);
            m.cleanup();
            //
            Service.sendPlayerWear(conn.p);
            Service.sendMainCharInfo(conn.p);
        }
    }

    public static void sell_item(Session conn, Message m2) throws IOException {
        if (conn.p.isdie) {
            return;
        }
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte typedel = m2.reader().readByte();
        if (id < 0) return;
        // FIX: id comes from the client and, for type 3, is used as a raw
        // index into the fixed-size Item3[] bag3 array (see Item.remove()).
        // GameMap.drop_item() already guards this with an upper-bound check;
        // sell_item()'s "sell in shop" path was missing the same check,
        // causing ArrayIndexOutOfBoundsException (e.g. "Index 512 out of
        // bounds for length 126") when a client sent an out-of-range id.
        if (type == 3 && id >= conn.p.item.bag3.length) return;
        switch (typedel) {
            case 0: { // drop 2 map
                conn.p.map.drop_item(conn.p, type, id);
                break;
            }
            case 1: { // sell in shop
                switch (type) {
                    case 3:
                    case 4:
                    case 7: {
                        int quant = conn.p.item.total_item_by_id(type, id);
                        conn.p.updateGold(quant * 10000); // 10k per item
                        conn.p.item.remove(type, id, quant);
                        conn.p.item.charInventory(type);
                        // conn.p.item.char_inventory(4);
                        // conn.p.item.char_inventory(7);
                        // conn.p.item.char_inventory(3);
                        break;
                    }
                    default: {
                        Service.send_notice_box(conn, "Chưa hỗ trợ bán vật phẩm này");
                        break;
                    }
                }
                break;
            }
        }
    }

    public static void buy_item(Player p, Message m) throws IOException {
        byte type = m.reader().readByte();
        short idbuy = m.reader().readShort();
        int quanity = Short.toUnsignedInt(m.reader().readShort());

        if (idbuy < 0 || quanity <= 0 || quanity > 32000) {
            return;
        }
        if (p.item.get_bag_able() < 1) {
            send_notice_nobox_white(p.conn, "Tas penuh!!");
            return;
        }
        log.info("Buy Item type= {} id= {} qty= {}", type, idbuy, quanity);

        if (p.getCurrentShop() != null) {
            ShopManager.getInstance().doPurchase(p, idbuy, quanity);
            return;
        }

        switch (type) {
            case 0: { // cua hang potion
                if (idbuy > (ItemTemplate4.item.size() - 1)) {
                    return;
                }
                long price = ItemTemplate4.item.get(idbuy).getPrice() * quanity;
                if (ItemTemplate4.item.get(idbuy).getPricetype() == 0) {
                    if (p.getGold() < price) {
                        send_notice_box(p.conn, "Tidak cukup " + price + " emas");
                        return;
                    }
                    p.updateGold(-price);
                } else {
                    if (p.getGem() < price) {
                        send_notice_box(p.conn, "Permata tidak cukup: " + price + " permata");
                        return;
                    }
                    p.updateGem(-price);
                }
                int quant_add_bag = quanity + p.item.total_item_by_id(4, idbuy);
                if (quant_add_bag > 32000) {
                    send_notice_box(p.conn, "Tidak bisa membeli lagi");
                    return;
                }
                Item47 itbag = new Item47();
                itbag.id = idbuy;
                itbag.quantity = (short) quanity;
                itbag.category = 4;
                p.item.add_item_bag47(4, itbag);
                p.item.charInventory(4);
                p.item.charInventory(7);
                p.item.charInventory(3);
                break;
            }

            case 1: {

                if (idbuy > (ItemTemplate3.item.size() - 1)) {
                    return;
                }

                if (p.map.mapId == 3) {
                    TokenShop.buyItemToken(p, idbuy);
                    return;
                }

                if (TokenShop.checkItem("Fashion Shop", idbuy)) {
                    TokenShop.buyItemFashion(p, idbuy);
                    return;
                }

                if (TokenShop.checkItem("Starter Pack", idbuy)) {
                    TokenShop.buyStarterPack(p, idbuy);
                    return;
                }

                if (utils.CheckItem.isBuyItemCoin(idbuy))// mua bằng coin
                {
                    for (Itemsellcoin itsell3 : Itemsellcoin.entry) {
                        if (itsell3.id == idbuy) {
                            if (p.getGem() < itsell3.price) {
                                send_notice_box(p.conn, "Kamu tidak cukup gem untuk membeli!");
                                return;
                            }
                            p.updateGem(-itsell3.price);

                            Item3 itbag = new Item3();
                            itbag.id = idbuy;
                            itbag.clazz = ItemTemplate3.item.get(idbuy).getClazz();
                            itbag.type = ItemTemplate3.item.get(idbuy).getType();
                            itbag.level = 60;
                            itbag.icon = ItemTemplate3.item.get(idbuy).getIcon();
                            itbag.color = itsell3.color;
                            itbag.part = ItemTemplate3.item.get(idbuy).getPart();
                            itbag.islock = true;
                            itbag.name = ItemTemplate3.item.get(idbuy).getName();
                            itbag.tier = 0;
                            itbag.op = new ArrayList<>();
                            itbag.op.addAll(itsell3.op);
                            itbag.time_use = 0;
                            long expireDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(21 * 30);
                            itbag.expiry_date = itbag.type != 14 ? expireDate : 0;
                            p.item.add_item_bag3(itbag);
                            p.item.charInventory(3);
                            send_notice_box(p.conn, "Berhasil membeli peralatan " + itbag.name);
                            return;
                        }
                    }

                    for (Item3 it : Itemsellcoin.bintang) {

                        if (idbuy == it.id) {
                            if (p.getGold() < 1000_000_000) {
                                send_notice_box(p.conn, "Kamu tidak cukup emas untuk membeli!");
                                return;
                            }
                            p.updateGold(-1000_000_000);

                            p.item.add_item_bag3(it);
                            p.item.charInventory(3);
                            send_notice_box(p.conn, "Berhasil membeli peralatan " + it.name);
                            return;
                        }
                    }

                    send_notice_box(p.conn, "Item tidak ditemukan!");
                    return;
                }
                if (ev_he.Event_2.isBuyItemSK(p.conn, 3, idbuy, 1)) {
                    return;
                }
                if (idbuy == 2943 || idbuy == 2944 || (idbuy == 4762 && Manager.gI().event == 2)) {
                    if ((p.getGem() < 500 && idbuy == 4762) || (p.getGem() < 150 && idbuy != 4762)) {
                        send_notice_box(p.conn, "Tidak cukup permata");
                        return;
                    }
                    if (idbuy == 4762) {
                        p.updateGem(-500);
                    } else {
                        p.updateGem(-150);
                    }
                    //
                    Item3 itbag = new Item3();
                    itbag.id = idbuy;
                    itbag.clazz = ItemTemplate3.item.get(idbuy).getClazz();
                    itbag.type = ItemTemplate3.item.get(idbuy).getType();
                    itbag.level = ItemTemplate3.item.get(idbuy).getLevel();
                    itbag.icon = ItemTemplate3.item.get(idbuy).getIcon();
                    itbag.color = ItemTemplate3.item.get(idbuy).getColor();
                    itbag.part = ItemTemplate3.item.get(idbuy).getPart();
                    itbag.islock = true;
                    itbag.name = ItemTemplate3.item.get(idbuy).getName();
                    itbag.tier = 0;
                    itbag.op = new ArrayList<>();
                    itbag.time_use = 0;
                    p.item.add_item_bag3(itbag);
                } else if (idbuy == 3590 || idbuy == 3591 || idbuy == 3592) {
                    if (p.map.mapId != 52) {
                        return;
                    }
                    if (p.pet_di_buon == null) {
                        send_notice_box(p.conn, "Kamu belum membawa hewan dagang!");
                        return;
                    }
                    int vang_quant = (idbuy - 3590) * 100_000 + 1_000_000;
                    if (p.getGold() < vang_quant) {
                        send_notice_box(p.conn, "Tidak cukup " + vang_quant + " emas");
                        return;
                    }
                    if (p.pet_di_buon.item.size() >= 12) {
                        send_notice_box(p.conn, "Terlalu berat!!!");
                        return;
                    }
                    p.updateGold(-vang_quant);
                    //
                    p.pet_di_buon.item.add(idbuy);

                } else {
                    long price = 0;
                    ItemSell3 buffer = null;

                    // Search in all regular shops first
                    outerLoop:
                    for (ItemSell3[] itsell_3 : Manager.gI().itemsellTB) {
                        for (ItemSell3 itsell3 : itsell_3) {
                            if (itsell3.id == idbuy) {
                                buffer = itsell3;
                                price = quanity * itsell3.price;
                                break outerLoop;
                            }
                        }
                    }

                    // If not found in regular shops, try coin shop
                    if (price == 0) {
                        Itemsellcoin itemsellcoin = null;
                        for (int i = 0; i < Itemsellcoin.entry.size(); i++) {
                            if (Itemsellcoin.entry.get(i).id == idbuy) {
                                itemsellcoin = Itemsellcoin.entry.get(i);
                                break;
                            }
                        }
                        if (itemsellcoin != null) {
                            if (p.getGem() < itemsellcoin.price) {
                                send_notice_box(p.conn, "Tidak cukup " + itemsellcoin.price + " gem");
                                return;
                            }
                            p.updateGem(-itemsellcoin.price);
                            Item3 itbag = new Item3();
                            itbag.id = idbuy;
                            itbag.clazz = ItemTemplate3.item.get(idbuy).getClazz();
                            itbag.type = ItemTemplate3.item.get(idbuy).getType();
                            itbag.level = 60;
                            itbag.icon = ItemTemplate3.item.get(idbuy).getIcon();
                            itbag.color = itemsellcoin.color;
                            itbag.part = ItemTemplate3.item.get(idbuy).getPart();
                            itbag.islock = true;
                            itbag.name = ItemTemplate3.item.get(idbuy).getName();
                            itbag.tier = 0;
                            long expireDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(21 * 30);
                            itbag.expiry_date = itbag.type != 14 ? expireDate : 0;
                            itbag.op = new ArrayList<>();
                            for (int i = 0; i < itemsellcoin.op.size(); i++) {
                                itbag.op.add(new Option(itemsellcoin.op.get(i).id,
                                        itemsellcoin.op.get(i).getParam(0)));
                            }
                            itbag.time_use = 0;
                            p.item.add_item_bag3(itbag);
                            p.item.charInventory(3);
                            send_notice_box(p.conn,
                                    "Berhasil membeli " + ItemTemplate3.item.get(idbuy).getName());

                        } else {
                            send_notice_box(p.conn, "Item tidak ditemukan");
                        }
                        return;
                    }
                    if (buffer.pricetype == 0) {
                        if (p.getGold() < price) {
                            send_notice_box(p.conn, "Tidak cukup " + price + " emas");

                            return;
                        }
                        p.updateGold(-price);
                    } else {
                        if (p.getGem() < price) {
                            send_notice_box(p.conn, "Tidak cukup " + price + " permata");
                            return;
                        }
                        p.updateGem(-price);
                    }
                    Item3 itbag = new Item3();
                    itbag.id = idbuy;
                    itbag.clazz = buffer.clazz;
                    itbag.type = buffer.type;
                    itbag.level = buffer.level;
                    itbag.icon = ItemTemplate3.item.get(idbuy).getIcon();
                    itbag.color = buffer.color;
                    itbag.part = ItemTemplate3.item.get(idbuy).getPart();
                    itbag.islock = true;
                    itbag.name = ItemTemplate3.item.get(idbuy).getName();
                    itbag.tier = 0;
                    List<Option> opnew = new ArrayList<>();
                    for (Option op_old : buffer.option) {
                        Option temp = new Option(1, 1, itbag.id);
                        temp.id = op_old.id;
                        if (temp.id != 37 && temp.id != 38) {
                            if (op_old.getParam(0) < 10) {
                                temp.setParam(Util.random(0, 10));
                            } else {
                                temp.setParam(Util.random((9 * op_old.getParam(0)) / 10, op_old.getParam(0)));
                            }
                        } else {
                            temp.setParam(1);
                        }
                        opnew.add(temp);
                    }
                    itbag.op = new ArrayList<>();
                    itbag.op.addAll(opnew);
                    p.item.add_item_bag3(itbag);
                }
                p.item.charInventory(4);
                p.item.charInventory(7);
                p.item.charInventory(3);
                break;
            }
            case 2: {
                if (p.getGem() < 500) {
                    p.sendNoticeBox("Kamu tidak memilii permata");
                    return;
                }

                p.updateGem(-500);
                p.hair = (byte) idbuy;
                for (int i = 0; i < p.map.players.size(); i++) {
                    Player p0 = p.map.players.get(i);
                    if (p0.objectId != p.objectId && Math.abs(p0.x - p.x) < 200 && Math.abs(p0.y - p.y) < 200) {
                        MapService.send_in4_other_char(p0.map, p0, p);
                    }
                }
                Service.sendMainCharInfo(p);
                break;
            }
            case 4: {
                if (idbuy > (ItemTemplate7.item.size() - 1)) {
                    return;
                }
                long price = ItemTemplate7.item.get(idbuy).getPrice() * quanity;
                if (ItemTemplate7.item.get(idbuy).getPricetype() == 0) {
                    if (p.getGold() < price) {
                        send_notice_box(p.conn, "Tidak cukup " + price + " emas");

                        return;
                    }
                    p.updateGold(-price);
                    Log.gI().add_log(p.name,
                            "mua " + quanity + " item " + ItemTemplate7.item.get(idbuy).getName() + " hết"
                                    + Util.number_format(price) + " vàng");
                } else {
                    if (p.getGem() < price) {
                        send_notice_box(p.conn, "Tidak cukup " + price + " permata");
                        return;
                    }
                    p.updateGem(-price);
                    Log.gI().add_log(p.name,
                            "mua " + quanity + " item " + ItemTemplate7.item.get(idbuy).getName() + " hết"
                                    + Util.number_format(price) + " ngọc");
                }
                int quant_add_bag = quanity + p.item.total_item_by_id(7, idbuy);
                if (quant_add_bag > 32000) {
                    send_notice_box(p.conn, "Tidak bisa membeli lagi");
                    return;
                }
                Item47 itbag = new Item47();
                itbag.id = idbuy;
                itbag.quantity = (short) quanity;
                itbag.category = 7;
                p.item.add_item_bag47(7, itbag);
                p.item.charInventory(4);
                p.item.charInventory(7);
                p.item.charInventory(3);
                break;
            }
            case 6: {
                int value = 200;
                if (idbuy >= 500 && idbuy <= 816) {
                    value = 500;
                } else if (idbuy >= 817 && idbuy <= 826) {
                    value = 1000;
                }
                if (p.myclan.icon == 0) {
                    if (value > 200 && p.getGem() < (value - 200)) {
                        Service.send_notice_box(p.conn, "Tidak cukup " + (value - 200) + " permata!");
                        return;
                    }
                    p.updateGem(-(value - 200));
                    send_notice_box(p.conn, "Selamat! Kamu berhasil mendaftarkan guild. Silakan buka Menu > Fitur > Guild untuk melihat informasi guild.");
                    if (p.getGem() < value) {
                        Service.send_notice_box(p.conn, "Tidak cukup " + value + " permata!");
                        return;
                    }
                    p.updateGem(-value);
                    send_notice_box(p.conn, "Icon berhasil diperbaharui");
                }
                p.item.charInventory(5);
                p.myclan.icon = idbuy;
                MapService.broadcastMainCharInfo(p.map, p);
                Service.sendMainCharInfo(p);
                for (ClanMember mem : p.myclan.members) {
                    Player p0 = GameMap.get_player_by_name(mem.name);
                    if (p0 != null) {
                        MapService.broadcastMainCharInfo(p0.map, p0);
                        Service.sendMainCharInfo(p0);
                    }
                }
                break;
            }
            case 10: {
                Message m2 = new Message(77);
                m2.writer().writeByte(6);
                p.conn.addmsg(m2);
                m2.cleanup();
                //
                ItemTemplate3 it = ItemTemplate3.item.get(idbuy);
                m2 = new Message(77);
                m2.writer().writeByte(0);
                m2.writer().writeInt(it.getId());
                m2.writer().writeUTF("Pembuatan sayap");
                m2.writer().writeInt(200_000);
                m2.writer().writeShort(60);
                m2.writer().writeInt(0);
                m2.writer().writeByte(6);
                m2.writer().writeShort(8);
                m2.writer().writeShort(80);
                m2.writer().writeShort(9);
                m2.writer().writeShort(60);
                m2.writer().writeShort(10);
                m2.writer().writeShort(40);
                m2.writer().writeShort(11);
                m2.writer().writeShort(20);
                m2.writer().writeShort(0);
                m2.writer().writeShort(100);
                m2.writer().writeShort(3);
                m2.writer().writeShort(20);
                p.conn.addmsg(m2);
                m2.cleanup();
                //
                m2 = new Message(77);
                m2.writer().writeByte(1);
                m2.writer().writeUTF(it.getName());
                p.conn.addmsg(m2);
                m2.cleanup();
                p.is_create_wing = true;
                break;
            }
            case 8: {
                if (p.myclan != null && p.myclan.members.get(0).name.equals(p.name)) {
                    long price = ItemTemplate4.item.get(idbuy).getPrice() * quanity;
                    if (ItemTemplate4.item.get(idbuy).getPricetype() == 0) {
                        if (p.myclan.get_vang() < price) {
                            send_notice_box(p.conn, "Tidak cukup " + price + " emas");

                            return;
                        }
                        p.myclan.updateGold(-price);
                    } else {
                        if (p.myclan.get_ngoc() < price) {
                            send_notice_box(p.conn, "Tidak cukup " + price + " permata");
                            return;
                        }
                        p.myclan.updateGem(-((int) price));
                    }
                    Item47 itbag = new Item47();
                    itbag.id = idbuy;
                    itbag.quantity = (short) 1;
                    itbag.category = 4;
                    p.myclan.clanItems.add(itbag);
                }
                break;
            }
            case 11: {
                send_notice_box(p.conn, "Tidak cukup token");
                break;
            }
        }
        if (type == 6 || type == 10) {
        } else {
            send_notice_box(p.conn, "Pembelian berhasil");
        }
    }

    public static void remove_time_use_item(Session conn, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        byte cat = m2.reader().readByte();
        short iditem = m2.reader().readShort();
        // System.out.println(type);
        // System.out.println(cat);
        // System.out.println(iditem);
        if (type == 6) {
            switch (cat) {
                case 3: {
                    Item3 it = conn.p.item.bag3[iditem];
                    if (it != null && it.time_use > 0) {
                        int ngoc_ = conn.p.getGem();
                        if (ngoc_ > 4) {
                            long price = it.time_use - System.currentTimeMillis();
                            price /= 3_600_000;
                            price = (price > 4) ? (price + 1) : 5;
                            if (ngoc_ >= price) {
                                send_box_input_yesno(conn, 115,
                                        "Đồng ý dùng " + price + " ngọc để mở khóa thời gian sử dụng?");
                            } else {
                                send_box_input_yesno(conn, 115,
                                        "Đồng ý dùng " + ngoc_ + " ngọc để mở khóa " + ngoc_ + "h");
                            }
                            conn.p.id_remove_time_use = iditem;
                        } else {
                            send_notice_box(conn, "Tối thiểu 5 ngọc!");
                        }
                    }
                    break;
                }
            }
        }
    }

    public static void open_box_notice_item(Player p, String notice, short[] id, short[] quant, short[] type)
            throws IOException {
        Message m = new Message(78);
        m.writer().writeUTF(notice);
        m.writer().writeByte(id.length);
        for (int i = 0; i < id.length; i++) {
            switch (type[i]) {
                case 3: {
                    m.writer().writeUTF(ItemTemplate3.item.get(id[i]).getName()); // name
                    m.writer().writeShort(ItemTemplate3.item.get(id[i]).getIcon()); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(ItemTemplate3.item.get(id[i]).getColor()); // color
                    //
                    ItemTemplate3 item = ItemTemplate3.item.get(id[i]);
                    Item3 itbag = new Item3();
                    itbag.id = item.getId();
                    itbag.name = item.getName();
                    itbag.clazz = item.getClazz();
                    itbag.type = item.getType();
                    itbag.level = 10;
                    itbag.icon = item.getIcon();
                    itbag.op = new ArrayList<>();
                    itbag.op.addAll(item.getOp());
                    itbag.color = item.getColor();
                    itbag.part = item.getPart();
                    itbag.tier = 0;
                    itbag.islock = false;
                    itbag.time_use = 0;
                    p.item.add_item_bag3(itbag);
                    break;
                }
                case 4: {
                    if (id[i] == -1) {
                        m.writer().writeUTF("Emas"); // name
                        m.writer().writeShort(0); // icon
                        p.updateGold(Util.random(50, 150));
                    } else {
                        m.writer().writeUTF(ItemTemplate4.item.get(id[i]).getName()); // name
                        m.writer().writeShort(ItemTemplate4.item.get(id[i]).getIcon()); // icon
                        //
                        Item47 it = new Item47();
                        it.id = (short) (id[i]);
                        it.quantity = (short) Util.random(1, 3);
                        it.category = 4;
                        p.item.add_item_bag47(4, it);
                    }
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    break;
                }
                case 7: {
                    m.writer().writeUTF(ItemTemplate7.item.get(id[i]).getName()); // name
                    m.writer().writeShort(ItemTemplate7.item.get(id[i]).getIcon()); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    //
                    Item47 it = new Item47();
                    it.id = (short) (id[i]);
                    it.quantity = (short) Util.random(1, 3);
                    it.category = 7;
                    p.item.add_item_bag47(7, it);
                    break;
                }
            }
        }
        m.writer().writeUTF("");
        m.writer().writeByte(1);
        m.writer().writeByte(1);
        p.conn.addmsg(m);
        m.cleanup();
        p.item.charInventory(4);
        p.item.charInventory(7);
        p.item.charInventory(3);
    }

    public static void Show_open_box_notice_item(Player p, String notice, short[] id, int[] quant, short[] type)
            throws IOException {
        Message m = new Message(78);
        m.writer().writeUTF(notice);
        m.writer().writeByte(id.length);
        for (int i = 0; i < id.length; i++) {
            switch (type[i]) {
                case 3: {
                    m.writer().writeUTF(ItemTemplate3.item.get(id[i]).getName()); // name
                    m.writer().writeShort(ItemTemplate3.item.get(id[i]).getIcon()); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(ItemTemplate3.item.get(id[i]).getColor()); // color
                    break;
                }
                case 4: {
                    if (id[i] == -1) {
                        m.writer().writeUTF("Emas"); // name
                        m.writer().writeShort(0); // icon
                    } else if (id[i] == -2) {
                        m.writer().writeUTF("Permata"); // name
                        m.writer().writeShort(246); // icon
                    } else {
                        m.writer().writeUTF(ItemTemplate4.item.get(id[i]).getName()); // name
                        m.writer().writeShort(ItemTemplate4.item.get(id[i]).getIcon()); // icon
                    }
                    m.writer().writeInt(quant[i]); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    break;
                }
                case 7: {
                    m.writer().writeUTF(ItemTemplate7.item.get(id[i]).getName()); // name
                    m.writer().writeShort(ItemTemplate7.item.get(id[i]).getIcon()); // icon
                    m.writer().writeInt(quant[i]); // quantity
                    m.writer().writeByte(type[i]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    break;
                }
            }
        }
        m.writer().writeUTF("");
        m.writer().writeByte(1);
        m.writer().writeByte(1);
        p.conn.addmsg(m);
        m.cleanup();
        p.item.charInventory(4);
        p.item.charInventory(7);
        p.item.charInventory(3);
    }

    public static void sendReward(Player p, String notice, Reward reward, int multiple) throws IOException {
        Message m = new Message(78);
        m.writer().writeUTF(notice);
        m.writer().writeByte(reward.getItems().size());
        for (Item item : reward.getItems()) {
            switch (item.getCategory()) {
                case 3 -> {
                    Item3 equip = Item3.fromTemplate((short) item.getItemId());
                    m.writer().writeUTF(equip.name); // name
                    m.writer().writeShort(equip.icon); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(3); // type in bag
                    m.writer().writeByte(equip.tier); // tier
                    m.writer().writeByte(equip.color); // color
                    p.item.add_item_bag3(equip);
                }
                case 4 -> {
                    if (item.getItemId() == -1) {
                        m.writer().writeUTF("Emas"); // name
                        m.writer().writeShort(0); // icon
                        p.updateGold((long) item.getQuantity() * multiple);
                    } else if (item.getItemId() == -2) {
                        m.writer().writeUTF("Permata"); // name
                        m.writer().writeShort(246); // icon
                        p.updateGem((long) item.getQuantity() * multiple);
                    } else {
                        m.writer().writeUTF(ItemTemplate4.item.get(item.getItemId()).getName()); // name
                        m.writer().writeShort(ItemTemplate4.item.get(item.getItemId()).getIcon()); // icon
                        p.item.add_item_bag47((short) item.getItemId(), (short) (item.getQuantity() * multiple), (byte) item.getCategory());
                    }
                    m.writer().writeInt(item.getQuantity() * multiple); // quantity
                    m.writer().writeByte(item.getCategory()); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                }
                case 7 -> {
                    m.writer().writeUTF(ItemTemplate7.item.get(item.getItemId()).getName()); // name
                    m.writer().writeShort(ItemTemplate7.item.get(item.getItemId()).getIcon()); // icon
                    m.writer().writeInt(item.getQuantity() * multiple); // quantity
                    m.writer().writeByte(item.getCategory()); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    p.item.add_item_bag47((short) item.getItemId(), (short) (item.getQuantity() * multiple), (byte) item.getCategory());
                }
            }
        }
        m.writer().writeUTF("");
        m.writer().writeByte(1);
        m.writer().writeByte(1);
        p.conn.addmsg(m);
        m.cleanup();
        p.item.charInventory(4);
        p.item.charInventory(7);
        p.item.charInventory(3);
    }

    public static void Show_open_box_notice_item(Player p, String notice, List<box_item_template> items)
            throws IOException {
        Message m = new Message(78);
        m.writer().writeUTF(notice);
        m.writer().writeByte(items.size());
        for (box_item_template tem : items) {
            switch (tem.catagory) {
                case 3: {
                    m.writer().writeUTF(ItemTemplate3.item.get(tem.id).getName()); // name
                    m.writer().writeShort(ItemTemplate3.item.get(tem.id).getIcon()); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(3); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(ItemTemplate3.item.get(tem.id).getColor()); // color
                    break;
                }
                case 4: {
                    if (tem.id == -1) {
                        m.writer().writeUTF("Emas"); // name
                        m.writer().writeShort(0); // icon
                    } else if (tem.id == -2) {
                        m.writer().writeUTF("Permata"); // name
                        m.writer().writeShort(246); // icon
                    } else {
                        m.writer().writeUTF(ItemTemplate4.item.get(tem.id).getName()); // name
                        m.writer().writeShort(ItemTemplate4.item.get(tem.id).getIcon()); // icon
                    }
                    m.writer().writeInt(tem.quantity); // quantity
                    m.writer().writeByte(tem.catagory); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    break;
                }
                case 7: {
                    m.writer().writeUTF(ItemTemplate7.item.get(tem.id).getName()); // name
                    m.writer().writeShort(ItemTemplate7.item.get(tem.id).getIcon()); // icon
                    m.writer().writeInt(tem.quantity); // quantity
                    m.writer().writeByte(tem.catagory); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    break;
                }
            }
        }
        m.writer().writeUTF("");
        m.writer().writeByte(1);
        m.writer().writeByte(1);
        p.conn.addmsg(m);
        m.cleanup();
        p.item.charInventory(3);
        p.item.charInventory(4);
        p.item.charInventory(7);

    }

    /**
     * Efek map (part_char type 111) berdiri sendiri. Sekarang cuma pembungkus tipis
     * ke MapItemEffect supaya cuma ada SATU implementasi paket -49/sub 1.
     * Isi paket identik dengan versi lama: idnpc = key, b3/b4 = area blocking,
     * b7 = flag, dedup = 2.
     */
    public static void sendEffMap(Player p, int idnpc, int id_eff, int x, int y, int b3, int b4, int b7) throws IOException {
        MapItemEffect.sendTo(p, id_eff, x, y,
                MapItemEffect.Options.create().key(idnpc).block(b3, b4).flag(b7).dedup(2));
    }

}