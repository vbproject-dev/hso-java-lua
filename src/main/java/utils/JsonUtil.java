package utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


@Slf4j
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private static final Type MAP = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type LIST = new TypeToken<List<Object>>() {}.getType();

    private JsonUtil() {
    }

    public static JsonArray array(Object... values) {
        JsonArray array = new com.google.gson.JsonArray();
        for (Object value : values) {
            array.add(GSON.toJsonTree(value));
        }
        return array;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static Map<String, Object> toMap(String json) {
        return parse(json, MAP, LinkedHashMap::new);
    }

    public static List<Object> toList(String json) {
        return parse(json, LIST, ArrayList::new);
    }


    private static <T> T parse(String json, Type type, Supplier<T> empty) {
        if (json == null || json.isBlank()) return empty.get();
        try {
            T result = GSON.fromJson(json, type);
            return result != null ? result : empty.get(); // literal "null"
        } catch (JsonParseException e) {
            log.error("Cannot parse JSON as {}", type.getTypeName(), e);
            return empty.get();
        }
    }
}