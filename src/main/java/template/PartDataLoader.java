package template;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

@Slf4j
public class PartDataLoader {

    private static final String BASE_DIR = "data/part_char";
    private static final Map<String, Map<Byte, Map<Short, PartData>>> variantParts = new HashMap<>();

    // zoomLv -> variantName
    private static String resolveVariant(int zoomLv) {
        return switch (zoomLv) {
            case 2 -> "x2";
            case 3 -> "x3";
            case 4 -> "x4";
            default -> "x1";
        };
    }

    /**
     * Lazy load: only loads the specific part when requested
     */
    public static PartData getByZoom(int zoomLv, byte type, short id) {
        String variant = resolveVariant(zoomLv);

        // Check if already loaded
        Map<Byte, Map<Short, PartData>> byVariant = variantParts.get(variant);
        if (byVariant != null) {
            Map<Short, PartData> byType = byVariant.get(type);
            if (byType != null && byType.containsKey(id)) {
                return byType.get(id);
            }
        }

        // Not loaded yet, load on demand
        return loadPartOnDemand(variant, type, id);
    }

    /**
     * Load a specific part from disk when requested
     */
    private static PartData loadPartOnDemand(String variant, byte type, short id) {
        String imageDir = BASE_DIR + "/" + variant + "/img";
        String dataDir = BASE_DIR + "/" + variant + "/data";

        String baseName = type + "_" + id;
        Path imagePath = Paths.get(imageDir, baseName + ".png");
        Path dataPath = Paths.get(dataDir, baseName);

        // Check if files exist
        if (!Files.exists(imagePath) || !Files.exists(dataPath)) {
            log.info("[PartDataLoader][{}] Part not found: {}", variant, baseName);
            return null;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            byte[] dataBytes = Files.readAllBytes(dataPath);

            PartData part = new PartData();
            part.type = type;
            part.id = id;
            part.image = imageBytes;
            part.imageData = dataBytes;

            // Cache it in memory for future requests
            variantParts
                    .computeIfAbsent(variant, k -> new HashMap<>())
                    .computeIfAbsent(type, k -> new HashMap<>())
                    .put(id, part);
            return part;

        } catch (Exception e) {
            log.error("[PartDataLoader][{}] Failed to load {}: {}", variant, baseName, e.getMessage());
            return null;
        }
    }



    /**
     * Load all parts of a specific type and cache them in HashMap
     * Returns list of loaded PartData objects
     */
    public static List<PartData> loadPartsByType(int zoomLv, byte type) {
        String variant = resolveVariant(zoomLv);
        String imageDir = BASE_DIR + "/" + variant + "/img";
        String dataDir = BASE_DIR + "/" + variant + "/data";

        List<PartData> loadedParts = new ArrayList<>();
        Path imageDirPath = Paths.get(imageDir);

        if (!Files.exists(imageDirPath)) {
            System.err.println("[PartDataLoader][" + variant + "] Image dir not found");
            return loadedParts;
        }

        System.out.println("[PartDataLoader] Loading type " + type + " for " + variant);

        try {
            String prefix = type + "_";
            Files.list(imageDirPath)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".png");
                    })
                    .forEach(imagePath -> {
                        String fileName = imagePath.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                        // Extract ID from filename: type_id.png
                        Matcher matcher = Pattern.compile("(\\d+)_(\\d+)").matcher(baseName);
                        if (!matcher.matches()) return;

                        byte fileType = Byte.parseByte(matcher.group(1));
                        short id = Short.parseShort(matcher.group(2));

                        Path dataPath = Paths.get(dataDir, baseName);

                        if (!Files.exists(dataPath)) {
                            System.err.println("[PartDataLoader][" + variant + "] Missing data file: " + baseName);
                            return;
                        }

                        try {
                            byte[] imageBytes = Files.readAllBytes(imagePath);
                            byte[] dataBytes = Files.readAllBytes(dataPath);

                            PartData part = new PartData();
                            part.type = fileType;
                            part.id = id;
                            part.image = imageBytes;
                            part.imageData = dataBytes;

                            // Cache in HashMap
                            variantParts
                                    .computeIfAbsent(variant, k -> new HashMap<>())
                                    .computeIfAbsent(fileType, k -> new HashMap<>())
                                    .put(id, part);

                            loadedParts.add(part);

                        } catch (Exception e) {
                            System.err.println("[PartDataLoader][" + variant + "] Failed to load " + baseName + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("[PartDataLoader][" + variant + "] Error reading directory: " + e.getMessage());
        }

        // Sort by ID for consistent ordering
        loadedParts.sort(Comparator.comparingInt(p -> p.id));

        System.out.println("[PartDataLoader] ✅ Loaded " + loadedParts.size() + " parts of type " + type);

        return loadedParts;
    }

    /**
     * Get all parts for a zoom level (loads entire variant if needed)
     */
    public static List<PartData> getAllByZoom(int zoomLv) {
        String variant = resolveVariant(zoomLv);
        Map<Byte, Map<Short, PartData>> byVariant = variantParts.get(variant);

        // If variant not loaded at all, load it completely
        if (byVariant == null || byVariant.isEmpty()) {
            loadVariant(variant);
            byVariant = variantParts.get(variant);
        }

        if (byVariant == null || byVariant.isEmpty()) {
            return Collections.emptyList();
        }

        List<PartData> allParts = new ArrayList<>();
        for (Map<Short, PartData> byType : byVariant.values()) {
            allParts.addAll(byType.values());
        }
        return allParts;
    }

    /**
     * Load entire variant (only use if you need all parts at once)
     */
    private static void loadVariant(String variant) {
        String imageDir = BASE_DIR + "/" + variant + "/img";
        String dataDir = BASE_DIR + "/" + variant + "/data";

        System.out.println("[PartDataLoader] ▶ Loading entire variant: " + variant);

        Path imageDirPath = Paths.get(imageDir);
        if (!Files.exists(imageDirPath)) {
            System.err.println("[PartDataLoader][" + variant + "] Image dir not found: " + imageDir);
            return;
        }

        try {
            Files.list(imageDirPath)
                    .filter(path -> path.toString().endsWith(".png"))
                    .forEach(imagePath -> loadPair(variant, imagePath, dataDir));
        } catch (IOException e) {
            System.err.println("[PartDataLoader][" + variant + "] Error reading images: " + e.getMessage());
        }
    }

    private static void loadPair(String variant, Path imagePath, String dataDir) {
        String fileName = imagePath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path dataPath = Paths.get(dataDir, baseName);

        if (!Files.exists(dataPath)) {
            System.err.println("[PartDataLoader][" + variant + "] Missing data file: " + baseName);
            return;
        }

        try {
            Matcher matcher = Pattern.compile("(\\d+)_(\\d+)").matcher(baseName);
            if (!matcher.matches()) {
                System.err.println("[PartDataLoader][" + variant + "] Invalid filename: " + baseName);
                return;
            }

            byte type = Byte.parseByte(matcher.group(1));
            short id = Short.parseShort(matcher.group(2));

            byte[] imageBytes = Files.readAllBytes(imagePath);
            byte[] dataBytes = Files.readAllBytes(dataPath);

            PartData part = new PartData();
            part.type = type;
            part.id = id;
            part.image = imageBytes;
            part.imageData = dataBytes;

            variantParts
                    .computeIfAbsent(variant, k -> new HashMap<>())
                    .computeIfAbsent(type, k -> new HashMap<>())
                    .put(id, part);

        } catch (Exception e) {
            System.err.println("[PartDataLoader][" + variant + "] Failed " + baseName + ": " + e.getMessage());
        }
    }

    /**
     * Invalidate a single part (type+id) across ALL zoom variants (x1-x4),
     * so the next getByZoom()/loadPartOnDemand() call re-reads fresh bytes from disk
     * instead of serving a stale cached PartData with mismatched image/dimensions.
     */
    public static void invalidate(byte type, short id) {
        for (Map<Byte, Map<Short, PartData>> byVariant : variantParts.values()) {
            Map<Short, PartData> byType = byVariant.get(type);
            if (byType != null) {
                byType.remove(id);
            }
        }
        log.info("[PartDataLoader] Invalidated type={} id={} across all variants", type, id);
    }

    /**
     * Clear cache for a specific variant to free memory
     */
    public static void clearVariant(String variant) {
        variantParts.remove(variant);
        System.out.println("[PartDataLoader] 🗑 Cleared cache for variant: " + variant);
    }

    /**
     * Clear all cached data
     */
    public static void clearAll() {
        variantParts.clear();
        System.out.println("[PartDataLoader] 🗑 Cleared all cached data");
    }

    /**
     * Get statistics about loaded parts
     */
    public static void printStats() {
        System.out.println("[PartDataLoader] 📊 Cache Statistics:");
        variantParts.forEach((variant, parts) -> {
            int count = parts.values().stream().mapToInt(Map::size).sum();
            System.out.printf("  ➜ %-3s : %d parts loaded%n", variant, count);
        });
    }

    public static int getPartIndex(int zoomLv) {
        return switch (zoomLv) {
            case 1 -> -1701;
            case 2 -> 2040;
            case 3 -> 12248;
            case 4 -> -21362;
            default -> -1;
        };
    }
}