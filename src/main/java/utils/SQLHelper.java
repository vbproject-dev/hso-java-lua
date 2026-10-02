package utils;

import core.ModelMapper;
import core.SQL;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class SQLHelper {

    // ============= FUNCTIONAL INTERFACE =============

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ============= MODEL TO MAP CONVERTER =============

    private static Map<String, Object> modelToMap(Object model) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = model.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object value = field.get(model);

                // Skip null values and static/transient fields
                if (value != null &&
                        !java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {

                    // Convert camelCase to snake_case for database columns
                    String columnName = camelToSnake(fieldName);
                    map.put(columnName, value);
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field: " + field.getName(), e);
            }
        }

        return map;
    }

    private static Map<String, Object> modelToMapWithNulls(Object model) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = model.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object value = field.get(model);

                // Skip static/transient fields only
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {

                    String columnName = camelToSnake(fieldName);
                    map.put(columnName, value);
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field: " + field.getName(), e);
            }
        }

        return map;
    }

    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // ============= BASE WHERE BUILDER =============

    public static abstract class WhereBuilder<T extends WhereBuilder<T>> {
        protected final List<String> whereClauses = new ArrayList<>();
        protected final List<Object> whereParams = new ArrayList<>();

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T where(String column, Object value) {
            return where(column, "=", value);
        }

        public T where(String column, String operator, Object value) {
            if (whereClauses.isEmpty()) {
                whereClauses.add(column + " " + operator + " ?");
            } else {
                whereClauses.add("AND " + column + " " + operator + " ?");
            }
            whereParams.add(value);
            return self();
        }

        public T whereRaw(String clause, Object... params) {
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            Collections.addAll(whereParams, params);
            return self();
        }

        public T and(String column, Object value) {
            return where(column, "=", value);
        }

        public T and(String column, String operator, Object value) {
            whereClauses.add("AND " + column + " " + operator + " ?");
            whereParams.add(value);
            return self();
        }

        public T or(String column, Object value) {
            return or(column, "=", value);
        }

        public T or(String column, String operator, Object value) {
            whereClauses.add("OR " + column + " " + operator + " ?");
            whereParams.add(value);
            return self();
        }

        public T whereGroup(Function<T, T> group) {
            whereClauses.add("AND (");
            group.apply(self());
            whereClauses.add(")");
            return self();
        }

        public T orWhereGroup(Function<T, T> group) {
            whereClauses.add("OR (");
            group.apply(self());
            whereClauses.add(")");
            return self();
        }

        public T whereIn(String column, Object... values) {
            return whereIn(column, Arrays.asList(values));
        }

        public T whereIn(String column, Collection<?> values) {
            if (values.isEmpty()) return self();

            String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
            String clause = column + " IN (" + placeholders + ")";

            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.addAll(values);
            return self();
        }

        public T whereNotIn(String column, Object... values) {
            return whereNotIn(column, Arrays.asList(values));
        }

        public T whereNotIn(String column, Collection<?> values) {
            if (values.isEmpty()) return self();

            String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
            String clause = column + " NOT IN (" + placeholders + ")";

            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.addAll(values);
            return self();
        }

        public T whereBetween(String column, Object start, Object end) {
            String clause = column + " BETWEEN ? AND ?";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.add(start);
            whereParams.add(end);
            return self();
        }

        public T whereNotBetween(String column, Object start, Object end) {
            String clause = column + " NOT BETWEEN ? AND ?";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.add(start);
            whereParams.add(end);
            return self();
        }

        public T whereNull(String column) {
            String clause = column + " IS NULL";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            return self();
        }

        public T whereNotNull(String column) {
            String clause = column + " IS NOT NULL";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            return self();
        }

        public T whereLike(String column, String pattern) {
            String clause = column + " LIKE ?";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.add(pattern);
            return self();
        }

        public T whereNotLike(String column, String pattern) {
            String clause = column + " NOT LIKE ?";
            if (whereClauses.isEmpty()) {
                whereClauses.add(clause);
            } else {
                whereClauses.add("AND " + clause);
            }
            whereParams.add(pattern);
            return self();
        }

        public T whereDate(String column, Object date) {
            return where("DATE(" + column + ")", "=", date);
        }

        public T whereYear(String column, int year) {
            return whereRaw("YEAR(" + column + ") = ?", year);
        }

        public T whereMonth(String column, int month) {
            return whereRaw("MONTH(" + column + ") = ?", month);
        }

        public T whereDay(String column, int day) {
            return whereRaw("DAY(" + column + ") = ?", day);
        }

        protected String buildWhereClause() {
            if (whereClauses.isEmpty()) {
                return null;
            }
            return String.join(" ", whereClauses);
        }
    }

    // ============= UPDATE BUILDER WITH MODEL SUPPORT =============

    @Getter
    public static class UpdateBuilder extends WhereBuilder<UpdateBuilder> {
        private String table;
        private final Map<String, Object> updates = new HashMap<>();
        private boolean useRawSql = false;

        public UpdateBuilder table(String table) {
            this.table = table;
            return this;
        }

        public UpdateBuilder set(String column, Object value) {
            updates.put(column, value);
            return this;
        }

        public UpdateBuilder set(Map<String, Object> values) {
            updates.putAll(values);
            return this;
        }

        // NEW: Set from model class
        public UpdateBuilder setFromModel(Object model) {
            return setFromModel(model, false);
        }

        public UpdateBuilder setFromModel(Object model, boolean includeNulls) {
            Map<String, Object> modelMap = includeNulls ?
                    modelToMapWithNulls(model) : modelToMap(model);
            updates.putAll(modelMap);
            return this;
        }

        // NEW: Set specific fields from model
        public UpdateBuilder setFromModel(Object model, String... fields) {
            Map<String, Object> modelMap = modelToMap(model);
            for (String field : fields) {
                String column = camelToSnake(field);
                if (modelMap.containsKey(column)) {
                    updates.put(column, modelMap.get(column));
                }
            }
            return this;
        }

        // NEW: Exclude specific fields when setting from model
        public UpdateBuilder setFromModelExcept(Object model, String... excludeFields) {
            Map<String, Object> modelMap = modelToMap(model);
            Set<String> excluded = new HashSet<>();
            for (String field : excludeFields) {
                excluded.add(camelToSnake(field));
            }

            for (Map.Entry<String, Object> entry : modelMap.entrySet()) {
                if (!excluded.contains(entry.getKey())) {
                    updates.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public UpdateBuilder increment(String column) {
            return increment(column, 1);
        }

        public UpdateBuilder increment(String column, Number value) {
            updates.put(column, "RAW:" + column + " + " + value);
            useRawSql = true;
            return this;
        }

        public UpdateBuilder decrement(String column) {
            return decrement(column, 1);
        }

        public UpdateBuilder decrement(String column, Number value) {
            updates.put(column, "RAW:" + column + " - " + value);
            useRawSql = true;
            return this;
        }

        public UpdateBuilder multiply(String column, Number value) {
            updates.put(column, "RAW:" + column + " * " + value);
            useRawSql = true;
            return this;
        }

        public UpdateBuilder setIf(boolean condition, String column, Object value) {
            if (condition) {
                set(column, value);
            }
            return this;
        }

        public boolean execute() {
            String whereClause = buildWhereClause();
            return SQL.gI().updateColumns(table, updates, whereClause, whereParams.toArray());
        }
    }

    public static UpdateBuilder update(String table) {
        return new UpdateBuilder().table(table);
    }

    public static UpdateBuilder update() {
        return new UpdateBuilder();
    }

    // ============= INSERT BUILDER WITH MODEL SUPPORT =============

    @Getter
    public static class InsertBuilder {
        private String table;
        private final Map<String, Object> values = new HashMap<>();
        private boolean ignore = false;
        private String onDuplicateKey;

        public InsertBuilder into(String table) {
            this.table = table;
            return this;
        }

        public InsertBuilder value(String column, Object value) {
            values.put(column, value);
            return this;
        }

        public InsertBuilder values(Map<String, Object> values) {
            this.values.putAll(values);
            return this;
        }

        // NEW: Insert from model class
        public InsertBuilder fromModel(Object model) {
            return fromModel(model, false);
        }

        public InsertBuilder fromModel(Object model, boolean includeNulls) {
            Map<String, Object> modelMap = includeNulls ?
                    modelToMapWithNulls(model) : modelToMap(model);
            this.values.putAll(modelMap);
            return this;
        }

        // NEW: Insert specific fields from model
        public InsertBuilder fromModel(Object model, String... fields) {
            Map<String, Object> modelMap = modelToMap(model);
            for (String field : fields) {
                String column = camelToSnake(field);
                if (modelMap.containsKey(column)) {
                    values.put(column, modelMap.get(column));
                }
            }
            return this;
        }

        // NEW: Exclude specific fields when inserting from model
        public InsertBuilder fromModelExcept(Object model, String... excludeFields) {
            Map<String, Object> modelMap = modelToMap(model);
            Set<String> excluded = new HashSet<>();
            for (String field : excludeFields) {
                excluded.add(camelToSnake(field));
            }

            for (Map.Entry<String, Object> entry : modelMap.entrySet()) {
                if (!excluded.contains(entry.getKey())) {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public InsertBuilder fromJSON(JSONObject json) {
            for (Object key : json.keySet()) {
                String column = key.toString();
                Object value = json.get(key);
                values.put(column, value);
            }
            return this;
        }

        public InsertBuilder valueJSON(String column, JSONObject json) {
            values.put(column, json.toJSONString());
            return this;
        }

        public InsertBuilder valueJSON(String column, JSONArray json) {
            values.put(column, json.toJSONString());
            return this;
        }

        public InsertBuilder valueIf(boolean condition, String column, Object value) {
            if (condition) {
                value(column, value);
            }
            return this;
        }

        public InsertBuilder orIgnore() {
            this.ignore = true;
            return this;
        }

        public InsertBuilder orUpdate(String... columns) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(columns[i]).append(" = VALUES(").append(columns[i]).append(")");
            }
            this.onDuplicateKey = sb.toString();
            return this;
        }

        public long execute() {
            return SQL.gI().insertInto(table, values);
        }

        public boolean executeAndCheck() {
            return execute() >= 0;
        }

        // NEW: Batch insert from model list
        public List<Long> executeBatchFromModels(List<?> models) {
            List<Long> ids = new ArrayList<>();
            for (Object model : models) {
                Map<String, Object> modelMap = modelToMap(model);
                long id = SQL.gI().insertInto(table, modelMap);
                ids.add(id);
            }
            return ids;
        }

        public List<Long> executeBatch(List<Map<String, Object>> batchValues) {
            List<Long> ids = new ArrayList<>();
            for (Map<String, Object> vals : batchValues) {
                long id = SQL.gI().insertInto(table, vals);
                ids.add(id);
            }
            return ids;
        }

        public List<Long> executeBatchJSON(JSONArray jsonArray) {
            List<Long> ids = new ArrayList<>();
            for (Object obj : jsonArray) {
                if (obj instanceof JSONObject) {
                    InsertBuilder builder = SQLHelper.insert(table).fromJSON((JSONObject) obj);
                    long id = builder.execute();
                    ids.add(id);
                }
            }
            return ids;
        }
    }

    public static InsertBuilder insert() {
        return new InsertBuilder();
    }

    public static InsertBuilder insert(String table) {
        return new InsertBuilder().into(table);
    }

    // NEW: Quick insert from model
    public static long insertModel(String table, Object model) {
        return insert(table).fromModel(model).execute();
    }

    public static boolean insertModelAndCheck(String table, Object model) {
        return insertModel(table, model) >= 0;
    }

    // ============= DELETE BUILDER =============

    @Getter
    public static class DeleteBuilder extends WhereBuilder<DeleteBuilder> {
        private String table;
        private Integer limit;

        public DeleteBuilder from(String table) {
            this.table = table;
            return this;
        }

        public DeleteBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public boolean execute() {
            String whereClause = buildWhereClause();
            return SQL.gI().delete(table, whereClause, whereParams.toArray());
        }

        public boolean softDelete(String deletedAtColumn) {
            return SQLHelper.update(table)
                    .set(deletedAtColumn, System.currentTimeMillis())
                    .whereRaw(buildWhereClause(), whereParams.toArray())
                    .execute();
        }
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder();
    }

    public static DeleteBuilder delete(String table) {
        return new DeleteBuilder().from(table);
    }

    // ============= SELECT BUILDER =============

    @Getter
    public static class SelectBuilder extends WhereBuilder<SelectBuilder> {
        private String table;
        private final List<String> columns = new ArrayList<>();
        private final List<String> joins = new ArrayList<>();
        private String orderBy;
        private Integer limit;
        private Integer offset;
        private String groupBy;
        private String having;
        private boolean distinct = false;

        public SelectBuilder from(String table) {
            this.table = table;
            return this;
        }

        public SelectBuilder select(String... columns) {
            Collections.addAll(this.columns, columns);
            return this;
        }

        public SelectBuilder distinct() {
            this.distinct = true;
            return this;
        }

        public SelectBuilder join(String table, String condition) {
            joins.add("JOIN " + table + " ON " + condition);
            return this;
        }

        public SelectBuilder leftJoin(String table, String condition) {
            joins.add("LEFT JOIN " + table + " ON " + condition);
            return this;
        }

        public SelectBuilder rightJoin(String table, String condition) {
            joins.add("RIGHT JOIN " + table + " ON " + condition);
            return this;
        }

        public SelectBuilder innerJoin(String table, String condition) {
            joins.add("INNER JOIN " + table + " ON " + condition);
            return this;
        }

        public SelectBuilder orderBy(String column) {
            this.orderBy = column + " ASC";
            return this;
        }

        public SelectBuilder orderBy(String column, String direction) {
            this.orderBy = column + " " + direction.toUpperCase();
            return this;
        }

        public SelectBuilder orderByAsc(String column) {
            return orderBy(column, "ASC");
        }

        public SelectBuilder orderByDesc(String column) {
            return orderBy(column, "DESC");
        }

        public SelectBuilder orderByRaw(String rawOrder) {
            this.orderBy = rawOrder;
            return this;
        }

        public SelectBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public SelectBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public SelectBuilder paginate(int page, int perPage) {
            this.limit = perPage;
            this.offset = (page - 1) * perPage;
            return this;
        }

        public SelectBuilder groupBy(String groupBy) {
            this.groupBy = groupBy;
            return this;
        }

        public SelectBuilder having(String having) {
            this.having = having;
            return this;
        }

        private String buildSQL() {
            StringBuilder sql = new StringBuilder("SELECT ");

            if (distinct) {
                sql.append("DISTINCT ");
            }

            if (columns.isEmpty()) {
                sql.append("*");
            } else {
                sql.append(String.join(", ", columns));
            }

            sql.append(" FROM ").append(table);

            for (String join : joins) {
                sql.append(" ").append(join);
            }

            String whereClause = buildWhereClause();
            if (whereClause != null && !whereClause.isEmpty()) {
                sql.append(" WHERE ").append(whereClause);
            }

            if (groupBy != null && !groupBy.isEmpty()) {
                sql.append(" GROUP BY ").append(groupBy);
            }

            if (having != null && !having.isEmpty()) {
                sql.append(" HAVING ").append(having);
            }

            if (orderBy != null && !orderBy.isEmpty()) {
                sql.append(" ORDER BY ").append(orderBy);
            }

            if (limit != null) {
                sql.append(" LIMIT ").append(limit);
            }

            if (offset != null) {
                sql.append(" OFFSET ").append(offset);
            }

            return sql.toString();
        }

        public Object value(String column) {
            SelectBuilder builder = new SelectBuilder()
                    .from(table)
                    .select(column)
                    .limit(1);
            builder.whereClauses.addAll(this.whereClauses);
            builder.whereParams.addAll(this.whereParams);
            builder.joins.addAll(this.joins);

            String sql = builder.buildSQL();
            return SQL.gI().selectOne(sql, rs -> rs.getObject(column), builder.whereParams.toArray());
        }

        public int count() {
            return count("*");
        }

        public int count(String column) {
            SelectBuilder countBuilder = new SelectBuilder()
                    .from(table)
                    .select("COUNT(" + column + ") as count");
            countBuilder.whereClauses.addAll(this.whereClauses);
            countBuilder.whereParams.addAll(this.whereParams);
            countBuilder.joins.addAll(this.joins);

            String sql = countBuilder.buildSQL();
            Integer result = SQL.gI().selectOne(sql, rs -> rs.getInt("count"), countBuilder.whereParams.toArray());
            return result != null ? result : 0;
        }

        public double sum(String column) {
            return aggregate("SUM", column);
        }

        public double avg(String column) {
            return aggregate("AVG", column);
        }

        public double max(String column) {
            return aggregate("MAX", column);
        }

        public double min(String column) {
            return aggregate("MIN", column);
        }

        private double aggregate(String function, String column) {
            SelectBuilder aggBuilder = new SelectBuilder()
                    .from(table)
                    .select(function + "(" + column + ") as result");
            aggBuilder.whereClauses.addAll(this.whereClauses);
            aggBuilder.whereParams.addAll(this.whereParams);

            String sql = aggBuilder.buildSQL();
            Double result = SQL.gI().selectOne(sql, rs -> rs.getDouble("result"), aggBuilder.whereParams.toArray());
            return result != null ? result : 0.0;
        }

        public boolean exists() {
            return count() > 0;
        }

        public <T> List<T> get(ResultSetMapper<T> mapper) {
            String sql = buildSQL();
            log.debug("Executing SQL: {}", sql);
            List<T> result = SQL.gI().selectList(sql, mapper::map, whereParams.toArray());
            return result != null ? result : new ArrayList<>();
        }

        public <T> List<T> getAsModel(Class<T> modelClass) {
            return get(rs -> ModelMapper.mapCurrentRow(rs, modelClass));
        }

        public <T> T firstAs(ResultSetMapper<T> mapper) {
            SelectBuilder builder = new SelectBuilder()
                    .from(table)
                    .limit(1);
            builder.columns.addAll(this.columns);
            builder.whereClauses.addAll(this.whereClauses);
            builder.whereParams.addAll(this.whereParams);
            builder.joins.addAll(this.joins);
            builder.orderBy = this.orderBy;

            String sql = builder.buildSQL();
            return SQL.gI().selectOne(sql, rs -> mapper.map(rs), builder.whereParams.toArray());
        }

        public <T> T firstAsModel(Class<T> modelClass) {
            return firstAs(rs -> ModelMapper.mapCurrentRow(rs, modelClass));
        }

        public JSONArray getJSON() {
            String sql = buildSQL();
            log.debug("Executing SQL: {}", sql);

            JSONArray result = SQL.gI().select(sql, rs -> {
                JSONArray jsonArray = new JSONArray();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    JSONObject jsonObject = new JSONObject();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        jsonObject.put(columnName, value);
                    }
                    jsonArray.add(jsonObject);
                }
                return jsonArray;
            }, whereParams.toArray());

            return result != null ? result : new JSONArray();
        }

        public JSONObject firstJSON() {
            SelectBuilder builder = new SelectBuilder()
                    .from(table)
                    .limit(1);
            builder.columns.addAll(this.columns);
            builder.whereClauses.addAll(this.whereClauses);
            builder.whereParams.addAll(this.whereParams);
            builder.joins.addAll(this.joins);

            String sql = builder.buildSQL();

            return SQL.gI().selectOne(sql, rs -> {
                JSONObject jsonObject = new JSONObject();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    jsonObject.put(columnName, value);
                }
                return jsonObject;
            }, builder.whereParams.toArray());
        }

        public JSONObject getJSONObject(String column) {
            SelectBuilder builder = new SelectBuilder()
                    .from(table)
                    .select(column)
                    .limit(1);
            builder.whereClauses.addAll(this.whereClauses);
            builder.whereParams.addAll(this.whereParams);

            String sql = builder.buildSQL();
            String jsonString = SQL.gI().selectOne(sql, rs -> rs.getString(column), builder.whereParams.toArray());
            return parseJSON(jsonString);
        }

        public JSONArray getJSONArray(String column) {
            SelectBuilder builder = new SelectBuilder()
                    .from(table)
                    .select(column)
                    .limit(1);
            builder.whereClauses.addAll(this.whereClauses);
            builder.whereParams.addAll(this.whereParams);

            String sql = builder.buildSQL();
            String jsonString = SQL.gI().selectOne(sql, rs -> rs.getString(column), builder.whereParams.toArray());
            return parseJSONArray(jsonString);
        }
    }

    // ============= SELECT HELPER METHODS =============

    public static SelectBuilder select(String... columns) {
        SelectBuilder builder = new SelectBuilder();
        if (columns.length > 0) {
            builder.select(columns);
        }
        return builder;
    }

    public static SelectBuilder selectFrom(String table) {
        return new SelectBuilder().from(table);
    }

    public static boolean tableExists(String tableName) {
        return selectFrom(tableName).limit(1).exists();
    }

    // ============= JSON HELPER METHODS =============

    public static JSONObject resultSetToJSON(ResultSet rs) throws SQLException {
        JSONObject json = new JSONObject();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = rs.getObject(i);
            json.put(columnName, value);
        }
        return json;
    }

    public static JSONObject parseJSON(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(jsonString);
        } catch (Exception e) {
            log.error("Error parsing JSON string", e);
            return null;
        }
    }

    public static JSONArray parseJSONArray(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(jsonString);
        } catch (Exception e) {
            log.error("Error parsing JSON array string", e);
            return null;
        }
    }

    // ============= JSON MODIFIER CLASS =============

    public static class JSONModifier {
        private final String table;
        private final String jsonColumn;
        private final String whereClause;
        private final Object[] whereParams;

        private JSONModifier(String table, String jsonColumn, String whereClause, Object[] whereParams) {
            this.table = table;
            this.jsonColumn = jsonColumn;
            this.whereClause = whereClause;
            this.whereParams = whereParams;
        }

        public boolean set(String key, Object value) {
            return modifyJSON(json -> {
                json.put(key, value);
                return json;
            });
        }

        public boolean setNested(String path, Object value) {
            return modifyJSON(json -> {
                String[] keys = path.split("\\.");
                JSONObject current = json;

                for (int i = 0; i < keys.length - 1; i++) {
                    Object obj = current.get(keys[i]);
                    if (obj instanceof JSONObject) {
                        current = (JSONObject) obj;
                    } else {
                        JSONObject newObj = new JSONObject();
                        current.put(keys[i], newObj);
                        current = newObj;
                    }
                }

                current.put(keys[keys.length - 1], value);
                return json;
            });
        }

        public boolean remove(String key) {
            return modifyJSON(json -> {
                json.remove(key);
                return json;
            });
        }

        public boolean removeNested(String path) {
            return modifyJSON(json -> {
                String[] keys = path.split("\\.");
                JSONObject current = json;

                for (int i = 0; i < keys.length - 1; i++) {
                    Object obj = current.get(keys[i]);
                    if (obj instanceof JSONObject) {
                        current = (JSONObject) obj;
                    } else {
                        return json;
                    }
                }

                current.remove(keys[keys.length - 1]);
                return json;
            });
        }

        public boolean merge(JSONObject data) {
            return modifyJSON(json -> {
                json.putAll(data);
                return json;
            });
        }

        public boolean increment(String key, Number amount) {
            return modifyJSON(json -> {
                Object current = json.get(key);
                if (current instanceof Number) {
                    double newValue = ((Number) current).doubleValue() + amount.doubleValue();
                    json.put(key, newValue);
                } else {
                    json.put(key, amount);
                }
                return json;
            });
        }

        public boolean decrement(String key, Number amount) {
            return increment(key, -amount.doubleValue());
        }

        public boolean toggle(String key) {
            return modifyJSON(json -> {
                Object current = json.get(key);
                if (current instanceof Boolean) {
                    json.put(key, !(Boolean) current);
                } else {
                    json.put(key, true);
                }
                return json;
            });
        }

        public boolean arrayPush(String arrayKey, Object item) {
            return modifyJSON(json -> {
                Object arr = json.get(arrayKey);
                JSONArray array;

                if (arr instanceof JSONArray) {
                    array = (JSONArray) arr;
                } else if (arr instanceof String) {
                    array = parseJSONArray((String) arr);
                    if (array == null) array = new JSONArray();
                } else {
                    array = new JSONArray();
                }

                array.add(item);
                json.put(arrayKey, array);
                return json;
            });
        }

        public boolean arrayRemove(String arrayKey, Object item) {
            return modifyJSON(json -> {
                Object arr = json.get(arrayKey);
                if (arr instanceof JSONArray) {
                    ((JSONArray) arr).remove(item);
                } else if (arr instanceof String) {
                    JSONArray array = parseJSONArray((String) arr);
                    if (array != null) {
                        array.remove(item);
                        json.put(arrayKey, array);
                    }
                }
                return json;
            });
        }

        public boolean arrayRemoveAt(String arrayKey, int index) {
            return modifyJSON(json -> {
                Object arr = json.get(arrayKey);
                if (arr instanceof JSONArray) {
                    ((JSONArray) arr).remove(index);
                } else if (arr instanceof String) {
                    JSONArray array = parseJSONArray((String) arr);
                    if (array != null && index >= 0 && index < array.size()) {
                        array.remove(index);
                        json.put(arrayKey, array);
                    }
                }
                return json;
            });
        }

        public boolean arrayClear(String arrayKey) {
            return modifyJSON(json -> {
                json.put(arrayKey, new JSONArray());
                return json;
            });
        }

        public boolean has(String key) {
            JSONObject json = getJSONObject();
            return json != null && json.containsKey(key);
        }

        private JSONObject getJSONObject() {
            String sql = "SELECT " + jsonColumn + " FROM " + table + " WHERE " + whereClause;
            String jsonStr = SQL.gI().selectOne(sql, rs -> rs.getString(jsonColumn), whereParams);
            return parseJSON(jsonStr);
        }

        private boolean modifyJSON(Function<JSONObject, JSONObject> modifier) {
            JSONObject json = getJSONObject();
            if (json == null) {
                json = new JSONObject();
            }

            JSONObject modified = modifier.apply(json);

            return SQLHelper.update(table)
                    .set(jsonColumn, modified.toJSONString())
                    .whereRaw(whereClause, whereParams)
                    .execute();
        }
    }

    public static JSONModifier modifyJSON(String table, String jsonColumn, String whereClause, Object... whereParams) {
        return new JSONModifier(table, jsonColumn, whereClause, whereParams);
    }

    // ============= JSON ARRAY MODIFIER CLASS =============

    public static class JSONArrayModifier {
        private final String table;
        private final String jsonColumn;
        private final String whereClause;
        private final Object[] whereParams;

        private JSONArrayModifier(String table, String jsonColumn, String whereClause, Object[] whereParams) {
            this.table = table;
            this.jsonColumn = jsonColumn;
            this.whereClause = whereClause;
            this.whereParams = whereParams;
        }

        public boolean push(Object item) {
            return modifyArray(array -> {
                array.add(item);
                return array;
            });
        }

        public boolean pushAll(Object... items) {
            return modifyArray(array -> {
                Collections.addAll(array, items);
                return array;
            });
        }

        public boolean remove(Object item) {
            return modifyArray(array -> {
                array.remove(item);
                return array;
            });
        }

        public boolean removeAt(int index) {
            return modifyArray(array -> {
                if (index >= 0 && index < array.size()) {
                    array.remove(index);
                }
                return array;
            });
        }

        public boolean clear() {
            return modifyArray(array -> {
                array.clear();
                return array;
            });
        }

        public boolean updateAt(int index, Object item) {
            return modifyArray(array -> {
                if (index >= 0 && index < array.size()) {
                    array.set(index, item);
                }
                return array;
            });
        }

        public boolean filter(Function<Object, Boolean> predicate) {
            return modifyArray(array -> {
                JSONArray filtered = new JSONArray();
                for (Object item : array) {
                    if (predicate.apply(item)) {
                        filtered.add(item);
                    }
                }
                return filtered;
            });
        }

        public boolean updateWhere(String property, Object propertyValue, String updateKey, Object updateValue) {
            return modifyArray(array -> {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (propertyValue.equals(obj.get(property))) {
                            obj.put(updateKey, updateValue);
                            array.set(i, obj);
                            break;
                        }
                    }
                }
                return array;
            });
        }

        public boolean updateWhereMultiple(String property, Object propertyValue, Map<String, Object> updates) {
            return modifyArray(array -> {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (propertyValue.equals(obj.get(property))) {
                            obj.putAll(updates);
                            array.set(i, obj);
                            break;
                        }
                    }
                }
                return array;
            });
        }

        public boolean updateAllWhere(String property, Object propertyValue, String updateKey, Object updateValue) {
            return modifyArray(array -> {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (propertyValue.equals(obj.get(property))) {
                            obj.put(updateKey, updateValue);
                            array.set(i, obj);
                        }
                    }
                }
                return array;
            });
        }

        public boolean removeWhere(String property, Object propertyValue) {
            return modifyArray(array -> {
                array.removeIf(item -> {
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        return propertyValue.equals(obj.get(property));
                    }
                    return false;
                });
                return array;
            });
        }

        public int findIndex(String property, Object propertyValue) {
            JSONArray array = getJSONArray();
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (propertyValue.equals(obj.get(property))) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }

        public boolean contains(String property, Object propertyValue) {
            return findIndex(property, propertyValue) != -1;
        }

        public JSONObject findObject(String property, Object propertyValue) {
            JSONArray array = getJSONArray();
            if (array != null) {
                for (Object item : array) {
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (propertyValue.equals(obj.get(property))) {
                            return obj;
                        }
                    }
                }
            }
            return null;
        }

        public boolean updateNestedWhere(String findProperty, Object findValue, String nestedPath, Object updateValue) {
            return modifyArray(array -> {
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        if (findValue.equals(obj.get(findProperty))) {
                            setNestedValue(obj, nestedPath, updateValue);
                            array.set(i, obj);
                            break;
                        }
                    }
                }
                return array;
            });
        }

        private void setNestedValue(JSONObject json, String path, Object value) {
            String[] keys = path.split("\\.");
            JSONObject current = json;

            for (int i = 0; i < keys.length - 1; i++) {
                Object obj = current.get(keys[i]);
                if (obj instanceof JSONObject) {
                    current = (JSONObject) obj;
                } else {
                    JSONObject newObj = new JSONObject();
                    current.put(keys[i], newObj);
                    current = newObj;
                }
            }

            current.put(keys[keys.length - 1], value);
        }

        public boolean sortBy(String property, boolean ascending) {
            return modifyArray(array -> {
                List<Object> list = new ArrayList<>(array);
                list.sort((a, b) -> {
                    if (a instanceof JSONObject && b instanceof JSONObject) {
                        Object valA = ((JSONObject) a).get(property);
                        Object valB = ((JSONObject) b).get(property);

                        if (valA instanceof Comparable && valB instanceof Comparable) {
                            int result = ((Comparable) valA).compareTo(valB);
                            return ascending ? result : -result;
                        }
                    }
                    return 0;
                });

                JSONArray sorted = new JSONArray();
                sorted.addAll(list);
                return sorted;
            });
        }

        private JSONArray getJSONArray() {
            String sql = "SELECT " + jsonColumn + " FROM " + table + " WHERE " + whereClause;
            String jsonStr = SQL.gI().selectOne(sql, rs -> rs.getString(jsonColumn), whereParams);
            return parseJSONArray(jsonStr);
        }

        private boolean modifyArray(Function<JSONArray, JSONArray> modifier) {
            JSONArray array = getJSONArray();
            if (array == null) {
                array = new JSONArray();
            }

            JSONArray modified = modifier.apply(array);

            return SQLHelper.update(table)
                    .set(jsonColumn, modified.toJSONString())
                    .whereRaw(whereClause, whereParams)
                    .execute();
        }
    }

    public static JSONArrayModifier modifyJSONArray(String table, String jsonColumn, String whereClause, Object... whereParams) {
        return new JSONArrayModifier(table, jsonColumn, whereClause, whereParams);
    }

}