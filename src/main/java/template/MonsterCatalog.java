package template;

public class MonsterCatalog {
    public short id;
    public String name;
    public byte level;
    public int hp;
    public byte type;

    public MonsterCatalog(short id, String name, byte level, int hp, byte type) {
        this.id =id;
        this.name=name;
        this.level = level;
        this.hp=hp;
        this.type=type;
    }

}
