package template;

public class LuaObject extends MainObject {

    public LuaObject(int id, String name, int x, int y) {
        this.objectId = id;
        this.x = (short)x;
        this.y = (short)y;
        this.name = name;
    }

}
