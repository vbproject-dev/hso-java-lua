package game.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import client.Pet;
import client.Player;
import core.Service;
import core.Util;
import client.io.Message;
import client.io.Session;
import model.map.MapData;
import template.EffTemplate;
import template.ItemTemplate3;
import template.ItemTemplate7;
import template.MobTemplate;
import template.Mob_Dungeon;
import template.Option;
import template.PetOption;

public class Dungeon {

    public static List<BXH_Dungeon_Finished> bxh_time_complete = new ArrayList<>();
    public GameMap template;
    public long time_live;
    public int state;
    private int mob_speed;
    public int wave;
    public int num_mob;
    private int num_mob_max;
    private int dame_buff;
    private int id_mob_in_wave;
    public int index_mob;
    private int hp;
    public String name_party;
    private byte mode_now;
    private List<Mob_Dungeon> mobs;

    public Dungeon() throws IOException {
        init();
    }

    private void init() throws IOException {
        MapData mapData = MapManager.getInstance().getMapData(48);
        GameMap temp = GameMap.get_map_dungeon(48);
        template = new GameMap(48, 0, mapData);
        template.mobs = new MobInMap[0];
        template.start_map();
        template.d = this;
        time_live = System.currentTimeMillis();
        // index_mob = Manager.gI().size_mob;
        index_mob = 1;
        state = -1;
        wave = 1;
        mobs = new ArrayList<>();
        // num_boss = 0;
    }

    public void finish_dungeon() {
    }

    public void update() {
        if (this.index_mob > 65_000) {
            this.state = 7;
        }
        int time_now = getTime_live();
        try {
            if (this.state == -1) {
                if (time_now == 4) {
                    this.state = 0;
                    notice_all_player_in_dungeon("Babak 1 akan dimulai dalam 10 detik");
                }
            } else if (this.state == 0) { // wait to begin
                if (time_now == 13) {
                    this.state = 1;
                }
            } else if (this.state == 1) { // wait at wave 20
                if (wave != 21) {
                    int dem = 0;
                    if (mobs.size() > 0) {
                        for (Mob_Dungeon mob_temp : mobs) {
                            for (Player p0 : template.players) {
                                Message m2 = new Message(17);
                                m2.writer().writeShort(p0.objectId);
                                m2.writer().writeShort(mob_temp.objectId);
                                p0.conn.addmsg(m2);
                                m2.cleanup();
                                dem++;
                                if (dem > 10_000) {
                                    break;
                                }
                            }
                            if (dem > 10_000) {
                                break;
                            }
                        }
                    }
                    this.state = 2;
                    notice_all_player_in_dungeon("Babak " + (wave++) + " dimulai");
                    this.num_mob = num_mob_max;
                    mobs.clear();
                    if (wave <= 198) {
                        id_mob_in_wave = wave - 1;
                    } else {
                        id_mob_in_wave = Util.random(0, 198);
                    }
                } else {
                    // leave_item_by_type3(template,)
                    notice_all_player_in_dungeon(
                            "Berhasil melewati, selamat para ksatria, akan keluar otomatis dalam 30 detik");
                    this.num_mob = num_mob_max;
                    mobs.clear();
                    this.state = 8;
                    // cal prime
                    synchronized (Dungeon.bxh_time_complete) {
                        BXH_Dungeon_Finished bxh = new BXH_Dungeon_Finished(this.name_party, this.getTime_live());
                        Dungeon.bxh_time_complete.add(bxh);
                        //
                        List<BXH_Dungeon_Finished> buffer = new ArrayList<>();
                        List<Integer> list_sorted = new ArrayList<>();
                        for (BXH_Dungeon_Finished temp : Dungeon.bxh_time_complete) {
                            list_sorted.add(Integer.valueOf(temp.time));
                        }
                        Collections.sort(list_sorted);
                        Set<String> check = new HashSet<>();
                        for (int i = 0; i < list_sorted.size(); i++) {
                            for (int i1 = 0; i1 < Dungeon.bxh_time_complete.size(); i1++) {
                                BXH_Dungeon_Finished temp = Dungeon.bxh_time_complete.get(i1);
                                if (temp.time == list_sorted.get(i)) {
                                    if (!check.contains(temp.name)) {
                                        buffer.add(temp);
                                        check.add(temp.name);
                                    }
                                    Dungeon.bxh_time_complete.remove(temp);
                                    break;
                                }
                            }
                            if (buffer.size() > 9) {
                                break;
                            }
                        }
                        Dungeon.bxh_time_complete.clear();
                        Dungeon.bxh_time_complete.addAll(buffer);
                        buffer.clear();
                        check.clear();
                    }
                }
            } else if (this.state == 2) { // fight
                if (mobs.size() < num_mob_max) {
                    Mob_Dungeon mob = new Mob_Dungeon(this, this.index_mob++, MobTemplate.entrys.get(id_mob_in_wave));
                    mob.x = 1308;
                    mob.y = (short) (672 + Util.random(-25, 25));
                    mob.from_gate = 2;
                    if (this.wave >= 21) {
                        mob.setMaxHP(mob.template.hpmax * (this.wave / 10));
                    } else {
                        mob.setMaxHP(mob.template.hpmax);
                    }
                    mob.hp = mob.getMaxHP();
                    mobs.add(mob);
                    Message m = new Message(4);
                    m.writer().writeByte(1);
                    m.writer().writeShort(mob.template.mob_id);
                    m.writer().writeShort(mob.objectId); // index
                    m.writer().writeShort(mob.x);
                    m.writer().writeShort(mob.y);
                    m.writer().writeByte(-1);
                    for (int i = 0; i < template.players.size(); i++) {
                        Player p0 = template.players.get(i);
                        p0.conn.addmsg(m);
                    }
                    m.cleanup();
                }
                update_mob();
            } else if (this.state == 4) {
                // this.state = 6;
            } else if (this.state == 5) {
                for (int i = 0; i < mobs.size(); i++) {
                    Mob_Dungeon mob = mobs.get(i);
                    Message m2 = new Message(17);
                    m2.writer().writeShort(-1);
                    m2.writer().writeShort(mob.objectId);
                    for (int i2 = 0; i2 < template.players.size(); i2++) {
                        Player p0 = template.players.get(i2);
                        p0.conn.addmsg(m2);
                    }
                    m2.cleanup();
                }
                mobs.clear();
                for (int i2 = 0; i2 < template.players.size(); i2++) {
                    Player p0 = template.players.get(i2);
                    Service.send_notice_box(p0.conn,
                            "Gagal:\n Hasil : waktu melewati rintangan : " + getTime_live()
                                    + " detik\nKeluar otomatis dalam 5 detik");
                }
                notice_all_player_in_dungeon("Thiên thạch đã vỡ, vượt phó bản thất bại!");
                this.state = 6;
            } else if (this.state == 6) {
                this.state = 7;
                //
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (this.state == 7) {
                for (int i = 0; i < template.players.size(); i++) {
                    Player p0 = template.players.get(i);
                    p0.x = 432;
                    p0.y = 354;
                    GameMap[] gameMap_enter = GameMap.getMapById(1);
                    int d = 0;
                    while ((d < (gameMap_enter[d].maxzone - 1)) && gameMap_enter[d].players.size() >= gameMap_enter[d].maxplayer) {
                        d++;
                    }
                    p0.map = gameMap_enter[d];
                    //
                    p0.isChangemap = false;
                    p0.xOld = p0.x;
                    p0.yOld = p0.y;
                    MapService.enter(p0.map, p0);
                    //
                    // p0.dungeon = null;
                }
                this.template.stop_map();
                this.template.d = null;
                DungeonManager.remove_list(this);
            } else if (this.state >= 8 && this.state <= 13) {
                if (this.state == 13) {
                    this.state = -2;
                } else {
                    this.state++;
                }
            } else if (this.state == -2) {
                this.state = 7;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notice_all_player_in_dungeon(String s) throws IOException {
        Message m = new Message(53);
        m.writer().writeUTF(s);
        m.writer().writeByte(0);
        for (int i = 0; i < template.players.size(); i++) {
            Player p0 = template.players.get(i);
            p0.conn.addmsg(m);
        }
        m.cleanup();
    }

    private void update_mob() throws IOException {
        for (int i = 0; i < mobs.size(); i++) {
            Mob_Dungeon mob = mobs.get(i);
            if (!mob.isdie) {
                mob_act(mob);
            }
        }
    }

    private void mob_act(Mob_Dungeon mob) throws IOException {
        int distance = Util.random(1, mob_speed);
        boolean is_atk = true;
        switch (mob.from_gate) {
            case 1: {
                if (!(Math.abs((mob.y + distance) - 672) < mob_speed)) {
                    mob.y += distance;
                    is_atk = false;
                }
                break;
            }
            case 2: {
                if (!(Math.abs((mob.x - distance) - 684) < mob_speed)) {
                    mob.x -= distance;
                    is_atk = false;
                }
                break;
            }
            case 3: {
                if (!(Math.abs((mob.y - distance) - 672) < mob_speed)) {
                    mob.y -= distance;
                    is_atk = false;
                }
                break;
            }
            case 4: {
                if (!(Math.abs((mob.x + distance) - 684) < mob_speed)) {
                    mob.x += distance;
                    is_atk = false;
                }
                break;
            }
        }
        if (is_atk) {
            int dame = (mob.getMaxHP() / 100) * (mob.template.level / 2);
            dame = Util.random((dame * 9) / 10, (dame + 1));
            dame += this.wave * 10;
            dame *= dame_buff;
            Message m = new Message(10);
            m.writer().writeByte(1);
            m.writer().writeShort(mob.objectId);
            m.writer().writeInt(mob.hp);
            m.writer().writeByte(2);
            m.writer().writeByte(1);
            m.writer().writeShort(32001); // index thien thach
            m.writer().writeInt(dame); // dame mob
            this.hp -= dame;
            if (this.hp <= 0) {
                this.hp = 0;
                this.state = 5;
            }
            m.writer().writeInt(this.hp);
            m.writer().writeByte(6); // id skill mob
            m.writer().writeByte(0);
            for (int i = 0; i < template.players.size(); i++) {
                Player p0 = template.players.get(i);
                p0.conn.addmsg(m);
            }
            m.cleanup();
        } else {
            Message m = new Message(4);
            m.writer().writeByte(1);
            m.writer().writeShort(mob.template.mob_id);
            m.writer().writeShort(mob.objectId); // index
            m.writer().writeShort(mob.x);
            m.writer().writeShort(mob.y);
            m.writer().writeByte(-1);
            for (int i = 0; i < template.players.size(); i++) {
                Player p0 = template.players.get(i);
                p0.conn.addmsg(m);
            }
            m.cleanup();
        }
    }

    public int getTime_live() {
        return (int) ((System.currentTimeMillis() - time_live + 2) / 1000);
    }

    public void send_map_data(Player p) throws IOException {
        // thien thach
        Message m = new Message(4);
        m.writer().writeByte(2);
        m.writer().writeShort(0);
        m.writer().writeShort(32001); // index
        m.writer().writeShort(684);
        m.writer().writeShort(672);
        m.writer().writeByte(-1);
        p.conn.addmsg(m);
        m.cleanup();
    }

    public void send_in4_npc(Session conn, short type) throws IOException {
        if (type == 32001) {
            Message m = new Message(-44);
            m.writer().writeShort(32001); // index
            m.writer().writeUTF("Batu Meteor");
            m.writer().writeInt(this.hp);
            m.writer().writeInt(this.hp);
            m.writer().writeShort(0);
            m.writer().writeShort(684);
            m.writer().writeShort(672);
            m.writer().writeByte(2);
            m.writer().writeByte(2);
            m.writer().writeUTF("");
            m.writer().writeShort(25);
            m.writer().writeByte(1);
            m.writer().writeShort(17);
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public void send_mob_in4(Session conn, int index) throws IOException {
        int index_ = -1;
        for (int i = 0; i < mobs.size(); i++) {
            Mob_Dungeon mob = mobs.get(i);
            if (mob.objectId == index) {
                index_ = i;
                break;
            }
        }
        if (index_ > -1) {
            Mob_Dungeon temp = mobs.get(index_);
            Message m = new Message(7);
            m.writer().writeShort(index);
            m.writer().writeByte(temp.template.level);
            m.writer().writeShort(temp.x);
            m.writer().writeShort(temp.y);
            m.writer().writeInt(temp.hp);
            m.writer().writeInt(temp.getMaxHP());
            m.writer().writeByte(20); // id skill monster (Spec: 32, ...)
            m.writer().writeInt(-4);
            m.writer().writeShort(temp.x);
            m.writer().writeShort(temp.y);
            m.writer().writeShort(-1); // clan monster
            m.writer().writeByte(1);
            m.writer().writeByte(this.mob_speed / 25); // speed
            m.writer().writeByte(0);
            m.writer().writeUTF("");
            m.writer().writeLong(-11111);
            m.writer().writeByte(0); // color name 1: blue, 2: yellow
            conn.addmsg(m);
            m.cleanup();
        }
    }

    public class BXH_Dungeon_Finished {

        public String name;
        public int time;

        public BXH_Dungeon_Finished(String name, int time_live) {
            this.name = name;
            this.time = time_live;
        }
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int i) {
        wave = i;
    }

    public void setMode(int mode) throws IOException {
        String name = "";
        this.mode_now = (byte) mode;
        switch (mode) {
            case 0: {
                hp = 50_000;
                mob_speed = 50;
                num_mob_max = 20;
                num_mob = num_mob_max;
                dame_buff = 1;
                name = "Easy";
                break;
            }
            case 1: {
                hp = 50_000;
                mob_speed = 75;
                num_mob_max = 30;
                num_mob = num_mob_max;
                dame_buff = 2;
                name = "Normal";
                break;
            }
            case 2: {
                hp = 50_000;
                mob_speed = 100;
                num_mob_max = 50;
                num_mob = num_mob_max;
                dame_buff = 4;
                name = "Hard";
                break;
            }
            case 3: {
                hp = 50_000;
                mob_speed = 150;
                num_mob_max = 75;
                num_mob = num_mob_max;
                dame_buff = 8;
                name = "Nightmare";
                break;
            }
            case 4: {
                hp = 50_000;
                mob_speed = 200;
                num_mob_max = 100;
                num_mob = num_mob_max;
                dame_buff = 16;
                name = "Hell";
                break;
            }
        }
        if (this.wave == 20) {
            notice_all_player_in_dungeon("Đội trưởng đã chọn độ khó " + name);
        }
    }

    public Mob_Dungeon get_mob(int id) {
        for (int i = 0; i < this.mobs.size(); i++) {
            if (this.mobs.get(i).objectId == id) {
                return this.mobs.get(i);
            }
        }
        return null;
    }

    public synchronized void fire_mob(GameMap gameMap, Mob_Dungeon mob, Player p, byte index_skill, long dame_atk)
            throws IOException {
        if (mob.isdie) {
            Message m2 = new Message(17);
            m2.writer().writeShort(p.objectId);
            m2.writer().writeShort(mob.objectId);
            p.conn.addmsg(m2);
            m2.cleanup();
            return;
        }
        //

        dame_atk -= ((dame_atk * (Util.random(10))) / 100);
        long dame = dame_atk;
        //
        int cr = p.body.getCrit();
        boolean crit = cr > Util.random(0, 10000);
        boolean pierce = false;
        if (crit) {
            dame *= 2;
        }
        if (!crit) {
            int pier = p.body.getPierce();
            if (pier > Util.random(0, 10000)) {
                pierce = true;
            }
        }
        if (!pierce) {
            long dameresist = (this.wave * this.wave * this.wave / 2);
            // if (mob.is_boss) {
            // dameresist *= 2;
            // }
            dame -= dameresist;
            if (dame <= 0) {
                dame = 1;
            }
        }
        if (mob.color_name != 0) {
            dame = (dame * 8) / 10;
            switch (mob.color_name) {
                case 1: {
                    if (p.clazz == 2) {
                        dame /= 2;
                    }
                    break;
                }
                case 2: {
                    if (p.clazz == 3) {
                        dame /= 2;
                    }
                    break;
                }
                case 4: {
                    if (p.clazz == 0) {
                        dame /= 2;
                    }
                    break;
                }
                case 5: {
                    if (p.clazz == 1) {
                        dame /= 2;
                    }
                    break;
                }
            }
        }
        if (mob.template.mob_id == 167 || mob.template.mob_id == 168 || mob.template.mob_id == 169
                || mob.template.mob_id == 170 || mob.template.mob_id == 171 || mob.template.mob_id == 172) {
            dame = 1;
        }
        if (15 > Util.random(0, 100)) { // mob get miss
            dame = 0;
        }
        if (dame < 0) {
            dame = 0;
        }
        if (dame > 2_000_000_000) {
            dame = 2_000_000_000;
        }
        mob.hp -= dame;
        // if mob die
        if (mob.hp <= 0) {
            mob.hp = 0;
            // mob die
            if (!mob.isdie) {
                mob.isdie = true;
                // send p outside
                if (Util.random_ratio(17))
                    ev_he.Event_3.LeaveItemMap(gameMap, mob, p);
                if (30 > Util.random(1, 101))
                    leave_item_by_type7(gameMap, (short) Util.random(417, 464), p, mob.objectId);
                Message m2 = new Message(17);
                m2.writer().writeShort(p.objectId);
                m2.writer().writeShort(mob.objectId);
                for (int i = 0; i < gameMap.players.size(); i++) {
                    Player p2 = gameMap.players.get(i);
                    if (!((Math.abs(p2.x - p.x) < 200) && (Math.abs(p2.y - p.y) < 200))) {
                        p2.conn.addmsg(m2);
                    }
                }
                m2.cleanup();
            }
            p.point_active[1] += (this.wave / 5);
            this.num_mob--;
            if (this.num_mob == 0) {
                this.state = 1;
            }
        }
        // attached in4
        Message m = new Message(9);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(index_skill);
        m.writer().writeByte(1);
        m.writer().writeShort(mob.objectId);
        m.writer().writeInt((int) dame); // dame
        m.writer().writeInt(mob.hp); // hp mob after
        if (dame > 0 && crit) {
            m.writer().writeByte(1); // size color show
            //
            m.writer().writeByte(4); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
            m.writer().writeInt((int) dame); // par
            //
        } else if (dame > 0 && pierce) {
            m.writer().writeByte(1); // size color show
            //
            m.writer().writeByte(1); // 1: xuyen giap, 2:hut hp, 3: hut mp, 4: chi mang, 5: phan don
            m.writer().writeInt((int) dame); // par
            //
        } else {
            m.writer().writeByte(0);
        }
        m.writer().writeInt(p.hp);
        m.writer().writeInt(p.mp);
        m.writer().writeByte(11); // 1: green, 5: small white 9: big white, 10: st dien, 11: st bang
        m.writer().writeInt(0); // dame plus
        MapService.sendMsgPlayerInside(gameMap, p, m, true);
        m.cleanup();
        // exp
        int expup = 0;
        // FIX: clamp dame ke rentang int aman dulu — kalau overflow jadi
        // negatif, updateExp() di bawah dipanggil TANPA syarat expup > 0,
        // jadi player bisa kehilangan exp alih-alih cuma dapat 0.
        long dameForExp = Math.max(0L, Math.min(dame, 2_000_000_000L));
        expup = (int) dameForExp; // tinh exp
        EffTemplate ef = p.getEffectDefault(-125);
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
        // pet attack
        if (!mob.isdie && p.pet_follow != -1) {
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
                for (PetOption temp : my_pet.op) {
                    if (temp.id >= 0 && temp.id <= 4 && temp.maxValue > 0) {
                        a1 = temp.value;
                        a2 = temp.maxValue;
                        break;
                    }
                }
                // Fallback: cari option apapun dengan maxValue > 0 (bukan stat id 23-26)
                if (a2 == 0) {
                    for (PetOption temp : my_pet.op) {
                        if (temp.id < 23 && temp.maxValue > 0) {
                            a1 = temp.value;
                            a2 = temp.maxValue;
                            break;
                        }
                    }
                }
                int dame_pet = Util.random(a1, Math.max(a2, a1 + 1)) - (this.wave * this.wave * 5);
                if (dame_pet <= 0) {
                    dame_pet = 1;
                }
                if (((mob.hp - dame_pet) > 0) && (p.pet_atk_speed < System.currentTimeMillis()) && (a1 > 0)) {
                    p.pet_atk_speed = System.currentTimeMillis() + 1500L;
                    m = new Message(84);
                    m.writer().writeByte(2);
                    m.writer().writeShort(p.objectId);
                    m.writer().writeByte(1);
                    m.writer().writeByte(1);
                    m.writer().writeShort(mob.objectId);
                    m.writer().writeInt(dame_pet);
                    mob.hp -= dame_pet;
                    m.writer().writeInt(mob.hp);
                    m.writer().writeInt(p.hp);
                    m.writer().writeInt(p.body.getMaxHP());
                    p.conn.addmsg(m);
                    m.cleanup();
                }
            }
        }
    }

    private static void leave_item_by_type3(GameMap gameMap, int idItem, int color_, Player p_master, String name, int index)
            throws IOException {
        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = (short) idItem;
            gameMap.itemDrop[index_item_map].color = (byte) color_;
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 3;
            gameMap.itemDrop[index_item_map].idmaster = (short) p_master.objectId;
            List<Option> opnew = new ArrayList<Option>();
            for (Option op_old : ItemTemplate3.item.get(idItem).getOp()) {
                Option temp = new Option(1, 1, (short) 0);
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
            gameMap.itemDrop[index_item_map].op = new ArrayList<>();
            gameMap.itemDrop[index_item_map].op.addAll(opnew);
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(3);
            mi.writer().writeShort(index); // index mob die
            mi.writer().writeShort(ItemTemplate3.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(name);
            mi.writer().writeByte(color_); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leave_item_by_type7(GameMap gameMap, short idItem, Player p_master, int index) throws IOException {
        // Drop item7 id 46-145 dinonaktifkan permanen (tidak tergantung setting player)
        if (idItem >= 46 && idItem <= 145) {
            return;
        }

        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = idItem;
            if (ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getColor() == 21) {
                gameMap.itemDrop[index_item_map].color = 1;
            } else {
                gameMap.itemDrop[index_item_map].color = 0;
            }
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 7;
            gameMap.itemDrop[index_item_map].idmaster = (short) p_master.objectId;
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(7);
            mi.writer().writeShort(index); // id mob die
            mi.writer().writeShort(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getName());
            mi.writer().writeByte(gameMap.itemDrop[index_item_map].color); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public void send_mob_move_when_exit(Player p) throws IOException {
        Message m = new Message(4);
        for (int i = 0; i < this.mobs.size(); i++) {
            Mob_Dungeon mob = this.mobs.get(i);
            m.writer().writeByte(1);
            m.writer().writeShort(mob.template.mob_id);
            m.writer().writeShort(mob.objectId); // index
            m.writer().writeShort(mob.x);
            m.writer().writeShort(mob.y);
            m.writer().writeByte(-1);
        }
        for (int i = 0; i < template.players.size(); i++) {
            Player p0 = template.players.get(i);
            p0.conn.addmsg(m);
        }
        m.cleanup();
    }
}