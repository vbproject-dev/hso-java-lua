package template;

import lombok.extern.slf4j.Slf4j;
import utils.SQLHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Switch" di DB buat nentuin file part_char mana yang dipakai untuk sebuah (type, id),
 * TANPA perlu rename file fisik di data/part_char/xN/img|data.
 * <p>
 * Contoh kasus: type=100 (tile), id=0. Defaultnya baca file "100_0.png". Ada file
 * alternatif "100_0_1.png" di folder yang sama (variant lain dari tile yang sama).
 * Biar bisa pindah ke variant itu tanpa bongkar-pasang nama file:
 * <pre>
 *   PartVariantConfig.setVariant((byte) 100, (short) 0, 1); // -> pakai 100_0_1.png
 *   PartVariantConfig.setVariant((byte) 100, (short) 0, 0); // -> balik ke 100_0.png (default)
 * </pre>
 * Client & protokol TIDAK berubah sama sekali — client tetap minta type=100 id=0 seperti
 * biasa, servernya aja yang nerjemahin ke file fisik yang beda berdasarkan setting DB ini.
 * Perubahan langsung kepakai (live), tanpa perlu restart server, karena begitu di-set,
 * cache PartData yang sudah ke-load untuk (type,id) itu langsung di-invalidate supaya
 * request berikutnya baca ulang dari file yang baru.
 */
@Slf4j
public class PartVariantConfig {

    private static final String TABLE = "part_variant";

    // key = (type << 16) | (id & 0xFFFF)
    private static final Map<Integer, Integer> variantMap = new ConcurrentHashMap<>();

    private static int key(byte type, short id) {
        return (type << 16) | (id & 0xFFFF);
    }

    /**
     * Load semua override dari DB ke memory. Panggil sekali saat server start
     * (mis. barengan MapManager.loadMapData() / init lainnya).
     */
    public static void load() {
        variantMap.clear();
        List<PartVariant> rows = SQLHelper.selectFrom(TABLE).getAsModel(PartVariant.class);
        for (PartVariant row : rows) {
            if (row.getVariant() != 0) {
                variantMap.put(key(row.getType(), row.getId()), row.getVariant());
            }
        }
        log.info("[PartVariantConfig] Loaded {} variant override(s) dari tabel {}", variantMap.size(), TABLE);
    }

    /** 0 = default (nama file asli type_id, tanpa suffix) */
    public static int getVariant(byte type, short id) {
        return variantMap.getOrDefault(key(type, id), 0);
    }

    /**
     * Nama file dasar (tanpa ekstensi) yang bakal dipakai PartDataLoader untuk baca
     * gambar/data physical, sudah termasuk suffix variant kalau ada override.
     * type=100, id=0, variant=1 -> "100_0_1"
     * type=100, id=0, variant=0 -> "100_0"
     */
    public static String resolveBaseName(byte type, short id) {
        int variant = getVariant(type, id);
        return variant > 0 ? (type + "_" + id + "_" + variant) : (type + "_" + id);
    }

    /**
     * Ganti variant (type,id) -> simpan ke DB + update cache di memory + invalidate
     * cache PartData yang sudah ke-load di PartDataLoader biar efeknya LANGSUNG kepakai,
     * tanpa perlu restart server.
     *
     * @param type    part_char type (mis. 100/101 = tile, 102 = water)
     * @param id      part_char id (mis. 0)
     * @param variant 0 = default (100_0), 1 = pakai suffix "_1" (100_0_1), dst.
     */
    public static boolean setVariant(byte type, short id, int variant) {
        boolean updated = SQLHelper.update(TABLE)
                .set("variant", variant)
                .where("type", type)
                .where("id", id)
                .execute();

        if (!updated) {
            // Row belum ada -> insert baru
            long insertedId = SQLHelper.insert(TABLE)
                    .value("type", type)
                    .value("id", id)
                    .value("variant", variant)
                    .execute();

            if (insertedId < 0) {
                log.warn("[PartVariantConfig] Gagal set variant type={} id={} -> {}", type, id, variant);
                return false;
            }
        }

        if (variant != 0) {
            variantMap.put(key(type, id), variant);
        } else {
            variantMap.remove(key(type, id));
        }

        // Buang cache lama biar request berikutnya baca file yang baru
        PartDataLoader.invalidate(type, id);

        log.info("[PartVariantConfig] type={} id={} -> variant={} (\"{}\"), langsung kepakai tanpa restart",
                type, id, variant, resolveBaseName(type, id));
        return true;
    }
}
