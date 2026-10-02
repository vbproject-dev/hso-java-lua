package template;

import java.util.ArrayList;
import java.util.List;
import game.map.GameMap;

public class MobTemplate {
	public static final List<MobTemplate> entrys = new ArrayList<>();
	public short mob_id;
	public String name;
	public short level;
	public int hpmax;
	public int damage;
	public byte typemove;
          public GameMap gameMap;
        public boolean is_boss;



        public static MobTemplate getMob(int id) {
            for (MobTemplate m : entrys) {
                if (m.mob_id == id) {
                    return m;
                }
            }

            return null;
        }
}