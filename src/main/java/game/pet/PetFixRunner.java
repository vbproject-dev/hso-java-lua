package game.pet;

import java.sql.*;

public class PetFixRunner {

    public static void main(String[] args) throws Exception {

        // 🔌 koneksi DB
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/hso_raw",
                "root",
                "asu123"
        );

        // load pet_data
        PetDataService petDataService = new PetDataService(conn);

        // init fixer
        PetFixer fixer = new PetFixer(petDataService);

        // ambil semua player
        String select = "SELECT id, pet FROM player";
        String update = "UPDATE player SET pet=? WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(select);
             ResultSet rs = ps.executeQuery();
             PreparedStatement ups = conn.prepareStatement(update)) {

            while (rs.next()) {
                int playerId = rs.getInt("id");
                String petJson = rs.getString("pet");

                String fixed = fixer.fix(petJson);

                ups.setString(1, fixed);
                ups.setInt(2, playerId);
                ups.executeUpdate();

                System.out.println("Fixed player: " + playerId);
            }
        }

        conn.close();
        System.out.println("DONE FIX ALL PLAYER PET");
    }
}