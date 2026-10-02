package game.rewards;

import core.SQL;
import org.json.simple.JSONArray;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserReward {
    public int id;
    public String name;
    public DonationPack item;
    public List<String> claimedBy = new ArrayList<>();



    public void markAsClaimed(String playerName) {
        try (Connection conn = SQL.gI().getConnection()) {
            conn.setAutoCommit(false);


            if (!claimedBy.contains(playerName)) {
                claimedBy.add(playerName);
            }


            JSONArray claimed = new JSONArray();
            claimed.addAll(claimedBy);


            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE user_reward SET claimed_by = ?, last_claim_date = NOW() WHERE name = ?")) {
                ps.setString(1, claimed.toJSONString());
                ps.setString(2, name);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
