
package template;


public class Eff_in_body {
    // 0 -> 4 : special skill clazz, 4: vat ly
    // 52 : + 10 %vang
    // 53 : + 1% hoi hp
    // -121 : troi 4 he clazz 0
    // -122 : .... 
    // -123 : ....
    // -124 : ....
    // 23 : + suc manh skill buff
    // 24 : + phong thu skill buff
    // -126 : chong pk
    // -125 : x2 time
    // = option item = param this option
    public short idEff;
    public int param1,param2;
    public long time_exist;
    
    public Eff_in_body(int id, int param1, int param2, long time_end){
        this.idEff = (short)id;
        this.param1 = param1;
        this.param2 = param2;
        this.time_exist = time_end;
    }
    public Eff_in_body(int id, int param1, long time_end){
        this.idEff = (short)id;
        this.param1 = param1;
        this.time_exist = time_end;
    }
}
