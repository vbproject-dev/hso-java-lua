package utils;


import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IconHelper {

    // ---------------- offsets (unchanged) ----------------
    private static final int ITEM_MAP_OFFSET = 0;
    private static final int MONSTER_OFFSET = 1000;
    private static final int EQUIPMENT_OFFSET = 2000;
    private static final int NPC_OFFSET = 3000;
    private static final int POTION_OFFSET = 4000;
    private static final int QUEST_OFFSET = 5000;
    private static final int MATERIAL_OFFSET = 5500;
    private static final int SKILL_OFFSET = 6000;
    private static final int ICON_CLAN_OFFSET = 7000;
    private static final int ICON_ARC_CLAN_OFFSET = 9500;
    private static final int PET_ICON_OFFSET = 10000;
    private static final int PET_IMAGE_OFFSET = 10200;
    private static final int MOUNT_OFFSET = 10700;
    private static final int NEW_EQUIPMENT_OFFSET = 13000;

    // ---------------- CACHE ----------------
    private static final LoadingCache<String, byte[]> ICON_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(5000)                  // hot icons only
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build(IconHelper::loadFromDisk);

    // cache key = "zoom:iconId"
    private static String key(int zoom, int iconId) {
        return zoom + ":" + iconId;
    }

    // ---------------- PUBLIC API ----------------
    public static byte[] getIcon(int zoom, int iconId) {
        return ICON_CACHE.get(key(zoom, iconId));
    }

    public static byte[] getIconByType(int zoom, IconType type, int localId) {
        int globalId = getOffset(type) + localId;
        return getIcon(zoom, globalId);
    }

    // ---------------- LAZY LOADER ----------------
    private static byte[] loadFromDisk(String key) {
        try {
            String[] p = key.split(":");
            int zoom = Integer.parseInt(p[0]);
            int iconId = Integer.parseInt(p[1]);

            Path path = buildIconPath(zoom, iconId);

            if (!Files.exists(path)) {
                log.warn("[ICON] Missing: {}", path.toAbsolutePath());
                return null;
            }

            return Files.readAllBytes(path);

        } catch (Exception e) {
            log.error("[ICON] Load failed: {}", key, e);
            return null;
        }
    }

    private static int getOffset(IconType type) {
        return switch (type) {
            case ITEM_MAP -> ITEM_MAP_OFFSET;
            case MONSTER -> MONSTER_OFFSET;
            case EQUIPMENT -> EQUIPMENT_OFFSET;
            case NPC -> NPC_OFFSET;
            case POTION -> POTION_OFFSET;
            case QUEST -> QUEST_OFFSET;
            case MATERIAL -> MATERIAL_OFFSET;
            case SKILL -> SKILL_OFFSET;
            case CLAN -> ICON_CLAN_OFFSET;
            case ARC_CLAN -> ICON_ARC_CLAN_OFFSET;
            case PET_ICON -> PET_ICON_OFFSET;
            case PET_IMAGE -> PET_IMAGE_OFFSET;
            case MOUNT -> MOUNT_OFFSET;
            default -> 0;
        };
    }

    static IconType resolveIcon(int iconId) {
        if (iconId >= NEW_EQUIPMENT_OFFSET) return IconType.EQUIPMENT;
        if (iconId >= MOUNT_OFFSET) return IconType.MOUNT;
        if (iconId >= PET_IMAGE_OFFSET) return IconType.PET_IMAGE;
        if (iconId >= PET_ICON_OFFSET) return IconType.PET_ICON;
        if (iconId >= ICON_ARC_CLAN_OFFSET) return IconType.ARC_CLAN;
        if (iconId >= ICON_CLAN_OFFSET) return IconType.CLAN;
        if (iconId >= SKILL_OFFSET) return IconType.SKILL;
        if (iconId >= MATERIAL_OFFSET) return IconType.MATERIAL;
        if (iconId >= QUEST_OFFSET) return IconType.QUEST;
        if (iconId >= POTION_OFFSET) return IconType.POTION;
        if (iconId >= NPC_OFFSET) return IconType.NPC;
        if (iconId >= EQUIPMENT_OFFSET) return IconType.EQUIPMENT;
        if (iconId >= MONSTER_OFFSET) return IconType.MONSTER;
        if (iconId >= ITEM_MAP_OFFSET) return IconType.ITEM_MAP;
        return IconType.UNKNOWN;
    }

    static int getLocalId(int iconId) {
        if (iconId >= NEW_EQUIPMENT_OFFSET) return iconId - EQUIPMENT_OFFSET;
        if (iconId >= MOUNT_OFFSET) return iconId - MOUNT_OFFSET;
        if (iconId >= PET_IMAGE_OFFSET) return iconId - PET_IMAGE_OFFSET;
        if (iconId >= PET_ICON_OFFSET) return iconId - PET_ICON_OFFSET;
        if (iconId >= ICON_ARC_CLAN_OFFSET) return iconId - ICON_ARC_CLAN_OFFSET;
        if (iconId >= ICON_CLAN_OFFSET) return iconId - ICON_CLAN_OFFSET;
        if (iconId >= SKILL_OFFSET) return iconId - SKILL_OFFSET;
        if (iconId >= MATERIAL_OFFSET) return iconId - MATERIAL_OFFSET;
        if (iconId >= QUEST_OFFSET) return iconId - QUEST_OFFSET;
        if (iconId >= POTION_OFFSET) return iconId - POTION_OFFSET;
        if (iconId >= NPC_OFFSET) return iconId - NPC_OFFSET;
        if (iconId >= EQUIPMENT_OFFSET) return iconId - EQUIPMENT_OFFSET;
        if (iconId >= MONSTER_OFFSET) return iconId - MONSTER_OFFSET;
        return iconId;
    }

    static Path buildIconPath(int zoom, int iconId) {
        IconType folder = resolveIcon(iconId);
        return Path.of(
                "data",
                "icons",
                "x" + zoom,
                folder.name().toLowerCase(),
                getLocalId(iconId) + ".png");
    }

    public static Path saveIconByType(int zoom, IconType type, int localId, byte[] bytes)
            throws IOException {
        int globalId = getOffset(type) + localId;
        return saveIcon(zoom, globalId, bytes);
    }

    public static Path saveIcon(int zoom, int iconId, byte[] bytes)
            throws IOException {

        Path path = buildIconPath(zoom, iconId);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);

        ICON_CACHE.invalidate(key(zoom, iconId));
        return path;
    }

    /**
     * Invalidate a single icon (type+localId) across ALL zoom levels (x1-x4).
     * Use this after replacing/editing an icon's PNG on disk (e.g. a variant change),
     * so every zoom tier re-reads the new file instead of serving stale cached bytes
     * whose dimensions may no longer match (causes cropped/cut-off rendering).
     */
    public static void invalidateAllZoom(IconType type, int localId) {
        int globalId = getOffset(type) + localId;
        for (int zoom = 1; zoom <= 4; zoom++) {
            ICON_CACHE.invalidate(key(zoom, globalId));
        }
        log.info("[ICON] Invalidated all zooms for type={} localId={}", type, localId);
    }
}
