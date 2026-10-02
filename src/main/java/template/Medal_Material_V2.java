package template;

/**
 * Material untuk pembuatan & upgrade Medal V2 — TERPISAH TOTAL dari
 * Medal_Material.java (dipakai Medal V1). Semua ID di sini adalah item
 * baru khusus Medal V2 (bukan re-use item lv1/lv3 lama), jadi tidak akan
 * bentrok / berebut stok dengan sistem medal lama.
 *
 * !! PENTING !! ID di bawah adalah PLACEHOLDER. Insert baris baru ke tabel
 * `item7` dengan ID ini (lihat medal_v2_migration.sql) sebelum dipakai,
 * atau ganti nilainya supaya cocok dengan ID kosong di database kamu.
 *
 * medal_create_material_v2[i] = salah satu nilai di bawah (langsung dipakai
 * tanpa offset tier, karena ini item khusus V2, bukan varian level dari
 * item lain).
 */
public class Medal_Material_V2 {

    // 2 grup "putih" x 10 item (grup dipilih acak beda supaya nama material slot 0 & 1 tidak sama)
    public static short[][] m_white = new short[][]{
            new short[]{500, 501, 502, 503, 504, 505, 506, 507, 508, 509},
            new short[]{510, 511, 512, 513, 514, 515, 516, 517, 518, 519},
    };

    public static short[] m_blue = new short[]{520, 521, 522, 523, 524, 525, 526, 527, 528, 529};

    public static short[] m_yellow = new short[]{530, 531, 532, 533, 534, 535, 536, 537, 538, 539};

    public static short[] m_violet = new short[]{540, 541, 542, 543, 544, 545, 546, 547, 548, 549};

    /**
     * Semua ID material Medal V2 digabung jadi satu array — dipakai untuk
     * menampilkan isi "Toko Material" Medal V2 (lihat Service.send_box_UI
     * case khusus Medal V2 & MenuManager.openMedalV2Menu).
     */
    public static short[] getAllMaterialIds() {
        java.util.List<Short> ids = new java.util.ArrayList<>();
        for (short[] group : m_white) {
            for (short id : group) ids.add(id);
        }
        for (short id : m_blue) ids.add(id);
        for (short id : m_yellow) ids.add(id);
        for (short id : m_violet) ids.add(id);

        short[] result = new short[ids.size()];
        for (int i = 0; i < result.length; i++) result[i] = ids.get(i);
        return result;
    }
}