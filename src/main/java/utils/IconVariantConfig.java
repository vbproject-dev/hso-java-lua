package utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Switch" di DB buat nentuin file icon mana yang dipakai untuk sebuah (IconType, localId),
 * TANPA perlu rename file fisik di data/icons/xN/{type}/.
 * <p>
 * Sama konsepnya kayak PartVariantConfig (buat part_char), cuma ini buat icon
 * (item_map, monster, equipment, npc, potion, quest, material, skill, dll - semua
 * IconType kepakai, gak cuma item_map).
 * <p>
 * Contoh kasus: IconType.ITEM_MAP, localId=8. Defaultnya baca file "8.png". Ada file
 * alternatif "8_1.png" di folder yang sama. Biar bisa pindah ke variant itu tanpa
 * bongkar-pasang nama file:
 * <pre>
 *   IconVariantConfig.setVariant(IconType.ITEM_MAP, 8, 1); // -> pakai 8_1.png
 *   IconVariantConfig.setVariant(IconType.ITEM_MAP, 8, 0); // -> balik ke 8.png (default)
 * </pre>
 * Client & protokol TIDAK berubah sama sekali - client tetap minta iconId yang sama
 * seperti biasa, servernya aja yang nerjemahin ke file fisik yang beda. Perubahan
 * langsung kepakai (live, tanpa restart server) karena cache icon (Caffeine, di
 * IconHelper) langsung di-invalidate begitu switch di-set.
 */
@Slf4j
public class IconVariantConfig {

    private static final String TABLE = "icon_variant";

    private static final Map<String, Integer> variantMap = new ConcurrentHashMap<>();

    private static String key(IconType type, int localId) {
        return type.name() + ":" + localId;
    }

    /**
     * Load semua override dari DB ke memory. Panggil sekali saat server start.
     */
    public static void load() {
        variantMap.clear();
        List<IconVariant> rows = SQLHelper.selectFrom(TABLE).getAsModel(IconVariant.class);
        for (IconVariant row : rows) {
            if (row.getVariant() != 0 && row.getIconType() != null) {
                variantMap.put(key(row.getIconType(), row.getLocalId()), row.getVariant());
            }
        }
        log.info("[IconVariantConfig] Loaded {} variant override(s) dari tabel {}", variantMap.size(), TABLE);
    }

    /** 0 = default (nama file asli localId, tanpa suffix) */
    public static int getVariant(IconType type, int localId) {
        return variantMap.getOrDefault(key(type, localId), 0);
    }

    /**
     * Nama file (dengan ekstensi .png) yang bakal dipakai IconHelper untuk baca file fisik,
     * sudah termasuk suffix variant kalau ada override.
     * ITEM_MAP, localId=8, variant=1 -> "8_1.png"
     * ITEM_MAP, localId=8, variant=0 -> "8.png"
     */
    public static String resolveFileName(IconType type, int localId) {
        int variant = getVariant(type, localId);
        return variant > 0 ? (localId + "_" + variant + ".png") : (localId + ".png");
    }

    /**
     * Ganti variant (type,localId) -> simpan ke DB + update cache di memory + invalidate
     * cache icon di IconHelper (utk zoom 1-4) biar efeknya LANGSUNG kepakai, tanpa restart.
     *
     * @param type    kategori icon (ITEM_MAP, MONSTER, EQUIPMENT, dst)
     * @param localId id icon di dalam kategori itu (mis. 8 -> file "8.png")
     * @param variant 0 = default, 1 = suffix "_1", dst.
     */
    public static boolean setVariant(IconType type, int localId, int variant) {
        boolean updated = SQLHelper.update(TABLE)
                .set("variant", variant)
                .where("icon_type", type.name())
                .where("local_id", localId)
                .execute();

        if (!updated) {
            long insertedId = SQLHelper.insert(TABLE)
                    .value("icon_type", type.name())
                    .value("local_id", localId)
                    .value("variant", variant)
                    .execute();

            if (insertedId < 0) {
                log.warn("[IconVariantConfig] Gagal set variant {} localId={} -> {}", type, localId, variant);
                return false;
            }
        }

        if (variant != 0) {
            variantMap.put(key(type, localId), variant);
        } else {
            variantMap.remove(key(type, localId));
        }

        IconHelper.invalidateAllZoom(type, localId);

        log.info("[IconVariantConfig] {} localId={} -> variant={} (\"{}\"), langsung kepakai tanpa restart",
                type, localId, variant, resolveFileName(type, localId));
        return true;
    }
}
