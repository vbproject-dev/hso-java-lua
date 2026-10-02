package client;

import java.util.ArrayList;
import java.util.List;

import game.pet.PetManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import core.Util;

import template.Level;
import template.PetData;
import template.PetOption;

public class Pet {


//    public static String[] name_template = new String[]{
//            "Burung Hantu",
//            "Kelelawar",
//            "Serigala",
//            "Elang",
//            "Monyet",
//            "Naga Api",
//            "Kelinci",
//            "Phoenix Es",
//            "Zabivaka",
//            "Hantu",
//            "Anak Kambing",
//            "Peri",
//            "Malaikat",
//            "Saola",
//            "Kucing"};
//
//    static short[] id_template = new short[]{2944, 2943, 2939, 3269, 3616, 4614, 4622, 4626, 4631, 4699, 4708, 4761, 4762, 4768, 4788};

    public List<PetOption> op;
    public String name;
    public short level;
    public byte type;
    public byte spriteImage;
    public byte nframe;
    public byte color;
    public short grown;
    public short maxgrown;
    public short point1;
    public short point2;
    public short point3;
    public short point4;
    public short maxpoint;
    public boolean is_follow;
    public boolean is_hatch;
    public long exp;
    public long time_born;
    public long time_eat;
    public long expiry_date;
    public PetData template;

    public void setup(JSONArray js) {

        level = Short.parseShort(js.get(0).toString());
        type = Byte.parseByte(js.get(1).toString());
        spriteImage = Byte.parseByte(js.get(2).toString());
        nframe = Byte.parseByte(js.get(3).toString());
        color = Byte.parseByte(js.get(4).toString());
        grown = Short.parseShort(js.get(5).toString());
        maxgrown = Short.parseShort(js.get(6).toString());
        point1 = Short.parseShort(js.get(7).toString());
        point2 = Short.parseShort(js.get(8).toString());
        point3 = Short.parseShort(js.get(9).toString());
        point4 = Short.parseShort(js.get(10).toString());
        maxpoint = Short.parseShort(js.get(11).toString());
        exp = Long.parseLong(js.get(12).toString());
        is_follow = Byte.parseByte(js.get(13).toString()) == 1;
        is_hatch = Byte.parseByte(js.get(14).toString()) == 1;
        time_born = Long.parseLong(js.get(15).toString());
        op = new ArrayList<>();
        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(16).toString());
        for (Object o : js2) {
            JSONArray js3 = (JSONArray) JSONValue.parse(o.toString());
            PetOption temp = new PetOption(Byte.parseByte(js3.get(0).toString()),
                    Integer.parseInt(js3.get(1).toString()), Integer.parseInt(js3.get(2).toString()));
            op.add(temp);
        }
        if (js.size() >= 18)
            expiry_date = Long.parseLong(js.get(17).toString());


        PetData petData = PetManager.getInstance().getPetByImageId(spriteImage);
        if (petData != null) {
            template = petData;
            name = template.getName();
        }
        if (name == null || name.isEmpty()) {
            name = "Pet";
        }
    }

    public int getLevelPercent() {
        return (int) ((exp * 1000) / Level.entrys.get(level - 1).exp);
    }

    public static Pet getPet(short id, long hsd) {

        PetData petData = PetManager.getInstance().getPetById(id);
        if (petData == null) {
            return null;
        }

        Pet temp = new Pet();
        temp.template = petData;
        temp.name = petData.getName();
        temp.level = 1;
        temp.nframe = 3;
        temp.color = 0;
        temp.grown = 0;
        temp.maxgrown = 300;
        temp.point1 = 0;
        temp.point2 = 0;
        temp.point3 = 0;
        temp.point4 = 0;
        temp.maxpoint = 10_000;
        temp.exp = 0;
        temp.is_follow = false;
        temp.is_hatch = true;
        temp.time_born = System.currentTimeMillis() + 1000L * 60 * 10;
        temp.op = new ArrayList<>();
        temp.expiry_date = hsd;
//        short[] id_ = new short[]{23, 24, 25, 26};
//        int[] param_ = new int[]{1, 1, 1, 1};
//        int[] maxdam_ = new int[]{0, 0, 0, 0};
//
//        switch (id) {
//            case 2943: { // [0,1,2] KELALAWAR
//                temp.spriteImage = 0;
//                temp.type = 1;
//                id_ = new short[]{23, 24, 25, 26, 47, 4};
//                param_ = new int[]{1, 1, 1, 1, 400, 1000};
//                maxdam_ = new int[]{0, 0, 0, 0, 500, 2000};
//                break;
//            }
//            case 2944: { // [3,4,5] BURUNG HANTU
//                temp.spriteImage = 3;
//                temp.type = 0;
//                id_ = new short[]{23, 24, 25, 26, 44, 45, 1};
//                param_ = new int[]{1, 1, 1, 1, 200, 2000, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 300, 5000, 20000};
//                break;
//            }
//            case 2939: { // [6,7,8] SRIGALA
//                temp.spriteImage = 6;
//                temp.type = 2;
//                id_ = new short[]{23, 24, 25, 26, 49, 48, 0};
//                param_ = new int[]{1, 1, 1, 1, 100, 15000, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 100, 45000, 20000};
//                break;
//            }
//            case 3269: {// [9,10,11] EAGLE
//                temp.spriteImage = 9;
//                temp.type = 3;
//                id_ = new short[]{23, 24, 25, 26, 46, 48, 3};
//                param_ = new int[]{1, 1, 1, 1, 10000, 1500, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 20000, 4500, 20000};
//                break;
//            }
//            case 3616: { // [12,13,14] MONYET
//                temp.spriteImage = 12;
//                temp.type = 4;
//                id_ = new short[]{23, 24, 25, 26, 48, 49, 2};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 100, 20000};
//                break;
//            }
//            case 4617: { // [15,16,17] NAGA API
//                temp.spriteImage = 15;
//                temp.type = 5;
//                id_ = new short[]{23, 24, 25, 26, 48, 97, 98, 2};
//                param_ = new int[]{1, 1, 1, 1, 1500, 1300, 2000, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 2300, 4500, 20000};
//                break;
//            }
//            case 4622: { // [18,19,20] KELINCI
//                temp.spriteImage = 18;
//                temp.type = 6;
//                id_ = new short[]{23, 24, 25, 26, 67, 113, 114, 1};
//                param_ = new int[]{1, 1, 1, 1, 1350, 300, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 2350, 500, 3, 20000};
//                break;
//            }
//            case 4626: {// [21, 22, 23] PHOENIX ES
//                temp.spriteImage = 21;
//                temp.type = 7;
//                id_ = new short[]{23, 24, 25, 26, 67, 113, 114, 1};
//                param_ = new int[]{1, 1, 1, 1, 1350, 300, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 2350, 500, 3, 20000};
//                break;
//            }
//            case 4631: {// [24,25,26] Zabawiaka
//                temp.spriteImage = 24;
//                temp.type = 8;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 85, 86, 114, 0};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 100, 1000, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 150, 1500, 3, 20000};
//                break;
//            }
//            case 4699: {// [27,28,29] HANTU
//                temp.spriteImage = 27;
//                temp.type = 9;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 85, 86, 114, 1};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 100, 1000, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 150, 1500, 3, 20000};
//                break;
//            }
//            case 4761: {// [33,34,35] PERI
//                temp.spriteImage = 33;
//                temp.type = 11;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 114, 2};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 3, 20000};
//                break;
//            }
//            case 4762: {// [36,37,38] MALAIKAT
//                temp.spriteImage = 36;
//                temp.type = 12;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 114, 3};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 3, 20000};
//                break;
//            }
//            case 4768: {// [39,40,41] SAOLA
//                temp.spriteImage = 39;
//                temp.type = 13;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 85, 86, 114, 1};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 100, 1000, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 150, 1500, 3, 20000};
//                break;
//            }
//            case 4708: // [30,31,32] ANAK KAMBING
//            {
//                temp.spriteImage = 30;
//                temp.type = 10;
//                id_ = new short[]{23, 24, 25, 26, 159};
//                param_ = new int[]{1, 1, 1, 1, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 10000};
//                break;
//            }
//            case 4788: {// [42,43,44] TELUR KUCING PENYIHIR
//                temp.spriteImage = 42;
//                temp.type = 14;
//                id_ = new short[]{23, 24, 25, 26, 48, 80, 85, 86, 114, 1};
//                param_ = new int[]{1, 1, 1, 1, 1500, 100, 100, 1000, 2, 10000};
//                maxdam_ = new int[]{0, 0, 0, 0, 4500, 200, 150, 1500, 3, 20000};
//                break;
//            }
//
//            default: {
//                return null;
//            }
//        }
//
//        for (int i = 0; i < id_.length; i++) {
//
//            PetOption op = new PetOption(id_[i], param_[i], maxdam_[i]);
//            temp.op.add(op);
//        }
        temp.spriteImage = petData.getImage().get(0);
        temp.type = (byte) petData.getTypeMove();
        temp.op = petData.getOptions();
        return temp;
    }

    public short getId() {
        return (short) template.getId();
    }



    public void update_exp(int i) {
        if (i <= 0) {
            return;
        }
        exp += i;
        for (PetOption o : op) {
            switch (o.id) {
                case 23:
                    o.value = (int) (point1 / 78);
                    break;
                case 24:
                    o.value = (int) (point2 / 78);
                    break;
                case 25:
                    o.value = (int) (point3 / 78);
                    break;
                case 26:
                    o.value = (int) (point4 / 78);
                    break;
            }
        }
        if ((this.level == 9 || this.level == 19 || this.level == 30) && exp >= Level.entrys.get(level - 1).exp) {
            exp = Level.entrys.get(level - 1).exp - 1;
        } else {
            while (exp >= Level.entrys.get(level - 1).exp) {
                exp -= Level.entrys.get(level - 1).exp;
                UpgradeLevel();
//                level++;
//                for (int j = 0; j < op.size(); j++) {
//                    if (op.get(j).id >= 23 && op.get(j).id <= 26) {
//                        op.get(j).param += Util.random(0, 5);
//                    } else {
//                        int par_plus = Util.random(50, 150);
//                        op.get(j).param += par_plus;
//                        op.get(j).maxdam += par_plus;
//                    }
//                }
            }
        }
    }

    public void UpgradeLevel() {
        level++;
        for (PetOption o : op) {
            if (o.id == 23)
                o.value = (int) (point1 / 78);
            else if (o.id == 24)
                o.value = (int) (point2 / 78);
            else if (o.id == 25)
                o.value = (int) (point3 / 78);
            else if (o.id == 26)
                o.value = (int) (point4 / 78);
            else if (o.id != 159) {
                int r = Util.random(50, 150);
                o.value = Math.min(o.value + r, o.maxValue);
            }

        }
//        for (int j = 0; j < op.size(); j++) {
//            
//            if (op.get(j).id >= 23 && op.get(j).id <= 26) {
//                //op.get(j).param += Util.random(0, 5);
//                op.get(j).param = (int)(level * 4.5);
//            } else {
//                int par_plus = Util.random(50, 150);
//                op.get(j).param += par_plus;
//                op.get(j).maxdam += par_plus;
//            }
//        }
    }

    public int get_age() {
        long age = System.currentTimeMillis() - time_born;
        age /= 3_600_000;
        if (age < 0) {
            age = 0;
        } else if (age > Integer.MAX_VALUE) {
            age = Integer.MAX_VALUE;
        }
        return (int) age;
    }



    public boolean can_revolution() {
        return (this.exp >= (Level.entrys.get(this.level - 1).exp - 1)) && (this.level == 9 || this.level == 19);
    }

    public void update_grown(long t) {
        this.grown -= (short) t;
        if (this.grown < 0) {
            this.grown = 0;
        }
    }
//
//    public void PetAttack(Player p, MainObject focus) throws IOException {
//        if (!focus.isdie && this.grown > 0) {
//            int a1 = 0;
//            int a2 = 1;
//            for (PetOption temp : this.op) {
//                if (temp.maxdam > 0) {
//                    a1 = temp.param;
//                    a2 = temp.maxdam;
//                    break;
//                }
//            }
//            int dame_pet = Util.random(a1, Math.max((a2 + 1), (a1 + 1))) - (focus.level * 15);
//            if (dame_pet <= 0) {
//                dame_pet = 1;
//            }
//            if (((focus.hp - dame_pet) > 0) && (p.pet_atk_speed < System.currentTimeMillis()) && (a2 > 1)) {
//                if (this.getId() == 3269 || this.name.equals("Đại Bàng")) {
//                    int vangjoin = Util.random(1666, 2292);
//                    p.updateGold(vangjoin);
//                    Service.send_notice_nobox_white(p.conn, "+ " + vangjoin + " emas");
//                }
//                p.pet_atk_speed = System.currentTimeMillis() + 1500L;
//                Message m = new Message(84);
//                m.writer().writeByte(2);
//                m.writer().writeShort(p.index);
//                m.writer().writeByte(1);
//                m.writer().writeByte(1);
//                m.writer().writeShort(focus.index);
//                m.writer().writeInt(dame_pet);
//                focus.hp -= dame_pet;
//                m.writer().writeInt(focus.hp);
//                m.writer().writeInt(p.hp);
//                m.writer().writeInt(p.body.getMaxHP());
//                p.conn.addmsg(m);
//                m.cleanup();
//            }
//        }
//    }
}