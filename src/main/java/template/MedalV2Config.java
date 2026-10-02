package template;

public class MedalV2Config {

    // ID item equipment (tabel `equipment`) untuk masing-masing kelas Medal V2.
    public static final short[] MEDAL_V2_ITEM_ID = new short[]{5467, 5468, 5469, 5470};

    public static final String[] MEDAL_V2_NAME = new String[]{
            "Medal Evo Ksatria", "Medal Evo Penyihir", "Medal Evo Assasin", "Medal Evo Penembak"
    };

    public static int getMedalIndexV2(short id) {
        for (int i = 0; i < MEDAL_V2_ITEM_ID.length; i++) {
            if (MEDAL_V2_ITEM_ID[i] == id) return i;
        }
        return -1;
    }

    public static short getMedalItemIdFromIndexV2(int index) {
        if (index < 0 || index >= MEDAL_V2_ITEM_ID.length) return -1;
        return MEDAL_V2_ITEM_ID[index];
    }
}
