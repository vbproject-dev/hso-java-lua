package core;

import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

@Slf4j
public class SQL {

    /** One SQL statement and the parameter rows to run it with (several rows = one JDBC batch). */
    public record Command(String sql, List<Object[]> rows) {
    }

    private static class Holder {
        private static final SQL INSTANCE = new SQL();
    }

    public static boolean is_connected = false;

    private HikariDataSource dataSource;
    public final String url;
    private final String user;
    private final String pass;


    public SQL() {
        url = "jdbc:mysql://" + Manager.gI().mysql_host + ":"+Manager.gI().mysql_port +"/" + Manager.gI().mysql_database
                + "?autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8&rewriteBatchedStatements=true";
        System.out.println(url);
        user = Manager.gI().mysql_user;
        pass = Manager.gI().mysql_pass;

        dataSource = createHikariDataSource();
        SQL.is_connected = true;

    }

    private @NonNull HikariDataSource createHikariDataSource() {
        HikariDataSource config = new HikariDataSource();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setAutoCommit(false);
        config.setConnectionTimeout(30_000L);
        config.setIdleTimeout(600_000);
        config.setKeepaliveTime(0);
        config.setMaxLifetime(1_800_000);
        config.setMaximumPoolSize(40);
        config.setMinimumIdle(10);
        config.setLeakDetectionThreshold(20_000L);
        config.setPoolName("HSO_pool");

        return config;
    }

    public static SQL gI() {
        return Holder.INSTANCE;
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

        } catch (SQLException e) {
            log.error("Error executing update: {}", sql, e);
            return false;
        }
    }


    public boolean executeTransaction(Collection<Command> commands) {
        try (Connection conn = getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (Command command : commands) {
                    run(conn, command);
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            log.error("Transaction failed ({} commands)", commands.size(), e);
            return false;
        }
    }

    private void run(Connection conn, Command command) throws SQLException {
        if (command.rows().isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(command.sql())) {
            for (Object[] row : command.rows()) {
                for (int i = 0; i < row.length; i++) {
                    ps.setObject(i + 1, row[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

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

        String sql = "UPDATE %s SET %s WHERE %s".formatted(table, setClause, whereClause);

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

    public long insertInto(String table, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }

        String columns = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream()
                .map(k -> "?")
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO %s (%s) VALUES (%s)".formatted(table, columns, placeholders);

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

    public boolean delete(String table, String whereClause, Object... whereParams) {
        String sql = "DELETE FROM %s WHERE %s".formatted(table, whereClause);

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

    public <T> T select(String sql, ResultSetHandler<T> handler, Object... params) {

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return handler.handle(rs);
            }

        } catch (SQLException e) {
            log.error(
                    "Error executing select: {} [SQLState={}, ErrorCode={}]",
                    sql,
                    e.getSQLState(),
                    e.getErrorCode(),
                    e
            );

            return null;
        }
    }

    public <T> List<T> selectList(String sql, RowMapper<T> mapper, Object... params) {
        return select(sql, rs -> {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.mapRow(rs));
            }
            return results;
        }, params);
    }


    public <T> T selectOne(String sql, RowMapper<T> mapper, Object... params) {
        return select(sql, rs -> {
            if (rs.next()) {
                return mapper.mapRow(rs);
            }
            return null;
        }, params);
    }

    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }

}