package game.event.pvp;

import client.Player;
import client.io.Message;

import client.io.Session;
import core.Util;
import game.map.GameMap;
import game.map.MapService;
import lombok.Getter;
import lombok.Setter;
import template.MainObject;

import java.io.IOException;

@Getter
@Setter

public class Building extends MainObject {
    private Team team;
    private Lane lane;

    private GameMap map;

    public Building(int id, int x, int y, Team team, Lane lane) {
        this.objectId = id;
        this.x =(short) x;
        this.y = (short)y;
        this.team = team;
        this.lane = lane;
        this.level = 300;
        this.maxHp = 5000_000;
        this.hp = this.maxHp;
        this.name = lane == Lane.BASE
                ? String.format("%s Team Base", team.getType())
                : String.format("%s %s Turret", team.getType(), lane);

        dame = 300;
    }

    @Override
    public int getBaseDamage() {
        var dmg = dame * this.level * (0.8 + Math.random() * 0.4); // 80%-120%
        return (int) (lane == Lane.BASE ? dmg * 1 : dmg);
    }

    public void sendBuildingInfo(Player player) {
        try {

            Message m = new Message(4);
            m.writer().writeByte(1);
            m.writer().writeShort(64);
            m.writer().writeShort(this.objectId);
            m.writer().writeShort(this.x);
            m.writer().writeShort(this.y);
            m.writer().writeByte(-1);
            player.conn.addmsg(m);

            Message mm = new Message(7);
            mm.writer().writeShort(this.objectId);
            mm.writer().writeByte((byte) this.level);
            mm.writer().writeShort(this.x);
            mm.writer().writeShort(this.y);
            mm.writer().writeInt(this.hp);
            mm.writer().writeInt(this.maxHp);
            mm.writer().writeByte(0);
            mm.writer().writeInt(4);

            mm.writer().writeShort(-1);

            mm.writer().writeUTF(this.name);
            mm.writer().writeByte(0);
            mm.writer().writeByte(2);
            mm.writer().writeByte(0);
            mm.writer().writeUTF("");
            mm.writer().writeLong(-11111);
            mm.writer().writeByte(4);

         player.conn.addmsg(mm);
        } catch (Exception ignore) {}
    }

    @Override
    public void setDie(GameMap gameMap, MainObject mainAtk) throws IOException {
        isdie = true;

        if (this.lane == Lane.BASE) {
            team.getMatch().onBaseDestroyed(this);
        }else{
            try {
                Message m = new Message(53);
                m.writer().writeUTF(String.format("%s telah menghancurkan %s ", mainAtk.name, this.name)) ;
                m.writer().writeByte(1);
                team.getMatch().broadcast(m);
            } catch (IOException ignore) {

            }
        }
    }
}
