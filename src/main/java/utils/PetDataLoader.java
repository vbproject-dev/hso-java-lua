package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class PetDataLoader {

    private static final Path PET_DATA_FOLDER = Path.of("data/pet_data");

    private static final Map<Integer, byte[]> CACHE = new HashMap<>();

    private static boolean loaded = false;

    private PetDataLoader() {
    }

    public static synchronized void loadAll() throws IOException {
        Map<Integer, byte[]> loadedData = new HashMap<>();

        try (Stream<Path> files = Files.list(PET_DATA_FOLDER)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                int petId;

                try {
                    petId = Integer.parseInt(path.getFileName().toString());
                } catch (NumberFormatException e) {
                    continue;
                }

                try (InputStream input = Files.newInputStream(path)) {
                    loadedData.put(petId, input.readAllBytes());
                }
            }
        }

        CACHE.clear();
        CACHE.putAll(loadedData);
        loaded = true;
    }

    public static Map<Integer, byte[]> getAll() {
        if (!loaded) {
            try {
                loadAll();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load pet binaries from " + PET_DATA_FOLDER, e);
            }
        }
        return CACHE;
    }
}