
package game.shop;


public class item_template {
    public byte catagory;
    public short id;
    public short date;
    public short level;
    public byte color;
    
    public byte typeMoney;
    public int Money;
    public item_template(int cat, int id, int quant, int date, int level, int color, int typeMoney, int money){
        this.catagory = (byte)cat;
        this.id = (short)id;
        this.date = (short)date;
        this.level = (short)level;
        this.color = (byte)color;
        this.typeMoney = (byte)typeMoney;
        this.Money = money;
    }
}
