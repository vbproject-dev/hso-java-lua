package client;

import java.io.IOException;
import java.util.ArrayList;

import core.Manager;
import core.Service;
import core.Util;
import event_daily.CastleSiegeManager;
import event_daily.KingCup;
import game.map.GameMap;
import game.map.MapService;
import template.EffTemplate;
import template.Item3;
import template.Kham_template;
import template.MainObject;
import template.Option;
import template.PetOption;
import template.StrucEff;

public class Body extends MainObject {

    private Player p;

    protected void SetPlayer(Player p) {
        if (this.p != null) {
            return;
        }
        this.p = p;

        kham = new Kham_template();
        effectDefault = new ArrayList<>();
        effectMedal = new ArrayList<>();
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    private int getPoint(int i) {
        int point = 0;
        switch (i) {
            case 1 -> {
                point += p.point1 + getPlusPoint(23);
            }
            case 2 -> {
                point += p.point2 + getPlusPoint(24);
            }
            case 3 -> {
                point += p.point3 + getPlusPoint(25);
            }
            case 4 -> {
                point += p.point4 + getPlusPoint(26);
            }
        }
        return point;
    }

    public int getPlusPoint(int i) {
        int param = 0;
        switch (i) {
            case 23 -> {
                param += getTotalItemParam(i);
                EffTemplate ef = p.getEffectDefault(23);
                if (ef != null) {
                    param += (p.point1 * (ef.param / 100)) / 100;
                }
                for (Pet temp : p.mypet) {
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
                for (Pet temp : p.mypet) {
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
                for (Pet temp : p.mypet) {
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
                for (Pet temp : p.mypet) {
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
    public int getTotalItemParam(int id) {
        int param = 0;
        // Baca dari equipment
        for (Item3 temp : p.item.wear) {
            if (temp != null) {
                if (p.level < temp.level) continue;
                for (Option op : temp.op) {
                    if (op.id == id) {
                        param += op.getParam(temp.tier);
                    }
                }
            }
        }
        // Tambahkan dari pet yang follow
        // id 23-26 (STR/DEX/VIT/INT) dihandle terpisah di getPoint(), skip agar tidak double-count
        boolean skip = (id >= 23 && id <= 26)   // stat point
               || (id >= 0  && id <= 4)     // elemen damage langsung
               || (id >= 7  && id <= 11);   // % elemen damage

        if (!skip) {
            for (Pet temp : p.mypet) {
                if (temp.is_follow) {
                    for (PetOption op : temp.op) {
                        if (op.id == id) {
                            param += op.value;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return param;
    }

    @Override
    public int getMaxHP() {
        // long hpm = (int) (2500 * Manager.ratio_hp);
        long hpm = 2500;
        switch (p.clazz) {
            case 0 -> hpm += (550 + getPoint(3) * 320L);
            case 1 -> hpm += (getPoint(3) * 300L);
            case 2 -> hpm += (50 + getPoint(3) * 310L);
            case 3 -> hpm += (120 + getPoint(3) * 300L);
        }
        int percent = getTotalItemParam(27);
        if (p.skillPoint[9] > 0) {
            for (Option op : p.skills[9].mLvSkill[p.skillPoint[9] - 1].minfo) {
                if (op.id == 27) {
                    percent += op.getParam(0);
                    break;
                }
            }
        }
        hpm += ((hpm * (percent / 100)) / 100);
        if (p.mount != null) {
            switch (p.mount.getType()) {
                case 11, 12, 13, 20, 114, 121 -> { hpm += (hpm / 10 );}
            }
        }
        EffTemplate ef = p.getEffectDefault(2);
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
        switch (p.clazz) {
            case 0, 1 -> mpm += getPoint(4) * 10L;
            case 2 -> mpm += 10 + getPoint(3) + getPoint(4) * 11L;
            case 3 -> mpm += 5 + getPoint(3) + getPoint(4) * 11L;
        }
        int percent = getTotalItemParam(28);
        if (p.skillPoint[10] > 0) {
            for (Option op : p.skills[10].mLvSkill[p.skillPoint[10] - 1].minfo) {
                if (op.id == 28) {
                    percent += op.getParam(0);
                    break;
                }
            }
        }
        mpm += ((mpm * (percent / 100)) / 100);
        EffTemplate ef = p.getEffectDefault(2);
        if (ef != null) {
            mpm = (mpm * 8) / 10;
        }
        if (mpm > 2_000_000_000) {
            mpm = 2_000_000_000;
        }
        return (int) mpm;
    }

    /**
     * Satuan: jumlah POIN HP (bukan persen) yang dipulihkan tiap 1 tick auto-regen.
     * 1 tick = 5 detik (lihat p.time_buff_hp di update(), interval += 5000L).
     * Dihitung dari persentase stat AUTO_HP_RECOVERY (id 29) terhadap MaxHP.
     */
    @Override
    public int getHpRegen() {
        int percent = getTotalSkillParam(29) + getTotalItemParam(29);
        if (percent <= 0) {
            return 0;
        }
        long maxHp = getMaxHP();
        long hpPerTick = (maxHp * (percent / 100)) / 100; // poin HP / 5 detik
        return (int) hpPerTick;
    }

    /**
     * Satuan: jumlah POIN MP (bukan persen) yang dipulihkan tiap 1 tick auto-regen.
     * 1 tick = 5 detik (lihat p.time_buff_mp di update(), interval += 5000L).
     * Dihitung dari persentase stat AUTO_MANA_RECOVERY (id 30) terhadap MaxMP.
     */
    @Override
    public int getMpRegen() {
        int percent = getTotalSkillParam(30) + getTotalItemParam(30);
        if (percent <= 0) {
            return 0;
        }
        long maxMp = getMaxMP();
        long mpPerTick = (maxMp * (percent / 100)) / 100; // poin MP / 5 detik
        return (int) mpPerTick;
    }

    /**
     * Poin Arena asli milik pemain (field p.pointarena, tersimpan di kolom DB point_arena),
     * bukan dari stat equipment — sebelumnya getStatInfo(112) salah ambil dari getTotalItemParam(112).
     */
    @Override
    public int getArenaPoints() {
        return p.pointarena;
    }

    /**
     * Poin isi ulang permata (akumulasi dari user_topup type=GEM, lihat TopupController#find).
     * Di-cap ke Integer.MAX_VALUE karena packet stat masih pakai int.
     */
    @Override
    public int getGemRechargePoints() {
        return (int) Math.min(p.poinIsiUlangPermata, Integer.MAX_VALUE);
    }

    public int getMerchantPoints() {
        // Format singkat seperti dicuop: dibagi 1000 supaya tampil sebagai "Xm" jika 1jt = 1000
        // misal 2.000.000 gold → 2000 → client tampilkan "2.000" atau custom label
        return (int) Math.min(p.gold_dagang / 1_000L, Integer.MAX_VALUE);
    }

    public int getGemUsagePoints() {
        return (int) Math.min(p.poinIsiUlangEmas, Integer.MAX_VALUE);
    }

    @Override
    public int getTotalSkillParam(int id) {
        int param = 0;
        for (int i = 0; i < p.skillPoint.length; i++) {
            if (p.skillPoint[i] > 0) {
                Option[] temp = p.skills[i].mLvSkill[getSkillPoint(i) - 1].minfo;
                for (Option op : temp) {
                    if (op.id == id) {
                        param += op.getParam(0);
                    }
                }
            }
        }
        return param;
    }

//    @Override
//    public int getPierce() { // penetrasi
//        int pie = getTotalItemParam(36) + getTotalSkillParam(36);
//        pie += getPoint(4) * 2;
//        EffTemplate ef = getEffectDefault(36);
//        if (ef != null) {
//            pie += ef.param;
//        }
//        return Math.min((int) (pie / 2.1), 7_500);
//    }

    @Override
    public int getPierce() { // penetrasi
        int param = 2 * getPoint(4);
        param += getTotalItemParam(36);
        param += getTotalSkillParam(36);
        EffTemplate ef = getEffectDefault(36);
        if (ef != null) {
            param += ef.param;
        }
        return Math.min((int) (param / 2.1), 7_500);
    }

    @Override
    public int getReflectDamage() { // refleksi damage
        int param = 2 * getPoint(3);
        param += getTotalItemParam(35);
        param += getTotalSkillParam(35);
        EffTemplate ef = getEffectDefault(35);
        if (ef != null) {
            param += ef.param;
        }
        return Math.min((int) (param / 2.1), 7_500);
    }

    @Override
    public int getMiss() { // menghindar
        int param = 2 * getPoint(2);
        param += getTotalItemParam(34);
        param += getTotalSkillParam(34);
        EffTemplate ef = getEffectDefault(34);
        if (ef != null) {
            param += ef.param;
        }
        return Math.min((int) (param / 1.8), 7_500);
    }

    @Override
    public int getCrit() {
        int param = 2 * getPoint(1);
        param += getTotalItemParam(33);
        param += getTotalSkillParam(33);
        EffTemplate ef = getEffectDefault(33);
        if (ef != null) {
            param += ef.param;
        }
        return Math.min((int) (param / 2.1), 7_500);
    }

//    @Override
//    public int getCrit() { // kritikal
//        int crit = getTotalItemParam(33) + getTotalSkillParam(33);
//        crit += getPoint(1) * 2;
//        EffTemplate ef = getEffectDefault(33);
//        if (ef != null) {
//            crit += ef.param;
//        }
//        return Math.min((int) (crit / 2.1), 7_500);
//    }

    public int getSkillPoint(int i) {
        if (i < 0 || i >= p.skillPoint.length) return 0;
        if (p.skillPoint[i] > 0) {
            int val = p.skillPoint[i] + getSkillPointPlus(i);
            return Math.min(val, 15);
        }
        return 0;
    }

    @Override
    public int getPercentDefBase() {
        int def = getTotalItemParam(15);
        def += getPoint(2) * 10;
        if (getSkillPoint(15) > 0) {
            for (Option op : p.skills[15].mLvSkill[getSkillPoint(15) - 1].minfo) {
                if (op.id == 15) {
                    def += op.getParam(0);
                    break;
                }
            }
        }
        EffTemplate ef = p.getEffectDefault(24);
        if (ef != null) {
            def += ef.param;
        }
        return def;
    }

    @Override
    public int getDefBase() {
        int def = getTotalItemParam(14);
        switch (p.clazz) {
            case 0, 2 -> def += getPoint(2) * 20;
            case 1, 3 -> def += getPoint(2) * 22;
        }
        def += ((def * (getPercentDefBase() / 100)) / 100);
        if (p.mount != null) {
            switch (p.mount.getType()) {
                case 2, 15, 106 -> def += ((def * 2) / 10);
                case  3, 5, 17, 20, 111, 115 -> def += ((def) / 10);
                case 22, 114, 116, 117  -> def += ((def * 15) / 100);
            }
        }

        EffTemplate ef = p.getEffectDefault(0);
        if (ef != null) {
            def = (def * 8) / 10;
        }
        ef = p.getEffectDefault(15);
        if (ef != null) {
            def += (def * (ef.param / 100)) / 100;
        }
        return (int) (def * 0.8);
    }

    @Override
    public int getPercentDameProp(int type) {
        if (type == 7) {
            int percent = getTotalItemParam(7);
            switch (p.clazz) {
                case 0, 1 -> {
                    percent += getPoint(1) * 20;
                }
                case 2, 3 -> {
                    percent += getPoint(1) * 20 + getPoint(4) * 18;
                }
            }
            if (getSkillPoint(11) > 0) {
                for (Option op : p.skills[11].mLvSkill[getSkillPoint(11) - 1].minfo) {
                    if (op.id == 7) {
                        percent += op.getParam(0);
                        break;
                    }
                }
            }
            EffTemplate eff = getEffectDefault(StrucEff.BuffSTVL);
            if (eff != null) {
                percent += eff.param;
            }
            return percent;
        }
        int perct = 0;
        switch (p.clazz) {
            case 0 -> {
                if (type == 9 || type == 2) {
                    perct += getPoint(1) * 20;
                    perct += getTotalItemParam(9);
                    if (getSkillPoint(12) > 0) {
                        for (Option op : p.skills[12].mLvSkill[getSkillPoint(12) - 1].minfo) {
                            if (op.id == 9) {
                                perct += op.getParam(0);
                                break;
                            }
                        }
                    }
                }
                EffTemplate eff = getEffectDefault(StrucEff.BuffSTLua);
                if (eff != null) {
                    perct += eff.param;
                }
            }
            case 1 -> {
                if (type == 11 || type == 4) {
                    perct += getPoint(1) * 20;
                    perct += getTotalItemParam(11);
                    if (getSkillPoint(12) > 0) {
                        for (Option op : p.skills[12].mLvSkill[getSkillPoint(12) - 1].minfo) {
                            if (op.id == 11) {
                                perct += op.getParam(0);
                                break;
                            }
                        }
                    }
                }
                EffTemplate eff = getEffectDefault(StrucEff.BuffSTDoc);
                if (eff != null) {
                    perct += eff.param;
                }
            }
            case 2 -> {
                if (type == 8 || type == 1) {
                    perct += getPoint(1) * 20 + getPoint(4) * 18;
                    perct += getTotalItemParam(8);
                    if (getSkillPoint(12) > 0) {
                        for (Option op : p.skills[12].mLvSkill[getSkillPoint(12) - 1].minfo) {
                            if (op.id == 8) {
                                perct += op.getParam(0);
                                break;
                            }
                        }
                    }
                }
                EffTemplate eff = getEffectDefault(StrucEff.BuffSTBang);
                if (eff != null) {
                    perct += eff.param;
                }
            }
            case 3 -> {
                if (type == 10 || type == 3) {
                    perct += getPoint(1) * 20 + getPoint(4) * 18;
                    perct += getTotalItemParam(10);
                    if (getSkillPoint(12) > 0) {
                        for (Option op : p.skills[12].mLvSkill[getSkillPoint(12) - 1].minfo) {
                            if (op.id == 10) {
                                perct += op.getParam(0);
                                break;
                            }
                        }
                    }
                }
                EffTemplate eff = getEffectDefault(StrucEff.BuffSTDien);
                if (eff != null) {
                    perct += eff.param;
                }
            }
        }
        return perct;
    }

    @Override
    public int getBaseDamage() {
        long base = getStatInfo(40);
        // Tambahkan DAMAGE_BONUS (id 48) dari pet yang sedang follow
        for (Pet temp : p.mypet) {
            if (temp.is_follow) {
                for (PetOption op : temp.op) {
                    if (op.id == 48) {
                        base += op.value;
                        break;
                    }
                }
                break;
            }
        }
        // FIX: getDameProp() sudah di-cap 2 milyar sebelum di-cast ke int,
        // tapi getBaseDamage() dulu enggak — stat damage yang gede banget
        // (build/gear ekstrem) bisa overflow int lalu wrap jadi NEGATIF.
        // Damage negatif ini kebawa ke MainObject.attack() -> expup jadi
        // negatif -> "expup > 0" di kalkulasi exp selalu false -> player
        // dapat 0xp walau damage-nya keliatan gede. Cap di sini biar
        // konsisten sama getDameProp() dan enggak overflow.
        if (base > 2_000_000_000L) {
            base = 2_000_000_000L;
        }
        return (int) base;
    }

    @Override
    public int getDameProp(int type) {
        // getTotalItemParam sudah otomatis baca pet option (id 0-4 dll)
        // karena override di atas, tidak perlu loop pet manual lagi
        if (type == 0) {
            long dame = getTotalItemParam(0); // termasuk pet id=0 (PHYSICAL_DMG)
            switch (p.clazz) {
                case 0, 1 -> dame += getPoint(1) * 4L;
                case 2, 3 -> dame += getPoint(4) * 4L;
            }
            dame += ((dame * (getPercentDameProp(7) / 100)) / 100);
            // Bonus Combo dari medal (op.id = 116), nilai dalam format persen*100 (misal 900 = 9%)
            int comboPercent = getTotalItemParam(116);
            if (comboPercent > 0) {
                dame += (dame * comboPercent) / 10000;
            }
            if (dame > 2_000_000_000) {
                dame = 2_000_000_000;
            }
            return (int) dame;
        }
        long dprop = 0;
        switch (p.clazz) {
            case 0: {
                if (type == 2) {
                    dprop += getPoint(1) * 4L;
                    dprop += getTotalItemParam(2); // termasuk pet id=2 (FIRE_DMG)
                }
                break;
            }
            case 1: {
                if (type == 4) {
                    dprop += getPoint(1) * 4L;
                    dprop += getTotalItemParam(4); // termasuk pet id=4 (POISON_DMG)
                }
                break;
            }
            case 2: {
                if (type == 1) {
                    dprop += getPoint(4) * 4L;
                    dprop += getTotalItemParam(1); // termasuk pet id=1 (ICE_DMG)
                }
                break;
            }
            case 3: {
                if (type == 3) {
                    dprop += getPoint(4) * 4L;
                    dprop += getTotalItemParam(3); // termasuk pet id=3 (LIGHTNING_DMG)
                }
                break;
            }
        }

        // ── Konfigurasi DB ────────────────────────────────────────────
        dprop += ((dprop * (getPercentDameProp(type) / 100)) / 100);
        // Bonus Combo dari medal (op.id = 116), nilai dalam format persen*100 (misal 900 = 9%)
        int comboPercent = getTotalItemParam(116);
        if (comboPercent > 0) {
            dprop += (dprop * comboPercent) / 10000;
        }
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
        // id 89 (ALL_RESISTANCE) dari pet berlaku untuk semua tipe resistansi elemen (16-22)
        if (type >= 16 && type <= 22) {
            param += getTotalItemParam(89);
        }
        EffTemplate ef = p.getEffectDefault(4);
        if (ef != null) {
            param -= 1000;
        }
        if (param < 0) {
            param = 0;
        }
        return param;
    }

    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk) throws IOException {
        if (gameMap.mapId == 87) {
            CastleSiegeManager.PlayerDie(p);
        }
        p.dame_affect_special_sk = 0;
        p.hp = 0;
        p.isdie = true;

        // Bug fix: kematian di arena King Cup (map 102) sebelumnya tidak pernah
        // menambah countWin siapa pun, jadi hasil PK di arena tidak berpengaruh
        // ke pemenang ronde/pertandingan (lihat KingCup.update()).
        if (gameMap.mapId == 102 && gameMap.kingCupRef != null && (p.typepk == 12 || p.typepk == 13)) {
            KingCup match = gameMap.kingCupRef;
            Player winner = (p.typepk == 12)
                    ? GameMap.get_player_by_name(match.name2)
                    : GameMap.get_player_by_name(match.name1);
            if (winner != null) {
                winner.countWin++;
            }
            match.end_round(); // reset HP/posisi kedua pemain untuk ronde berikutnya (best of 3)
        }

        Player pATK = mainAtk.isPlayer() ? (Player) mainAtk : null;
        if (pATK != null) {
            if (pATK.list_enemies.contains(this.name)) {
                pATK.list_enemies.remove(this.name);
                MapService.SendChat(gameMap, pATK, "Take a bite :v", true);
            } else if (p.typepk == -1) {
                if (!p.list_enemies.contains(pATK.name)) {
                    p.list_enemies.add(pATK.name);
                    if (p.list_enemies.size() > 20) {
                        p.list_enemies.remove(0);
                    }
                }
                MapService.SendChat(gameMap, p, "Anak kecil ini, tunggu aku online, aku akan menendangmu tanpa meleset sekali ><", true);
            }
        }
    }

    @Override
    public int getTypeObject() {
        return 0;
    }

    @Override
    public void update(GameMap gameMap) {
        try {
            if (isdie) {
                return;
            }
            //<editor-fold defaultstate="collapsed" desc="auto +hp,mp       ...">  
            int hpRegen = p.body.getHpRegen();
            if (p.time_buff_hp < System.currentTimeMillis()) {
                p.time_buff_hp = System.currentTimeMillis() + 5000L;
                if (hpRegen > 0 && p.hp < p.body.getMaxHP()) {
                    Service.usepotion(p, 0, hpRegen);
                }
            }
            int mpRegen = p.body.getMpRegen();
            if (p.time_buff_mp < System.currentTimeMillis()) {
                p.time_buff_mp = System.currentTimeMillis() + 5000L;
                if (mpRegen > 0 && p.mp < p.body.getMaxMP()) {
                    Service.usepotion(p, 1, mpRegen);
                }
            }
            //</editor-fold>    auto +hp,mp

            //<editor-fold defaultstate="collapsed" desc="eff Player       ...">  
            if (getEffectMedal(StrucEff.BongLua) != null && !p.isdie) {
                Service.usepotion(p, 0, (int) -(p.hp * Util.random(5, 10) * 0.01));
//                p.hp -= (int) (p.hp * Util.random(5, 10) * 0.01);
                if (p.hp <= 0) {
                    p.hp = 1;
                }
//                Service.send_char_main_in4(p);
            }
            // BongPetir: sengatan listrik — kurangi 1% maxHP tiap tick (sisa minimal 1)
            if (getEffectMedal(StrucEff.BongPetir) != null && !p.isdie) {
                Service.usepotion(p, 0, -Math.max(1, p.body.getMaxHP() / 100));
                if (p.hp <= 0) {
                    p.hp = 1;
                }
            }
            if (getEffectMedal(StrucEff.BongLanh) != null) {
                Service.usepotion(p, 1, (int) -(p.mp * Util.random(5, 10) * 0.01));
//                p.mp -= (int) (p.mp * Util.random(5, 10) * 0.01);
//                Service.send_char_main_in4(p);
            }
            // FIX: BongDoc (racun DoT) — kurangi HP player tiap tick
            {
                EffTemplate dotEff = getEffectDefault(StrucEff.BongDoc);
                if (dotEff != null && !p.isdie) {
                    int dotDmg = Math.max(1, dotEff.param);
                    Service.usepotion(p, 0, -dotDmg);
                    if (p.hp <= 0) {
                        p.hp = 1;
                    }
                }
            }
            if (p.hp <= 0 && !p.isdie) {
                // FIX RACE CONDITION: dulu blok ini langsung menge-set isdie=true begitu
                // membaca hp<=0, tanpa lewat setDie()/Player_Die(). Kalau tick ini pas
                // nyelonong di tengah MainObject.attack() -- setelah "focus.hp -= dame"
                // tapi sebelum logika grace-period anti-oneshot sempat menyelamatkan HP
                // ke 5 -- isdie kepasang true padahal HP sudah/akan diselamatkan, dan
                // tidak ada yang pernah revive() pemain ini lagi (round-transition PvpSession
                // tidak pernah terpicu karena setDie() tidak pernah dipanggil). Efeknya
                // pemain kelihatan hidup tapi tidak bisa menyerang selamanya.
                // synchronized(p) di sini + baca ulang p.hp SETELAH lock didapat menutup
                // jendela race itu: kalau attack() sudah keburu menyelamatkan HP (thread
                // itu juga harus lewat lock yang sama sebelum re-check ini), p.hp di sini
                // sudah bukan <=0 lagi dan isdie tidak jadi di-set.
                synchronized (p) {
                    if (p.hp <= 0 && !p.isdie) {
                        p.hp = 0;
                        p.isdie = true;
                    }
                }
            }
            //</editor-fold>    eff Player
        } catch (Exception e) {
        }
    }
}