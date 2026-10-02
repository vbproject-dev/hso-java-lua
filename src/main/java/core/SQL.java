package core;

import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQL {

    public static boolean is_connected = false;
    private static SQL instance = null;
    private HikariDataSource dataSource;
    public final String url;
    private final String user;
    private final String pass;


    public SQL() {
        url = "jdbc:mysql://" + Manager.gI().mysql_host + ":"+Manager.gI().mysql_port +"/" + Manager.gI().mysql_database
                + "?autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8";
        System.out.println(url);
        user = Manager.gI().mysql_user;
        pass = Manager.gI().mysql_pass;
        HikariDataSource config = new HikariDataSource();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setAutoCommit(false);

        config.setConnectionTimeout(30_000L);
        config.setIdleTimeout(600_000);
        config.setKeepaliveTime(0);
        config.setMaxLifetime(1_800_000);
        // Dinaikkan dari 15 -> 40. Pool 15 kehabisan koneksi (waiting sampai 50+)
        // saat jam ramai, bikin query login/save gagal timeout.
        // NOTE: pastikan MySQL `max_connections` di server DB cukup besar
        // (minimal beberapa kali lipat dari nilai ini, karena proses lain juga
        // mungkin konek ke DB yang sama).
        config.setMaximumPoolSize(40);
        config.setMinimumIdle(10);
        // Bantu ketahuan di log kalau ada kode yang pegang koneksi kelamaan
        // (nggak di-close), supaya lebih gampang investigasi ke depannya.
        config.setLeakDetectionThreshold(20_000L);
        config.setPoolName("HSO_pool");

        dataSource = new HikariDataSource(config);
        SQL.is_connected = true;
        System.out.println("OPEN DataBase connect");
    }

    public static SQL gI() {
        if (instance == null) {
            instance = new SQL();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (SQL.is_connected) {
            if (dataSource != null) {
                dataSource.close();
                System.out.println("CLOSE DataBase connect");
            }
            SQL.is_connected = false;
        }
    }

    public boolean executeUpdate(String sql) {
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            int rows = ps.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return rows > 0;

        } catch (Exception e) {
            log.error("Error executing update: {}", sql, e);
            return false;
        }
    }

    /**
     * Update multiple columns with Map of column-value pairs
     */
    /**
     * Update multiple columns with Map of column-value pairs.
     * Mendukung ekspresi RAW (dipakai oleh SQLHelper.UpdateBuilder#increment/decrement/multiply):
     * jika value berupa String dan diawali "RAW:", nilai tsb ditaruh langsung di klausa SET
     * (bukan sebagai bind parameter), contoh: "RAW:poin_isi_ulang_permata + 100000"
     * -> SET `poin_isi_ulang_permata` = poin_isi_ulang_permata + 100000
     */
    public boolean updateColumns(String table, Map<String, Object> updates,
                                 String whereClause, Object... whereParams) {
        if (updates == null || updates.isEmpty()) {
            return false;
        }

        List<Object> bindValues = new ArrayList<>();
        String setClause = updates.entrySet().stream()
                .map(e -> {
                    Object value = e.getValue();
                    if (value instanceof String s && s.startsWith("RAW:")) {
                        return e.getKey() + " = " + s.substring(4);
                    }
                    bindValues.add(value);
                    return e.getKey() + " = ?";
                })
                .collect(Collectors.joining(", "));

        String sql = "UPDATE " + table + " SET " + setClause + " WHERE " + whereClause;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;

            for (Object value : bindValues) {
                ps.setObject(paramIndex++, value);
            }

            for (Object param : whereParams) {
                ps.setObject(paramIndex++, param);
            }

            int rows = ps.executeUpdate();

            if (!conn.getAutoCommit()) {
                conn.commit();
            }

            return rows > 0;

        } catch (SQLException e) {
            log.error("Error updating columns in table: {}", table, e);
            return false;
        }
    }

    /**
     * Insert data into table
     */
    public long insertInto(String table, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }

        String columns = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream()
                .map(k -> "?")
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            for (Object value : values.values()) {
                ps.setObject(paramIndex++, value);
            }

            int rows = ps.executeUpdate();

            if (!conn.getAutoCommit()) {
                conn.commit();
            }

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }

            return rows > 0 ? 0 : -1;

        } catch (SQLException e) {
            log.error("Error inserting into table: {}", table, e);
            return -1;
        }
    }

    /**
     * Delete rows with WHERE clause
     */
    public boolean delete(String table, String whereClause, Object... whereParams) {
        String sql = "DELETE FROM " + table + " WHERE " + whereClause;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            for (Object param : whereParams) {
                ps.setObject(paramIndex++, param);
            }

            int rows = ps.executeUpdate();

            if (!conn.getAutoCommit()) {
                conn.commit();
            }

            return rows > 0;

        } catch (SQLException e) {
            log.error("Error deleting from table: {}", table, e);
            return false;
        }
    }

    /**
     * Select query executor with callback pattern
     * Ensures proper resource cleanup
     *
     * @param sql SQL query string
     * @param handler Function to process ResultSet and return result
     * @param params Query parameters
     * @return Result from handler, or null if the query ran but found nothing / failed after connecting
     * @throws DbUnavailableException if a connection could not be acquired from the pool
     *         (pool exhausted, timeout, interrupted). Callers that need to tell this apart
     *         from "no data found" (e.g. login) should catch this specifically.
     */
    public <T> T select(String sql, ResultSetHandler<T> handler, Object... params) {
        Connection conn;
        try {
            conn = getConnection();
        } catch (SQLException e) {
            // Gagal DAPAT koneksi dari pool (pool exhausted/timeout/interrupted).
            // Ini bukan "data kosong" - jangan return null di sini, karena pemanggil
            // (mis. cek login/akun) bisa salah mengira akun tidak ada / password salah,
            // padahal DB-nya yang lagi sibuk.
            log.error("Failed to acquire DB connection for select: {}", sql, e);
            throw new DbUnavailableException("Gagal mendapatkan koneksi database: " + sql, e);
        }

        try (Connection c = conn; PreparedStatement ps = c.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return handler.handle(rs);
            }

        } catch (SQLException e) {
            log.error("Error executing select: {}", sql, e);
            return null;
        }
    }

    /**
     * Select query that returns list of results
     */
    public <T> List<T> selectList(String sql, RowMapper<T> mapper, Object... params) {
        return select(sql, rs -> {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.mapRow(rs));
            }
            return results;
        }, params);
    }

    /**
     * Select query that returns single result
     */
    public <T> T selectOne(String sql, RowMapper<T> mapper, Object... params) {
        return select(sql, rs -> {
            if (rs.next()) {
                return mapper.mapRow(rs);
            }
            return null;
        }, params);
    }

    /**
     * Functional interface for handling ResultSet
     */
    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    /**
     * Functional interface for mapping a single row
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}