package template;

import equipment.EnumOption;
import equipment.StatEntry;
import lombok.Data;

@Data
public class Option {

    private static final int[] parafterupdate
            = new int[]{1, 110, 120, 130, 140, 150, 160, 172, 184, 196, 208, 220, 235, 250, 265, 280};
    public byte id;
    private int param;
    private short idItem;

    public Option(int id, int param, short iditem) {
        this.id = (byte) id;
        this.param = param;
        this.idItem = iditem;
    }


    public Option(int id, int param) {
        this.id = (byte) id;
        this.param = param;
    }

    public int getParam(int tier) {


        if ((id >= 100 && id <= 107)||(id >= 58 && id<=60)) {
            return param;
        }
        if (utils.CheckItem.isMedal(idItem) || utils.CheckItem.isMedalV2(idItem)) {
            return getParamMD(tier);
        }

        if (tier == 0) {
            return param;
        }
        //


        //if (this.id >= 29 && this.id <= 36 || this.id >= 16 && this.id <= 22 || this.id == 41) {
        //    parbuffer += 50 * tier;
        //    return parbuffer;
        //}
        int parbuffer = this.param;
        byte[] optIds = {16,17,18,19,20,21,22,29,30,31,32,33,34,35,36,41}; // this increase 0.2% per tier
        for (byte opt : optIds) {
            if (opt == id) {
                int bonus = 20 * tier; // 20 = 0.2% in your integer system
                return parbuffer + bonus; // 0.2% per tier

            }
        }

        // POTENTIAL POINTS
        if (this.id >= 23 && this.id <= 26) {
            return (parbuffer + tier);
        }

        // DURASI KEMARAHAN
        if (this.id == 42) {
            return (parbuffer + tier * 400);
        }
        // ELEMENTAL DAMAGE PERCENT, DEF, HP, MANA
        if ((this.id >= 7 && this.id <= 13) || this.id == 15 || this.id == 27 || this.id == 28) {
            return (parbuffer + 100 * tier);
        }

        // SKILL PTS
        if ((this.id == 37 || this.id == 38) && tier < 9) {
            return 1;
        }

        if (tier > 15) {
            tier = 15;
        }

        if ((this.id >= 0 && this.id <= 6) || this.id == 14 || this.id == 40) {
            parbuffer = (parafterupdate[tier] * this.param) / 100;
            return parbuffer;
        }
        return parbuffer;
    }

    public int getParamMD(int tier) {

        if (tier == 0) {
            return param;
        }
        if ((this.id == 37 || this.id == 38)) {
            return 1;
        }
        //
        int parbuffer = this.param;
        if (this.id >= 0 && this.id <= 6) {
            return (parbuffer + ((int) (parbuffer * tier * 0.33)));
        }

        if (this.id == 81 || this.id == 86 || this.id == 88 || this.id == 77 || this.id == 79) // giây dòng vip
        {
            return (int) (parbuffer * tier * 0.5);
        }
        if (this.id == 85 || this.id == 87 || this.id == 80 || this.id == 82) // dòng vip
        {
            return (int) (parbuffer * tier * 0.07);
        }
        if (this.id == 78 || this.id == 76) // dòng vip
        {
            return (int) (parbuffer * tier * 0.1);
        }

        if ((this.id >= 76 && this.id <= 89) || this.id == 97 || this.id == 98 || this.id == 95) // dòng vip
        {
            return (int) (parbuffer * tier * 0.07);
        }
        if (this.id >= 29 && this.id <= 36 || this.id >= 16 && this.id <= 22 || this.id == 41) {
            parbuffer += 50 * tier;
            return parbuffer;
        }

        if (this.id >= 23 && this.id <= 26) {
            return (parbuffer + tier);
        }
        if (this.id == 42) {
            return (parbuffer + tier * 400);
        }
        // EXP BONUS - naik 1% per tier
        if (this.id == 51) {
            return (parbuffer + tier);
        }
        if ((this.id >= 7 && this.id <= 13)) {
            double multiplier = 1.0 + (tier * 0.6);
            return (this.param * (int) multiplier);
        }
        if (this.id == 15 || this.id == 27 || this.id == 28) {
            return (parbuffer + 100 * tier);
        }

        if (tier > 15) {
            tier = 15;
        }
        if ((this.id >= 0 && this.id <= 6) || this.id == 14 || this.id == 40) {
            parbuffer = (parafterupdate[tier] * this.param) / 100;
            return parbuffer;
        }
        return parbuffer;
    }

    public void setParam(int param) {
        this.param = param;
    }

    public StatEntry toEntry() {
        return new StatEntry(EnumOption.fromId(id), param);
    }

}