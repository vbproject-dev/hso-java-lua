package game.rewards;

import core.SQL;
import game.event.spin.models.BetType;
import game.event.spin.models.SpinType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GambleReward {
    public int id;
    public SpinType type;
    public String name;
    public BetType betType;
    public long amount;
    public String status;



    public void delete() {
        String sql = "DELETE FROM gamble_winner WHERE id = ?";

        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // ensure commit works

            ps.setInt(1, id); // integer value
            ps.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try (Connection conn = SQL.gI().getConnection()) {
                conn.rollback();
            } catch (SQLException ignored) {}
        }
    }

}
