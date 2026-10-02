package template;

/**
 * Material untuk upgrade medal (bintang).
 *
 * Cara kerja:
 *   medal_create_material[i] = Medal_Material.m_xxx[x][y] + 200
 *
 * Nilai di array ini adalah ID item lv1 (46-145), setelah +200 menjadi ID lv3 (246-345).
 *
 * Pengelompokan berdasarkan setcolorname di item7:
 *   m_white : lv1 ID 46-115  → lv3 ID 246-315 (setcolorname 40)
 *   m_blue  : lv1 ID 116-125 → lv3 ID 316-325 (setcolorname 41)
 *   m_yellow: lv1 ID 126-135 → lv3 ID 326-335 (setcolorname 42)
 *   m_violet: lv1 ID 136-145 → lv3 ID 336-345 (setcolorname 43)
 *
 * m_white dibagi 7 grup (type I–VII), masing-masing 10 item unik.
 * Util.random(0, 10) → index 0..9, jadi tiap array harus panjang 10.
 */
public class Medal_Material {

    // White materials — 7 grup x 10 item, lv1 ID 46–115 → +200 = lv3 ID 246–315
    public static short[][] m_white = new short[][] {
        // type I  : Feather, Fox eyes, Seaweed, Herbs, Blood orchid, Wild orchid leaves, Blood fruit, Wild orchids, Lotus bud, Pink clam shell
        new short[] {46, 47, 48, 49, 50, 51, 52, 53, 54, 55},
        // type II : Coral, Fang leopard, White rhino horn, Phoenix mine, Golden Rhino Horn, Kayu hitam, Balok heksagonal, Bola kaca, Tengkorak, Kayu hitam kecil
        new short[] {56, 57, 58, 59, 60, 61, 62, 63, 64, 65},
        // type III: Ekor kadal, Jamur merah, Kulit kerang, Medali, Tengkorak kristal, Tumbuhan merambat, Ginseng hutan, Mata iblis, Sarang lebah, Cabang kaktus
        new short[] {66, 67, 68, 69, 70, 71, 72, 73, 74, 75},
        // type IV : Akar pohon, Taring serigala, Kain robek, Sabuk ksatria, Pecahan tulang, Kulit kerang, Batu bintang kecil, Batu bintang besar, Amber, Bulu elang
        new short[] {76, 77, 78, 79, 80, 81, 82, 83, 84, 85},
        // type V  : Batu kerikil, Kulit kerang laut, Liontin iblis, Perangkap binatang, Telur phoenix, Kalung iblis, Gading gajah hutan, Cakar, Bulu, Batu sihir
        new short[] {86, 87, 88, 89, 90, 91, 92, 93, 94, 95},
        // type VI : Bulu domba, Cakar serigala, Cakar naga, Mata sihir, Ekor rubah, Mahkota emas, Taring babi hutan, Batu nisan sihir, Tulang kambing, Tulang serigala
        new short[] {96, 97, 98, 99, 100, 101, 102, 103, 104, 105},
        // type VII: Kayu gaharu, Kaki katak, Taji ayam hutan, Jaring laba-laba, Cangkang kura-kura hijau, Paruh gagak, Lonceng sihir, Telinga kelinci, Bola kristal, Cermin sihir
        new short[] {106, 107, 108, 109, 110, 111, 112, 113, 114, 115},
    };

    // Blue materials — lv1 ID 116–125 → +200 = lv3 ID 316–325 (setcolorname 41)
    // Cakar serigala, Topeng ksatria, Kulit pohon, Ekor kadal, Bulu kelinci putih,
    // Sayap kelelawar, Tulang kaktus, Kaki beruang, Tangan beruang, Sisik naga
    public static final short[] m_blue = new short[] {116, 117, 118, 119, 120, 121, 122, 123, 124, 125};

    // Yellow materials — lv1 ID 126–135 → +200 = lv3 ID 326–335 (setcolorname 42)
    // Topeng iblis, Kulit ular, Paruh elang, Ekor ular, Batu lava,
    // Kumbang kristal, Karang hitam, Kaki naga, Sayap burung, Kaki kadal
    public static final short[] m_yellow = new short[] {126, 127, 128, 129, 130, 131, 132, 133, 134, 135};

    // Violet materials — lv1 ID 136–145 → +200 = lv3 ID 336–345 (setcolorname 43)
    // Sayap naga, Ekor ikan mas, Sirip hiu, Cangkang kerang hijau, Sirip paus,
    // Ekor kalajengking, Ekor ular sanca, Mata elang, Taring ular, Labu hutan
    public static final short[] m_violet = new short[] {136, 137, 138, 139, 140, 141, 142, 143, 144, 145};
}