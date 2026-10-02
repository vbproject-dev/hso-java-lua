package ai;

import client.Player;
import client.io.Message;

import java.io.IOException;
import java.util.List;
import game.map.GameMap;
import game.map.MobInMap;
import template.MainObject;
import template.MobTemplate;
import template.PlayerPart;


public class MobAi extends MobInMap {
    public long timeATK;
    
    public List<PlayerPart> part_p;//
    private byte head;//
    private byte eye;//
    private byte hair;//
    private int pointpk;//
    private short clan_icon = -1;
    private int clan_id = -1;
    private String clan_name_clan_shorted;
    private byte clan_mem_type;
    private byte[] fashion = new byte[]{-1,-1,-1,-1,-1,-1,-1};//
    private short mat_na=-1;//
    private short phi_phong=-1;//
    private short weapon=-1;//
    private short id_horse=-1;//
    private short id_hair=-1;//
    private short id_wing=-1;//
    private short id_img_mob=-1;//
    private byte type_use_mount=-1;//
    public long act_time;
    public boolean is_move=true;//
    public Player p_target;
    public int p_skill_id;//
    public int crit;
    public long time_hp_buff;
    public int pierce;
    
    
    public MobAi(int map_id, int zone_id, int id_temp_mob, int index, int x, int y, int clazz, int head, int eye, int hair, int lv, int hp, int pk, int dame, int def, int crit, List<PlayerPart> part, int id_img_mob){
        MobTemplate temp = MobTemplate.entrys.get(id_temp_mob);
        this.map_id = (byte)map_id;
        this.zone_id = (byte)zone_id;
        this.name = temp.name;
        this.clazz = (byte) clazz;
        this.objectId = index;
        this.x = this.xOld = (short)x;
        this.y = this.yOld = (short)y;
        this.head = (byte)head;
        this.eye = (byte) eye;
        this.hair = (byte) hair;
        this.level = (short) lv;
        this.hp = maxHp = hp;
        this.pointpk = (byte)pk;
        this.dame = dame;
        this.def = def;
        this.crit = crit;
        this.part_p = part;
        this.id_img_mob = (short)id_img_mob;
        this.template = temp;
        this.time_refresh = 11;
    }
   // @Override
    public boolean isMobCTruong(){
        return true;
    }
    
    public MobAi(int map_id, int zone_id, int id_temp_mob, String name, int index, int x, int y, int clazz, int head, int eye, int hair, int lv, int hp, int pk, int dame, int def, int crit, List<PlayerPart> part, int id_img_mob){
        MobTemplate temp = MobTemplate.entrys.get(id_temp_mob);
        this.map_id = (byte)map_id;
        this.zone_id = (byte)zone_id;
        this.name = name;
        this.clazz = (byte) clazz;
        this.objectId = index;
        this.x = this.xOld = (short)x;
        this.y = this.yOld = (short)y;
        this.head = (byte)head;
        this.eye = (byte) eye;
        this.hair = (byte) hair;
        this.level = (short) lv;
        this.hp = this.maxHp = hp;
        this.pointpk = (byte)pk;
        this.dame = dame;
        this.def = def;
        this.crit = crit;
        this.part_p = part;
        this.id_img_mob = (short)id_img_mob;
        this.template = temp;
        this.time_refresh = 11;
    }
    
    public void send_in4(Player p)throws IOException{
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
        m.writer().writeByte(this.part_p.size());
        //
        for (int i = 0; i < this.part_p.size(); i++) {
            m.writer().writeByte(this.part_p.get(i).type);
            m.writer().writeByte(this.part_p.get(i).part);
            m.writer().writeByte(3);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1); // eff
        }
        //
        m.writer().writeShort(this.clan_icon);
        if(clan_icon>-1)
        {
            m.writer().writeInt(this.clan_id);
            m.writer().writeUTF(this.clan_name_clan_shorted);
            m.writer().writeByte(this.clan_mem_type);
        }
        m.writer().writeByte(-1); // pet
        m.writer().writeByte(this.fashion.length);
        for (int i = 0; i < this.fashion.length; i++) {
            // Sama seperti fix di Clone.send_in4(): format elemen fashion (byte vs short)
            // tergantung versi client si penerima (p), bukan selalu byte.
            if (p.conn.version < 280) {
                m.writer().writeByte(this.fashion[i]);
            } else {
                m.writer().writeShort(this.fashion[i]);
            }
        }
        //
        m.writer().writeShort(id_img_mob);//id_img_mob
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
    public int getTypeObject(){
        return 0;
    }
    
    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk){
        try{
            if(this.hp >0)return;
            this.hp =0;
            this.isdie = true;
            this.time_back = System.currentTimeMillis() + 120_000;
        }catch(Exception e){}
    }
}