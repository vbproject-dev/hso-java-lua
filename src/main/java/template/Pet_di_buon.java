package template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import core.Service;
import client.io.Message;
import game.map.ItemMap;
import game.map.GameMap;
import game.map.MapService;

public class Pet_di_buon extends MainObject {

    public Player p;
    public short type;
    public List<Short> item;
    public int id_map;
    public long time_move;
    public long time_skill;
    public int speed;
    private byte countSpeed;

    public Pet_di_buon(int type, int index_mob, int x, int y, int id_map, String name, Player p) {
        this.type = (short) type;
        this.objectId = Short.toUnsignedInt((short) index_mob);
        this.item = new ArrayList<>();
        this.x = (short) x;
        this.y = (short) y;
        this.time_move = System.currentTimeMillis() + 1000L;
        this.id_map = id_map;
        this.name = name;
        this.hp = 1_000_000;
        this.maxHp = 1_000_000;
        this.time_skill = System.currentTimeMillis() + 15_000L;
        this.speed = 1;
        this.p = p;
    }

    public synchronized void update_hp(Player p, int hp) throws IOException {
        if (this.time_skill < System.currentTimeMillis()) {
            this.time_skill = System.currentTimeMillis() + 5_000L;
            this.hp += hp;
            if (this.hp > this.maxHp) {
                this.hp = this.maxHp;
            }
            if (this.hp < 0) {
                this.hp = 0;
            }
            this.update_all(p);
            p.updateGem(-5);
            p.item.charInventory(5);
        } else {
            Service.send_notice_box(p.conn,
                    "Waktu penggunaan berikutnya : " + (this.time_skill - System.currentTimeMillis()) + "ms");
        }
    }

    public synchronized void update_speed(Player p) throws IOException {
        if (countSpeed > 0) {
            Service.send_notice_box(p.conn, "Hanya bisa mempercepat 1 kali");
            return;
        }
        if (this.time_skill < System.currentTimeMillis()) {
            this.time_skill = System.currentTimeMillis() + 10_000L;
            this.speed = 4;
            countSpeed++;
            this.update_all(p);
            p.updateGem(-5);
            p.item.charInventory(5);
        } else {
            Service.send_notice_box(p.conn,
                    "Waktu penggunaan berikutnya : " + (this.time_skill - System.currentTimeMillis()) + "ms");
        }
    }

    public void update_all(Player p) throws IOException {
        Message mm = new Message(7);
        mm.writer().writeShort(this.objectId);
        mm.writer().writeByte(120);
        mm.writer().writeShort(this.x);
        mm.writer().writeShort(this.y);
        mm.writer().writeInt(this.hp);
        mm.writer().writeInt(this.maxHp);
        mm.writer().writeByte(0);
        mm.writer().writeInt(-1);
        mm.writer().writeShort(-1);
        mm.writer().writeByte(1);
        mm.writer().writeByte(this.speed);
        mm.writer().writeByte(0);
        mm.writer().writeUTF(this.name);
        mm.writer().writeLong(-11111);
        mm.writer().writeByte(4);
        MapService.sendMsgPlayerInside(p.map, p, mm, true);
        mm.cleanup();
    }

    @Override
    public boolean isMobDiBuon() {
        return true;
    }

    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk) {
        try {
            if (this.hp <= 0) {
                this.isdie = true;
                this.hp = 0;
                Pet_di_buon_manager.remove(this.name);
                this.p.pet_di_buon = null;
                for (int j = 0; j < this.item.size(); j++) {
                    ItemMap it_leave = new ItemMap();
                    it_leave.id_item = (short) this.item.get(j);
                    it_leave.color = (byte) 0;
                    it_leave.quantity = 1;
                    it_leave.category = 3;
                    it_leave.idmaster = (short) this.p.objectId;
                    it_leave.op = new ArrayList<>();
                    it_leave.time_exist = System.currentTimeMillis() + 60_000L;
                    it_leave.time_pick = System.currentTimeMillis() + 1_500L;
                    gameMap.add_item_map_leave(gameMap, (Player) this.p, it_leave, this.objectId);
                }
            }
        } catch (Exception e) {
        }
    }
}
