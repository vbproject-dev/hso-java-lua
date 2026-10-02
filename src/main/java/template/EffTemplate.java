package template;

public class EffTemplate {
// 52  : +10% gold (Vàng)
// 53  : +1% HP regeneration (Hồi HP)

// -121 : Restriction: usable only by class 0 (Trói 4 hệ clazz 0)
// -122 : (reserved / unknown effect)
// -123 : (reserved / unknown effect)
// -124 : (reserved / unknown effect)
// 23  : + Strength (Sức mạnh) when skill buff is active
// 24  : + Defense (Phòng thủ) when skill buff is active
// -126 : Anti-PK (Chống PK)
// -125 : Double skill duration (x2 time)
// => These are item options = parameters for this option
    public int id;
    public short param;
    public short param2;
    public long time;

    public EffTemplate(int id, int param, long time) {
        this.id = id;
        this.param = (short) param;
        this.time = time;
    }

    public EffTemplate(int id, int param, int para2, long time) {
        this.id = id;
        this.param = (short) param;
        this.param2 = (short) param2;
        this.time = time;
    }
}
