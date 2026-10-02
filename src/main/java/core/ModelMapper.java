package core;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.player.PlayerWear;
import model.player.PlayerWearMapper;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced Model Mapper with Generic Polymorphism Support
 */
@Slf4j
public class ModelMapper {

    private static final GsonBuilder gsonBuilder = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Gson gson;

    // Cache for reflection operations
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> SNAKE_CASE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> CAMEL_CASE_CACHE = new ConcurrentHashMap<>();

    // Registry for polymorphic types
    private static final Map<Class<?>, PolymorphicTypeRegistry<?>> POLYMORPHIC_REGISTRIES = new ConcurrentHashMap<>();

    static {
        rebuildGson();
    }

    /**
     * Registry for a polymorphic type
     */
    private static class PolymorphicTypeRegistry<T> {
        private final Class<T> baseClass;
        private final Map<String, Class<? extends T>> typeMap = new ConcurrentHashMap<>();
        private final Map<Class<? extends T>, String> reverseMap = new ConcurrentHashMap<>();
        @Getter
        @Setter
        private String discriminatorField = "type";

        public PolymorphicTypeRegistry(Class<T> baseClass) {
            this.baseClass = baseClass;
        }

        public void register(String typeKey, Class<? extends T> concreteClass) {
            typeMap.put(typeKey, concreteClass);
            reverseMap.put(concreteClass, typeKey);
        }

        public Class<? extends T> getConcreteClass(String typeKey) {
            return typeMap.get(typeKey);
        }

        public String getTypeKey(Class<? extends T> concreteClass) {
            return reverseMap.get(concreteClass);
        }

        public boolean isEmpty() {
            return typeMap.isEmpty();
        }
    }

    /**
     * Register polymorphic type for an interface/abstract class
     *
     * Usage:
     * ModelMapper.registerPolymorphicType(Item.class, "EQUIPMENT", EquipmentItem.class);
     * ModelMapper.registerPolymorphicType(Item.class, "POTION", PotionItem.class);
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerPolymorphicType(Class<T> baseClass, String typeKey, Class<? extends T> concreteClass) {
        PolymorphicTypeRegistry<T> registry = (PolymorphicTypeRegistry<T>)
                POLYMORPHIC_REGISTRIES.computeIfAbsent(baseClass, k -> new PolymorphicTypeRegistry<>(baseClass));

        registry.register(typeKey, concreteClass);

        // Rebuild Gson with new type adapter
        rebuildGson();

        log.debug("Registered polymorphic type: {} -> {} for {}", typeKey, concreteClass.getSimpleName(), baseClass.getSimpleName());
    }

    /**
     * Set custom discriminator field name for a polymorphic type
     * Default is "type"
     *
     * Usage:
     * ModelMapper.setDiscriminatorField(Item.class, "itemType");
     */
    @SuppressWarnings("unchecked")
    public static <T> void setDiscriminatorField(Class<T> baseClass, String fieldName) {
        PolymorphicTypeRegistry<T> registry = (PolymorphicTypeRegistry<T>)
                POLYMORPHIC_REGISTRIES.get(baseClass);

        if (registry != null) {
            registry.setDiscriminatorField(fieldName);
            rebuildGson();
        }
    }

    /**
     * Get concrete class from type discriminator
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getConcreteClass(Class<T> baseClass, String typeKey) {
        PolymorphicTypeRegistry<T> registry = (PolymorphicTypeRegistry<T>)
                POLYMORPHIC_REGISTRIES.get(baseClass);

        return registry != null ? registry.getConcreteClass(typeKey) : null;
    }

    /**
     * Get type key from concrete class
     */
    @SuppressWarnings("unchecked")
    public static <T> String getTypeKey(Class<T> baseClass, Class<? extends T> concreteClass) {
        PolymorphicTypeRegistry<T> registry = (PolymorphicTypeRegistry<T>)
                POLYMORPHIC_REGISTRIES.get(baseClass);

        return registry != null ? registry.getTypeKey(concreteClass) : null;
    }

    /**
     * Rebuild Gson instance with all registered polymorphic type adapters
     */
    private static void rebuildGson() {
        GsonBuilder builder = gsonBuilder;

        // Register type adapters for all polymorphic types
        for (Map.Entry<Class<?>, PolymorphicTypeRegistry<?>> entry : POLYMORPHIC_REGISTRIES.entrySet()) {
            Class<?> baseClass = entry.getKey();
            PolymorphicTypeRegistry<?> registry = entry.getValue();

            if (!registry.isEmpty()) {
                builder.registerTypeAdapter(baseClass, new PolymorphicTypeAdapter<>(registry));
            }
        }


        builder.registerTypeAdapter(PlayerWear.class, new PlayerWearMapper());

        gson = builder.create();
    }

    /**
     * Generic Polymorphic Type Adapter
     */
    private static class PolymorphicTypeAdapter<T> implements JsonDeserializer<T>, JsonSerializer<T> {
        private final PolymorphicTypeRegistry<T> registry;

        public PolymorphicTypeAdapter(PolymorphicTypeRegistry<T> registry) {
            this.registry = registry;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (!json.isJsonObject()) {
                return null;
            }

            JsonObject jsonObject = json.getAsJsonObject();
            String discriminatorField = registry.getDiscriminatorField();

            // Try camelCase and snake_case versions of discriminator field
            String camelCase = discriminatorField;
            String snakeCase = toSnakeCase(discriminatorField);

            String typeKey = null;
            if (jsonObject.has(camelCase)) {
                typeKey = jsonObject.get(camelCase).getAsString();
            } else if (jsonObject.has(snakeCase)) {
                typeKey = jsonObject.get(snakeCase).getAsString();
            }

            if (typeKey != null) {
                Class<? extends T> concreteClass = registry.getConcreteClass(typeKey);
                if (concreteClass != null) {
                    return context.deserialize(json, concreteClass);
                } else {
                    log.warn("Unknown type key '{}' for {}", typeKey, registry.baseClass.getSimpleName());
                }
            } else {
                log.warn("No discriminator field '{}' found in JSON for {}", discriminatorField, registry.baseClass.getSimpleName());
            }

            return null;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }

            // Serialize the actual object
            JsonElement element = context.serialize(src, src.getClass());

            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();

                // Get type key for this concrete class
                @SuppressWarnings("unchecked")
                String typeKey = registry.getTypeKey((Class<? extends T>) src.getClass());

                if (typeKey != null) {
                    // Add discriminator field
                    String discriminatorField = registry.getDiscriminatorField();
                    obj.addProperty(discriminatorField, typeKey);
                }
            }

            return element;
        }
    }

    /**
     * Convert Model to Map (excludes null by default for UPDATE operations)
     */
    public static <T> Map<String, Object> toMap(T model) {
        return toMap(model, true);
    }

    /**
     * Convert Model to Map with null handling option
     */
    public static <T> Map<String, Object> toMap(T model, boolean excludeNulls) {
        if (model == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        List<Field> fields = getCachedFields(model.getClass());

        for (Field field : fields) {
            try {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }

                Object value = field.get(model);

                if (excludeNulls && value == null) {
                    continue;
                }

                String columnName = toSnakeCase(field.getName());
                Object dbValue = toDbValue(value);

                map.put(columnName, dbValue);

            } catch (IllegalAccessException e) {
                log.debug("Cannot access field: {}", field.getName());
            } catch (Exception e) {
                log.error("Error processing field: {}", field.getName(), e);
            }
        }

        return map;
    }

    /**
     * Convert ResultSet single row to Model (with polymorphism support)
     */
    public static <T> T toModel(ResultSet rs, Class<T> modelClass) {
        return toModel(rs, modelClass, null);
    }

    /**
     * Convert ResultSet single row to Model with type discriminator field
     * @param typeField - the column name that contains the type discriminator (e.g., "item_type")
     */
    public static <T> T toModel(ResultSet rs, Class<T> modelClass, String typeField) {
        try {
            if (rs == null || !rs.next()) {
                return null;
            }

            return mapRow(rs, modelClass, typeField);

        } catch (Exception e) {
            log.error("Error converting ResultSet to model: {}", modelClass.getName(), e);
            return null;
        }
    }

    /**
     * Convert entire ResultSet to List
     */
    public static <T> List<T> toList(ResultSet rs, Class<T> modelClass) {
        return toList(rs, modelClass, null);
    }

    /**
     * Convert entire ResultSet to List with polymorphism support
     * @param typeField - the column name that contains the type discriminator
     */
    public static <T> List<T> toList(ResultSet rs, Class<T> modelClass, String typeField) {
        List<T> list = new ArrayList<>();

        try {
            if (rs == null) {
                return list;
            }

            while (rs.next()) {
                T instance = mapRow(rs, modelClass, typeField);
                if (instance != null) {
                    list.add(instance);
                }
            }

        } catch (Exception e) {
            log.error("Error converting ResultSet to list: {}", modelClass.getName(), e);
        }

        return list;
    }

    public static <T> T mapCurrentRow(ResultSet rs, Class<T> modelClass) {
        try {
            if (rs == null) {
                log.warn("ResultSet is null for class: {}", modelClass.getSimpleName());
                return null;
            }

            return mapRow(rs, modelClass, null);

        } catch (Exception e) {
            log.error("Error mapping current row to {}: {}",
                    modelClass.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }
    /**
     * Map single ResultSet row with polymorphism support
     */
    @SuppressWarnings("unchecked")
    private static <T> T mapRow(ResultSet rs, Class<T> modelClass, String typeField) throws Exception {
        // Check if we need polymorphic mapping
        Class<? extends T> concreteClass = modelClass;

        if (typeField != null && (modelClass.isInterface() || Modifier.isAbstract(modelClass.getModifiers()))) {
            PolymorphicTypeRegistry<T> registry = (PolymorphicTypeRegistry<T>)
                    POLYMORPHIC_REGISTRIES.get(modelClass);

            if (registry != null) {
                try {
                    String typeValue = rs.getString(typeField);
                    if (typeValue != null) {
                        Class<? extends T> registered = registry.getConcreteClass(typeValue);
                        if (registered != null) {
                            concreteClass = registered;
                        } else {
                            log.warn("No registered class for type: {} in {}", typeValue, modelClass.getName());
                            return null;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading type discriminator field: {}", typeField, e);
                }
            }
        }

        T instance = concreteClass.getDeclaredConstructor().newInstance();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String fieldName = toCamelCase(columnName);
            Object value = rs.getObject(i);

            setFieldWithType(instance, fieldName, value);
        }

        return instance;
    }

    /**
     * Serialize object to JSON string with polymorphism support
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return gson.toJson(obj);
    }

    /**
     * Deserialize JSON string to object of specified class
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            log.error("Error deserializing JSON to {}: {}", clazz.getName(), json, e);
            return null;
        }
    }

    /**
     * Deserialize JSON string to object with generic type (for List, Map, etc.)
     */
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("Error deserializing JSON to type {}: {}", type, json, e);
            return null;
        }
    }

    /**
     * Deserialize JSON string to List of objects
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("Error deserializing JSON list to {}: {}", clazz.getName(), json, e);
            return new ArrayList<>();
        }
    }

    /**
     * Deserialize JSON string to Map
     */
    public static <K, V> Map<K, V> fromJsonMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Type type = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("Error deserializing JSON map: {}", json, e);
            return new HashMap<>();
        }
    }

    /**
     * Get table name from model class
     */
    public static String getTableName(Class<?> modelClass) {
        return toSnakeCase(modelClass.getSimpleName());
    }

    /**
     * Convert value to database format
     */
    private static Object toDbValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }

        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }

        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof byte[]) {
            return gson.toJson(value);
        }

        if (value.getClass().isArray()) {
            return gson.toJson(value);
        }

        if (value instanceof Collection || value instanceof Map || isCustomObject(value)) {
            return gson.toJson(value);
        }

        return value.toString();
    }

    public static void setField(Object instance, String fieldName, Object value) {
        setFieldWithType(instance, fieldName, value);
    }

    private static void setFieldWithType(Object instance, String fieldName, Object value) {
        try {
            Field field = findField(instance.getClass(), fieldName);
            if (field == null) {
                return;
            }

            try {
                field.setAccessible(true);
            } catch (Exception e) {
                log.debug("Cannot make field accessible: {}.{}", instance.getClass().getName(), fieldName);
                return;
            }

            Class<?> fieldType = field.getType();
            Type genericType = field.getGenericType();

            if (value == null) {
                field.set(instance, null);
                return;
            }

            if (fieldType.isAssignableFrom(value.getClass())) {
                field.set(instance, value);
                return;
            }

            Object converted = fromDbValue(value, fieldType, genericType);
            field.set(instance, converted);

        } catch (InaccessibleObjectException e) {
            log.debug("Module access denied for field '{}' in {}", fieldName, instance.getClass().getName());
        } catch (Exception e) {
            log.debug("Could not set field '{}' = {}: {}", fieldName, value, e.getMessage());
        }
    }

    public static Object getField(Object instance, String fieldName) {
        try {
            Field field = findField(instance.getClass(), fieldName);
            if (field == null) {
                return null;
            }

            try {
                field.setAccessible(true);
            } catch (Exception e) {
                log.debug("Cannot make field accessible: {}.{}", instance.getClass().getName(), fieldName);
                return null;
            }

            return field.get(instance);

        } catch (InaccessibleObjectException e) {
            log.debug("Module access denied for field '{}'", fieldName);
            return null;
        } catch (Exception e) {
            log.debug("Could not get field '{}'", fieldName);
            return null;
        }
    }

    private static Object fromDbValue(Object value, Class<?> targetType, Type genericType) {
        if (value == null) {
            return null;
        }

        String valueStr = value.toString();

        if (targetType == int.class || targetType == Integer.class) {
            return parseIntSafe(valueStr);
        }
        if (targetType == long.class || targetType == Long.class) {
            return parseLongSafe(valueStr);
        }
        if (targetType == double.class || targetType == Double.class) {
            return parseDoubleSafe(valueStr);
        }
        if (targetType == float.class || targetType == Float.class) {
            return parseFloatSafe(valueStr);
        }
        if (targetType == short.class || targetType == Short.class) {
            return parseShortSafe(valueStr);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return parseByteSafe(valueStr);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(valueStr) || "1".equals(valueStr);
        }

        if (targetType == LocalDateTime.class && value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }

        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, valueStr);
        }

        if (targetType == byte[].class && value instanceof String && isJsonString(valueStr)) {
            try {
                return gson.fromJson(valueStr, byte[].class);
            } catch (JsonSyntaxException e) {
                log.debug("Failed to parse JSON byte array: {}", e.getMessage());
            }
        }

        if (targetType.isArray() && value instanceof String && isJsonString(valueStr)) {
            try {
                return gson.fromJson(valueStr, targetType);
            } catch (JsonSyntaxException e) {
                log.debug("Failed to parse JSON array for type {}: {}", targetType.getName(), e.getMessage());
            }
        }

        if (value instanceof String && isJsonString(valueStr)) {
            try {
                if (genericType instanceof ParameterizedType) {
                    return gson.fromJson(valueStr, genericType);
                }
                return gson.fromJson(valueStr, targetType);
            } catch (JsonSyntaxException e) {
                log.debug("Failed to parse JSON for type {}: {}", targetType.getName(), e.getMessage());
            }
        }

        return value;
    }

    private static Integer parseIntSafe(String str) {
        try { return Integer.parseInt(str); } catch (Exception e) { return 0; }
    }

    private static Long parseLongSafe(String str) {
        try { return Long.parseLong(str); } catch (Exception e) { return 0L; }
    }

    private static Double parseDoubleSafe(String str) {
        try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; }
    }

    private static Float parseFloatSafe(String str) {
        try { return Float.parseFloat(str); } catch (Exception e) { return 0.0f; }
    }

    private static Short parseShortSafe(String str) {
        try { return Short.parseShort(str); } catch (Exception e) { return (short) 0; }
    }

    private static Byte parseByteSafe(String str) {
        try { return Byte.parseByte(str); } catch (Exception e) { return (byte) 0; }
    }

    private static boolean isCustomObject(Object value) {
        Class<?> clazz = value.getClass();
        Package pkg = clazz.getPackage();

        if (pkg == null) return false;

        String packageName = pkg.getName();
        return !packageName.startsWith("java.") && !packageName.startsWith("javax.");
    }

    private static boolean isJsonString(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) ||
                (str.startsWith("[") && str.endsWith("]"));
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            if (isJdkClass(clazz)) {
                return null;
            }

            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null && !isJdkClass(clazz.getSuperclass())) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        } catch (Exception e) {
            log.debug("Cannot find field: {}.{}", clazz.getName(), fieldName);
            return null;
        }
    }

    private static List<Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = clazz;

            while (current != null && current != Object.class) {
                if (!isJdkClass(current)) {
                    try {
                        Field[] declaredFields = current.getDeclaredFields();
                        for (Field field : declaredFields) {
                            try {
                                field.setAccessible(true);
                                fields.add(field);
                            } catch (Exception e) {
                                log.debug("Cannot access field: {}.{}", current.getName(), field.getName());
                            }
                        }
                    } catch (SecurityException e) {
                        log.debug("Cannot access fields of class: {}", current.getName());
                    }
                }
                current = current.getSuperclass();
            }

            return fields;
        });
    }

    private static boolean isJdkClass(Class<?> clazz) {
        if (clazz == null) return false;
        String className = clazz.getName();
        return className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("jdk.");
    }

    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        return SNAKE_CASE_CACHE.computeIfAbsent(camelCase,
                k -> k.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase()
        );
    }

    private static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        return CAMEL_CASE_CACHE.computeIfAbsent(snakeCase, k -> {
            String[] parts = k.toLowerCase().split("_");
            StringBuilder camelCase = new StringBuilder(parts[0]);

            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    camelCase.append(parts[i].substring(0, 1).toUpperCase());
                    camelCase.append(parts[i].substring(1));
                }
            }

            return camelCase.toString();
        });
    }

    public static String whereClause(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "1=1";
        }

        return conditions.keySet().stream()
                .map(key -> key + " = ?")
                .collect(Collectors.joining(" AND "));
    }

    public static Object[] whereParams(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new Object[0];
        }

        return conditions.values().toArray();
    }

    public static void clearCache() {
        FIELD_CACHE.clear();
        SNAKE_CASE_CACHE.clear();
        CAMEL_CASE_CACHE.clear();
        POLYMORPHIC_REGISTRIES.clear();
        rebuildGson();
        log.info("ModelMapper cache cleared");
    }
}

/*
                              FOR POLYMORP
        ModelMapper.registerPolymorphicType(Item.class, "EQUIPMENT", EquipmentItem.class);
        ModelMapper.registerPolymorphicType(Item.class, "POTION", PotionItem.class);
        ModelMapper.registerPolymorphicType(Item.class, "MATERIAL", MaterialItem.class);
        ModelMapper.setDiscriminatorField(Item.class, "category");

 */