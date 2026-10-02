package game.ai;

import client.Pet;
import client.Player;
import client.io.Message;
import core.Manager;
import core.Service;
import core.Util;
import game.guild.Guild;
import game.map.GameMap;
import game.map.MapService;
import game.mount.Mount;
import io.ytcode.pathfinding.astar.Path;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.player.PlayerData;
import model.player.PlayerWear;
import template.*;
import utils.SQLHelper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Getter
@Setter
public class PlayerBot extends MainObject {
    private PlayerData data;
    private Skill[] skills;
    private Mount mount;
    private List<Pet> pets;
    private Guild guild;

    private short hairId;
    private short maskId;
    private short cloakId;
    private short weaponId;
    private short wingId;
    private short titleId;

    private Path path;

    private GameMap map;

    private int[] fashion;

    public PlayerBot(int id) {
        setup(id);
    }

    public static PlayerBot copyFrom(Player player) {
        PlayerBot playerBot = new PlayerBot(player.objectId);

        playerBot.fashion = player.fashion;
        playerBot.guild = player.myclan;
        playerBot.pets = player.mypet;
        playerBot.map = player.map;

        playerBot.map_id = player.map_id;
        playerBot.zone_id = player.zone_id;
        playerBot.x = player.x;
        playerBot.y = player.y;

        playerBot.maskId = Service.getMaskId(player);
        playerBot.cloakId = Service.getCloakId(player);
        playerBot.hairId = Service.getHairId(player);
        playerBot.weaponId = Service.getWeaponId(player);
        playerBot.wingId = Service.getWingId(player);
        playerBot.titleId = Service.getTitleId(player);
        playerBot.initial();
        return playerBot;
    }

    private void setup(int id) {
        data = SQLHelper.selectFrom("player").where("id", id).firstAsModel(PlayerData.class);
        clazz = (byte) data.getClazz();
        level = (short) data.getLevel();
        fashion = new int[]{-1, -1, -1, -1, -1, -1, -1};
        pets = new ArrayList<>();
        objectId = 10_000 + id;
        effectDefault = new ArrayList<>();

    }

    public void initial() {
        loadSkill();
        maxHp = getMaxHP();
        hp = maxHp;
        maxMp = getMaxMP();
        mp = maxMp;
        isdie = false;
    }

    private int getPoint(int i) {
        int point = 0;
        switch (i) {
            case 1 -> point += data.point1 + getPlusPoint(23);
            case 2 -> point += data.point2 + getPlusPoint(24);
            case 3 -> point += data.point3 + getPlusPoint(25);
            case 4 -> point += data.point4 + getPlusPoint(26);
        }
        return point;
    }

    public int getPlusPoint(int i) {
        int param = 0;
        switch (i) {
            case 23 -> {
                param += getTotalItemParam(i);
                EffTemplate ef = getEffectDefault(23);
                if (ef != null) {
                    param += (data.point1 * (ef.param / 100)) / 100;
                }
                for (Pet temp : pets) {
                    if (temp.is_follow) {
                        for (PetOption op : temp.op) {
                            if (op.id == 23) {
                                param += op.value;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            case 24 -> {
                param += getTotalItemParam(i);
                for (Pet temp : pets) {
                    if (temp.is_follow) {
                        for (PetOption op : temp.op) {
                            if (op.id == 24) {
                                param += op.value;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            case 25 -> {
                param += getTotalItemParam(i);
                for (Pet temp : pets) {
                    if (temp.is_follow) {
                        for (PetOption op : temp.op) {
                            if (op.id == 25) {
                                param += op.value;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            case 26 -> {
                param += getTotalItemParam(i);
                for (Pet temp : pets) {
                    if (temp.is_follow) {
                        for (PetOption op : temp.op) {
                            if (op.id == 26) {
                                param += op.value;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return param;
    }

    @Override
    public int getBaseDamage() {
        return getStatInfo(40);
    }

    @Override
    public int getDameProp(int type) {
        if (type == 0) {
            long dame = getTotalItemParam(0);
            switch (data.getClazz()) {
                case 0, 1 -> dame += getPoint(1) * 4L;
                case 2, 3 -> dame += getPoint(4) * 4L;
            }
            dame += ((dame * (getPercentDameProp(0) / 100)) / 100);
            if (dame > 2_000_000_000) {
                dame = 2_000_000_000;
            }
            return (int) dame;
        }
        long dprop = 0;
        switch (data.getClazz()) {
            case 0: {
                if (type == 2) {
                    dprop += getPoint(1) * 4L;
                    dprop += getTotalItemParam(2);
                }
                break;
            }
            case 1: {
                if (type == 4) {
                    dprop += getPoint(1) * 4L;
                    dprop += getTotalItemParam(4);
                }
                break;
            }
            case 2: {
                if (type == 1) {
                    dprop += getPoint(4) * 4L;
                    dprop += getTotalItemParam(1);
                }
                break;
            }
            case 3: {
                if (type == 3) {
                    dprop += getPoint(4) * 4L;
                    dprop += getTotalItemParam(3);
                }
                break;
            }
        }
        dprop += ((dprop * (getPercentDameProp(type) / 100)) / 100);
        if (dprop > 2_000_000_000) {
            dprop = 2_000_000_000;
        }
        return (int) dprop;
    }

    public int getSkillPointPlus(int i) {
        int par = 0;
        if (i >= 1 && i <= 8 || i == 19 || i == 20 || i == 17) {
            par = getTotalItemParam(37);
        }
        if ((i >= 9 && i <= 16) || i == 18) {
            par = getTotalItemParam(38);
        }
        return Math.min(par, 5);
    }

    @Override
    public int getDefensePercent(int type) {
        int param = getTotalItemParam(type) + getTotalSkillParam(type);
        EffTemplate ef = getEffectDefault(4);
        if (ef != null) {
            param -= 1000;
        }
        if (param < 0) {
            param = 0;
        }
        return param;
    }

    @Override
    public int getTotalItemParam(int id) {
        int param = 0;
        for (PlayerWear temp : data.getItemwear()) {
            if (temp != null) {
                if (data.getLevel() < temp.getLevel()) {
                    continue;
                }
                for (Option op : temp.getOption()) {
                    if (op.id == id) {
                        param += op.getParam(temp.getTier());
                    }
                }
            }
        }
        return param;
    }

    @Override
    public int getMaxHP() {
        long hpm = (int) (2500 * Manager.ratio_hp);
        switch (data.getClazz()) {
            case 0 -> hpm += (550 + getPoint(3) * 320L);
            case 1 -> hpm += (getPoint(3) * 300L);
            case 2 -> hpm += (50 + getPoint(3) * 310L);
            case 3 -> hpm += (120 + getPoint(3) * 300L);
        }
        int percent = getTotalItemParam(27);
        if (data.getSkill()[9] > 0) {
            for (Option op : skills[9].mLvSkill[data.getSkill()[9] - 1].minfo) {
                if (op.id == 27) {
                    percent += op.getParam(0);
                    break;
                }
            }
        }
        hpm += ((hpm * (percent / 100)) / 100);
        if (mount != null) {
            switch (mount.getType()) {
                case 11, 12, 13, 20, 114, 121 -> {
                    hpm += (hpm / 10);
                }
            }
        }
        EffTemplate ef = getEffectDefault(2);
        if (ef != null) {
            hpm = (hpm * 8) / 10;
        }
        if (hpm > 2_000_000_000) {
            hpm = 2_000_000_000;
        }
        return (int) (hpm * Manager.ratio_hp);
    }

    @Override
    public int getMaxMP() {
        long mpm = 250;
        switch (data.getClazz()) {
            case 0, 1 -> mpm += getPoint(4) * 10L;
            case 2 -> mpm += 10 + getPoint(3) + getPoint(4) * 11L;
            case 3 -> mpm += 5 + getPoint(3) + getPoint(4) * 11L;
        }
        int percent = getTotalItemParam(28);
        if (data.getSkill()[10] > 0) {
            for (Option op : skills[10].mLvSkill[data.getSkill()[10] - 1].minfo) {
                if (op.id == 28) {
                    percent += op.getParam(0);
                    break;
                }
            }
        }
        mpm += ((mpm * (percent / 100)) / 100);
        EffTemplate ef = getEffectDefault(2);
        if (ef != null) {
            mpm = (mpm * 8) / 10;
        }
        if (mpm > 2_000_000_000) {
            mpm = 2_000_000_000;
        }
        return (int) mpm;
    }

    @Override
    public int getTotalSkillParam(int id) {
        int param = 0;

        byte[] skillPoint = data.getSkill();
        for (int i = 0; i < skillPoint.length; i++) {
            if (skillPoint[i] > 0) {
                Option[] temp = skills[i].mLvSkill[getSkillPoint(i) - 1].minfo;
                for (Option op : temp) {
                    if (op.id == id) {
                        param += op.getParam(0);
                    }
                }
            }
        }
        return param;
    }

    @Override
    public int getPierce() {
        int pie = getTotalItemParam(36) + getTotalSkillParam(36);
        pie += getPoint(4) * 2;
        EffTemplate ef = getEffectDefault(36);
        if (ef != null) {
            pie += ef.param;
        }
        return (int) (pie / 2.1);
    }

    @Override
    public int getReflectDamage() {
        // Jika skill refleksi aktif → dijamin 100%
        EffTemplate ef = getEffectDefault(35);
        if (ef != null) {
            return 10_000;
        }
        int param = 2 * getPoint(3);
        param += getTotalItemParam(35);
        param += getTotalSkillParam(35); // FIX: sertakan bonus refleksi dari skill
        return (int) (param / 2.1);
    }

    @Override
    public int getMiss() {
        int param = 2 * getPoint(2);
        param += getTotalItemParam(34);
        EffTemplate ef = getEffectDefault(34);
        if (ef != null) {
            param += ef.param;
        }
        return (int) (param / 1.8);
    }

    @Override
    public int getCrit() {
        int crit = getTotalItemParam(33) + getTotalSkillParam(33);
        crit += getPoint(1) * 2;
        EffTemplate ef = getEffectDefault(33);
        if (ef != null) {
            crit += ef.param;
        }
        return (int) (crit / 2.1);
    }

    public int getSkillPoint(int i) {
        if (data.getSkill()[i] > 0) {
            int val = data.getSkill()[i] + getSkillPointPlus(i);
            return Math.min(val, 15);
        }
        return 0;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    private void loadSkill() {

        byte[] data = switch (clazz) {
            case 0 -> Manager.gI().msg_29_chienbinh;
            case 1 -> Manager.gI().msg_29_satthu;
            case 2 -> Manager.gI().msg_29_phapsu;
            case 3 -> Manager.gI().msg_29_xathu;
            default -> {
                log.error("Invalid clazz value: {}", clazz);
                yield null;
            }
        };

        if (data == null) {
            log.error("Skill data is null for clazz: {}", clazz);
            return;
        }

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteStream)) {

            int size = input.readByte();
            skills = new Skill[size];

            for (int i = 0; i < size; i++) {

                Skill skill = new Skill();
                skill.id = input.readByte();
                skill.iconid = input.readByte();
                skill.name = input.readUTF();
                skill.type = input.readByte();
                skill.range = input.readShort();
                skill.detail = input.readUTF();
                skill.typeBuff = input.readByte();
                skill.subEff = input.readByte();

                int levelSize = input.readByte();
                skill.mLvSkill = new LvSkill[levelSize];

                for (int j = 0; j < levelSize; j++) {

                    LvSkill level = new LvSkill();
                    level.mpLost = input.readShort();
                    level.LvRe = input.readShort();
                    level.delay = input.readInt();
                    level.timeBuff = input.readInt();
                    level.per_Sub_Eff = input.readByte();
                    level.time_Sub_Eff = input.readShort();
                    level.plus_Hp = input.readShort();
                    level.plus_Mp = input.readShort();

                    int optionSize = input.readByte();
                    level.minfo = new Option[optionSize];

                    for (int k = 0; k < optionSize; k++) {
                        level.minfo[k] = new Option(
                                input.readUnsignedByte(),
                                input.readInt(),
                                (short) 0
                        );
                    }

                    level.nTarget = input.readByte();
                    level.range_lan = input.readShort();

                    skill.mLvSkill[j] = level;
                }

                skill.performDur = input.readShort();
                skill.typePaint = input.readByte();

                skills[skill.id] = skill;
            }

        } catch (Exception e) {
            log.error("Failed to load skills for clazz {}: {}", clazz, e.getMessage(), e);
            skills = new Skill[0]; // fallback safe state
        }
    }

    public void takeDamage(GameMap gameMap, MainObject hitter, int idxSkill, LvSkill temp, int type) throws IOException {

        boolean penetration = hitter.getPierce() > Util.random(10_000);
        float bonusDamage = 0;
        float damageReduction = 0;
        float ptCrit = 0;
        int lifesteal = 0;
        long totalDamage = hitter.getBaseDamage();

        //<editor-fold desc="BASE DAMAGE CALCULATION">
        if (type == 0) {
            int tempDameProp = hitter.getDameProp(0);
            int dameProp = tempDameProp - (int) (penetration ? 0 : tempDameProp * 0.0001 * getDefensePercent(16));
            totalDamage += Math.max(dameProp, 0);
        } else if (type == 1) {
            switch (hitter.clazz) {
                case 0: {
                    int tempDameProp = hitter.getDameProp(2);
                    int dameProp = tempDameProp
                            - (int) (penetration ? 0 : tempDameProp * 0.0001 * getDefensePercent(18));
                    totalDamage += Math.max(dameProp, 0);
                    break;
                }
                case 1: {
                    int tempDameProp = hitter.getDameProp(4);
                    int dameProp = tempDameProp
                            - (int) (penetration ? 0 : tempDameProp * 0.0001 * getDefensePercent(20));
                    totalDamage += Math.max(dameProp, 0);
                    break;
                }
                case 2: {
                    int tempDameProp = hitter.getDameProp(1);
                    int dameProp = tempDameProp
                            - (int) (penetration ? 0 : tempDameProp * 0.0001 * getDefensePercent(17));
                    totalDamage += Math.max(dameProp, 0);
                    break;
                }
                case 3: {
                    int tempDameProp = hitter.getDameProp(3);
                    int dameProp = tempDameProp
                            - (int) (penetration ? 0 : tempDameProp * 0.0001 * getDefensePercent(19));
                    totalDamage += Math.max(dameProp, 0);
                    break;
                }
            }
        } else {
            totalDamage += hitter.getDameProp(0);
        }
        //</editor-fold>

        //<editor-fold desc="SKILL ATTRIBUTE CALCULATION">
        if (hitter.isPlayer()) {
            if (idxSkill == 19 && hitter.clazz == 1) {
                for (Option op : temp.minfo) {
                    if (op.id == 4) {
                        totalDamage += op.getParam(0);
                    }
                    if (op.id == 11) {
                        totalDamage += totalDamage * (op.getParam(0) / 100) / 100;
                    }
                }
            } else {
                for (int i = temp.minfo.length - 1; i >= 0; i--) {
                    Option op = temp.minfo[i];
                    if (type == 0) {
                        if (op.id == 0) {
                            totalDamage += op.getParam(0);
                        }
                        if (op.id == 7) {
                            totalDamage += totalDamage * (op.getParam(0) / 100) / 100;
                        }
                    } else {
                        if (op.id == 1 || op.id == 2 || op.id == 3 || op.id == 4) {
                            totalDamage += op.getParam(0);
                        }
                        if (op.id == 9 || op.id == 10 || op.id == 11 || op.id == 8) {
                            totalDamage += totalDamage * (op.getParam(0) / 100) / 100;
                        }
                    }
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="MOUNT CALCULATION">
        Mount hitterMount = null;


        if (hitter instanceof Player p) {
            hitterMount = p.mount;
        } else if (hitter instanceof PlayerBot bot) {
            hitterMount = bot.mount;
        }

        if (hitterMount != null) {
            if (hitterMount.getType() == 3) {
                bonusDamage += 0.2F;
            } else if (hitterMount.getType() == 5) {
                bonusDamage += 0.4F;
            } else if (hitterMount.getType() == 11 || hitterMount.getType() == 12) {
                bonusDamage += 0.1F;
            } else if ((hitterMount.getType() == 20 && hitterMount.getPart() == 114)
                    || (hitterMount.getType() == 22 && hitterMount.getPart() == 117)) {
                bonusDamage += 0.15F;
            } else if ((hitterMount.getType() == 20 && hitterMount.getPart() == 116)) {
                bonusDamage += 0.35F;
            }
        }
        //</editor-fold>

        //<editor-fold desc="DAMAGE REDUCTION CALCULATION">
        List<Float> giamdame = new ArrayList<>();
        EffTemplate ef = hitter.getEffectDefault(3);
        if (ef != null) {
            giamdame.add((float) 0.2);
        }
        if (hitter instanceof Player p && p.getlevelpercent() < 0) {
            giamdame.add((float) 0.5);
        }

        //</editor-fold>

        //<editor-fold desc="WING CALCULATION">
        if (hitter.isPlayer() && hitter instanceof Player p) {
            EffTemplate temp2 = hitter.getEffectDefault(StrucEff.PowerWing);
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

                        hitter.addEffectDefault(StrucEff.PowerWing, 1000, time);

                        try {
                            Message mw = new Message(40);
                            mw.writer().writeByte(0);
                            mw.writer().writeByte(1);
                            mw.writer().writeShort(hitter.objectId);
                            mw.writer().writeByte(21);
                            mw.writer().writeInt(time);
                            mw.writer().writeShort(hitter.objectId);
                            mw.writer().writeByte(0);
                            mw.writer().writeByte(30);
                            byte[] id__ = new byte[]{7, 8, 9, 10, 11, 15, 0, 1, 2, 3, 4, 14};
                            int[] par__ = new int[]{2000, 2000, 2000, 2000, 2000, 2000,
                                    2 * (hitter.getStatInfo(0) / 10), 2 * (hitter.getStatInfo(1) / 10),
                                    2 * (hitter.getStatInfo(2) / 10), 2 * (hitter.getStatInfo(3) / 10),
                                    2 * (hitter.getStatInfo(4) / 10), 2 * (hitter.getStatInfo(14) / 10)};
                            mw.writer().writeByte(id__.length);

                            for (int i = 0; i < id__.length; i++) {
                                mw.writer().writeByte(id__[i]);
                                mw.writer().writeInt(par__[i]);
                            }

                            MapService.sendMsgPlayerInside(p.map, p, mw, true);
                            mw.cleanup();
                        } catch (Exception ignore) {
                        }
                    }
                }
            } else {
                bonusDamage += 0.2F;
            }
        }
        //</editor-fold>


        //<editor-fold desc="FINAL DAMAGE CALCULATION">
        int def = getDefBase();

        if (totalDamage > 2_000_000_000) {
            totalDamage = 2_000_000_000;
        }
        totalDamage -= (long) (totalDamage * 0.35);
        totalDamage -= (penetration ? 0 : def);
        if (!giamdame.isEmpty()) {
            for (float f : giamdame) {
                totalDamage -= (long) (totalDamage * f);
            }
        }
        //</editor-fold>

        List<Eff_TextFire> textDamage = new ArrayList<>();

        if (lifesteal > 0) {
            textDamage.add(new Eff_TextFire(0, (int) totalDamage));
            textDamage.add(new Eff_TextFire(2, lifesteal));
            hp += lifesteal;
            if (hp > getMaxHP()) {
                hp = getMaxHP();
            }
        }

        if (penetration) {
            textDamage.add(new Eff_TextFire(1, (int) totalDamage));
        } else if (hitter.getCrit() > Util.random(10_000)) {
            // dame *= 2;
            totalDamage += (long) (totalDamage * (ptCrit + 1));
            if (totalDamage > Integer.MAX_VALUE) {
                totalDamage = Integer.MAX_VALUE;
            }
            textDamage.add(new Eff_TextFire(4, (int) totalDamage));
        }

        hp -= (int) totalDamage;

        //<editor-fold desc="REFLECT DAMAGE CALCULATION">
        if (getReflectDamage() > Util.random(10_000)) {
            int reflectDamage = (int) (totalDamage * 0.5);
            reflectDamage -= hitter.getDefBase();

            if (type == 1) {
                if (hitter.clazz == 0) {
                    reflectDamage -= (int) (reflectDamage * 0.0001 * hitter.getDefensePercent(18));
                } else if (hitter.clazz == 1) {
                    reflectDamage -= (int) (reflectDamage * 0.0001 * hitter.getDefensePercent(20));
                } else if (hitter.clazz == 2) {
                    reflectDamage -= (int) (reflectDamage * 0.0001 * hitter.getDefensePercent(17));
                } else if (hitter.clazz == 3) {
                    reflectDamage -= (int) (reflectDamage * 0.0001 * hitter.getDefensePercent(19));
                }
            } else {
                reflectDamage -= (int) (reflectDamage * 0.0001 * hitter.getDefensePercent(16));
            }
            if (reflectDamage <= 0) {
                reflectDamage = 1;
            }

            textDamage.add(new Eff_TextFire(5, reflectDamage));
            hitter.hp -= reflectDamage;
            if (hitter.hp <= 0) {
                hitter.hp = 5;
            }
        }
        //</editor-fold>

        if (hp <= 0) {
            setDie(map, hitter);
            sendDie(hitter);
        }

        if (hitter instanceof Player p) {
            firePlayer(gameMap, p, idxSkill, (int) totalDamage, textDamage);
        }

    }

    private void sendDie(MainObject hitter) throws IOException {
        map.broadcast(player -> {
            try {
                Message m = new Message(41);
                m.writer().writeShort(objectId);
                m.writer().writeShort(hitter.objectId);
                m.writer().writeShort(hitter.typepk);
                m.writer().writeByte(hitter.getTypeObject());
                player.conn.addmsg(m);
                m.cleanup();
            } catch (Exception ignore) {
            }
        });

    }

    private void firePlayer(GameMap gameMap, Player p, int indexskill, int totalDamage,
                            List<Eff_TextFire> ListFire) throws IOException {

        Message m = new Message(6);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(indexskill);
        m.writer().writeByte(1);
        m.writer().writeShort(objectId);
        m.writer().writeInt(totalDamage); // dame
        m.writer().writeInt(hp); // hp after
        m.writer().writeByte(ListFire.size());
        for (Eff_TextFire ef : ListFire) {
            if (ef == null) {
                continue;
            }
            m.writer().writeByte(ef.type); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
            m.writer().writeInt(ef.dame); // par
        }
        m.writer().writeInt(p.hp);
        m.writer().writeInt(p.mp);
        m.writer().writeByte(11);
        m.writer().writeInt(0);
        MapService.sendMsgPlayerInside(gameMap, p, m, true);
        m.cleanup();
    }

    public void sendBotInfo(Player notify) {

        try {
            List<PlayerPart> parts = data.getItemwear()
                    .stream()
                    .filter(wear -> wear.getIndex() == 0 || wear.getIndex() == 1 || wear.getIndex() == 6 || wear.getIndex() == 7 || wear.getIndex() == 10)
                    .map(wear -> new PlayerPart(wear.getType(), wear.getPart()))
                    .toList();

            Message m = new Message(5);
            m.writer().writeShort(objectId);
            m.writer().writeUTF(data.getName());
            m.writer().writeShort(x);
            m.writer().writeShort(y);
            m.writer().writeByte(data.getClazz());
            m.writer().writeByte(-1); // IS BOT ?
            m.writer().writeByte(data.getBody()[0]);
            m.writer().writeByte(data.getBody()[1]);
            m.writer().writeByte(data.getBody()[2]);
            m.writer().writeShort(data.getLevel());
            m.writer().writeInt(hp);
            m.writer().writeInt(maxMp);
            m.writer().writeByte(0);
            m.writer().writeShort(0); // Point PK

            m.writer().writeByte(parts.size());
            for (PlayerPart wear : parts) {
                m.writer().writeByte(wear.getType());
                m.writer().writeByte(wear.getPart());
                // GEM SIZE TOTAL 12 EQUIPMENT, EACH EQUIPMENT HAS 3 SLOT GEM
                m.writer().writeByte(3);
                // ARRAY INDEX
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);

                m.writer().writeShort(-1); // EFFECT
            }

            if (guild == null) {
                m.writer().writeShort(-1);
            } else {
                m.writer().writeShort(guild.icon);
                m.writer().writeInt(Guild.get_id_clan(guild));
                m.writer().writeUTF(guild.shortName);
                m.writer().writeByte(guild.get_mem_type(data.getName()));
            }

            // PET


            Pet pet = null;
            for (Pet tmp : pets) {
                if (tmp.is_follow) {
                    pet = tmp;
                    break;
                }
            }

            if (pet == null) {
                m.writer().writeByte(-1);
            } else {
                m.writer().writeByte(pet.type);
                m.writer().writeByte(pet.spriteImage);
                m.writer().writeByte(pet.nframe);
            }

            //FASHION
            m.writer().writeByte(fashion.length);
            for (int value : fashion) {
                // Sama seperti fix di Clone.send_in4() / MobAi.send_in4(): elemen fashion
                // harus byte buat client versi lama (< 280), short buat versi baru.
                // Sebelumnya selalu ditulis short, jadi client lama (< 280) bakal misalign
                // baca field2 setelah ini (mount/mask/cloak/weapon/wing/hair/dll) kalau
                // bot punya fashion aktif.
                if (notify.conn.version < 280) {
                    m.writer().writeByte(value);
                } else {
                    m.writer().writeShort(value);
                }
            }

            m.writer().writeShort(-1);          //TransformationImageId
            m.writer().writeByte(mount == null ? -1 : mount.getType());            // Type Mount
            m.writer().writeBoolean(false);     // isFootSnow
            m.writer().writeByte(1);            // TypeFocus
            m.writer().writeByte(0);            // TypeFire

            m.writer().writeShort(maskId);            // MaskID
            m.writer().writeByte(1);              // isMaskInFront()
            m.writer().writeShort(cloakId);            // getCloakId()
            m.writer().writeShort(weaponId);            // getWeaponId()
            m.writer().writeShort(mount == null ? -1 : mount.getPart());            // getMountId()
            m.writer().writeShort(hairId);            // getHairId()
            m.writer().writeShort(wingId);            // getWingId()
            m.writer().writeShort(-1);            // getIdName()
            m.writer().writeShort(-1);            // p.getBodyId()
            m.writer().writeShort(-1);            // getLegId()
            m.writer().writeShort(-1);            //
            notify.conn.addmsg(m);
            m.cleanup();
        } catch (Exception ignore) {

        }

    }

    public void move(Player player) {
        try {
            Message m22 = new Message(4);
            m22.writer().writeByte(0);
            m22.writer().writeShort(0);

            m22.writer().writeShort(objectId);
            m22.writer().writeShort(x);
            m22.writer().writeShort(y);
            m22.writer().writeByte(-1);
            player.conn.addmsg(m22);
            m22.cleanup();
        } catch (Exception ignore) {

        }

    }

}