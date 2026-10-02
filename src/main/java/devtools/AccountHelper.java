package devtools;


import core.Manager;
import core.ModelMapper;
import core.SQL;
import core.Util;

import lombok.extern.slf4j.Slf4j;

import model.player.Account;
import model.player.PlayerData;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utils.SQLHelper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@Slf4j
public class AccountHelper {

    private static final String TABLE_PLAYER = "player";
    private static final String TABLE_ACCOUNT = "account";

    public static void main(String[] args) throws IOException {
        Manager.gI().init();

        List<Account> accounts = SQLHelper.selectFrom(TABLE_ACCOUNT).getAsModel(Account.class);
        String json = ModelMapper.toJson(accounts);

        Util.saveFile(json.getBytes(StandardCharsets.UTF_8), "data/account.json");

        List<PlayerData> players = SQLHelper.selectFrom(TABLE_PLAYER).getAsModel(PlayerData.class);
        json = ModelMapper.toJson(players);

        Util.saveFile(json.getBytes(StandardCharsets.UTF_8), "data/player.json");
    }


    public static boolean isPlayerExists(String name) {
        return SQLHelper.selectFrom("player")
                .where("name", name)
                .exists();
    }

    private static void kompensasi() {

        List<String> players =
                SQLHelper.selectFrom("player").where("uid", "!=", -1)
                        .get(rs ->
                                rs.getString("name")

                        );

        JSONArray mt = new JSONArray();
        for (String name : players) {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("status", false);
            mt.add(obj);
        }
        SQLHelper.update("compensation")
                .set("players", mt.toJSONString())
                .where("name", "mt")
                .execute();
        log.info("Total {}", players.size());
    }

    private static void banUser(List<String> names) {

        List<Integer> users = SQLHelper.selectFrom("player")
                .whereIn("name", names)
                .get(rs -> rs.getInt("uid"));

        users.forEach(id -> SQLHelper.update("account")
                .set("`lock`", 1)
                .where("id", id)
                .execute());

    }

    private static void resetAllPlayer() {


        try (Connection conn = SQL.gI().getConnection();
             PreparedStatement ps = conn.prepareStatement(String.format("SELECT name, clazz FROM %s WHERE level >= ?", TABLE_PLAYER))) {

            ps.setInt(1, 1);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("name");
                    int clazz = rs.getInt("clazz");

                    SQLHelper.update()
                            .table(TABLE_PLAYER)
                            .set("item3", "[]")
                            .set("item4", "[]")
                            .set("item7", "[]")
                            .set("itembox3", "[]")
                            .set("itembox4", "[]")
                            .set("itembox7", "[]")
                            .set("itemwear", getItemwear(clazz))  // Use clazz here
                            .set("game/pet", "[]")
                            .set("enemies", "[]")
                            .set("eff", "[]")
                            .set("friend", "[]")
                            .set("level", 1)
                            .set("site", "[1,510,384]")
                            .set("rms_save", "[[],[]]")
                            .set("maxbag", 126)
                            .set("exp", 0)
                            .set("vang", 1000_000)
                            .set("kimcuong", 50_000)
                            .set("tiemnang", 5)
                            .set("kynang", 1)
                            .set("point1", 5)
                            .set("point2", 5)
                            .set("point3", 5)
                            .set("point4", 5)
                            .set("game/giftcode", "[]")
                            .set("skill", "[1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]")
                            .where("name = ?", playerName)
                            .execute();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static void printPlayerByLevel(int lv) {

        String query = String.format("SELECT * FROM %s WHERE level >= ?", TABLE_PLAYER);
        String csvFile = "player.csv";

        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {

            ps.setInt(1, lv);

            try (ResultSet rs = ps.executeQuery()) {

                // CSV header
                writer.write("name, level");
                writer.newLine();

                // Data
                while (rs.next()) {
                    String name = rs.getString("name");
                    int level = rs.getInt("level");

                    // escape name if needed
                    if (name != null) {
                        name = name.replace("\"", "\"\"");
                        writer.write("\"" + name + "\"");
                    } else {
                        writer.write("");
                    }

                    writer.write(",");
                    writer.write(String.valueOf(level));
                    writer.newLine();
                }
            }

            System.out.println("CSV saved: " + csvFile);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteAccountWithPlayers(String accountWhereClause, Object[] accountWhereParams,
                                                   String playerTable, String playerNameColumn) {
        try (Connection conn = SQL.gI().getConnection()) {

            // Get the character JSON array from account
            String getPlayersSql = "SELECT `char` FROM account WHERE " + accountWhereClause;

            try (PreparedStatement psGetPlayers = conn.prepareStatement(getPlayersSql)) {
                for (int i = 0; i < accountWhereParams.length; i++) {
                    psGetPlayers.setObject(i + 1, accountWhereParams[i]);
                }

                try (java.sql.ResultSet rs = psGetPlayers.executeQuery()) {
                    if (rs.next()) {
                        String characterJson = rs.getString("char");

                        if (characterJson != null && !characterJson.isEmpty()) {
                            // Parse JSON array - simple parsing for ["name1","name2"]
                            String[] playerNames = parseJsonArray(characterJson);

                            // Delete each player
                            if (playerNames.length > 0) {
                                StringBuilder deletePlayers = new StringBuilder("DELETE FROM " + playerTable + " WHERE " + playerNameColumn + " IN (");
                                for (int i = 0; i < playerNames.length; i++) {
                                    if (i > 0) deletePlayers.append(", ");
                                    deletePlayers.append("?");
                                }
                                deletePlayers.append(")");

                                try (PreparedStatement psDeletePlayers = conn.prepareStatement(deletePlayers.toString())) {
                                    for (int i = 0; i < playerNames.length; i++) {
                                        psDeletePlayers.setString(i + 1, playerNames[i]);
                                    }
                                    psDeletePlayers.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }

            // Delete the account
            String deleteAccountSql = "DELETE FROM account WHERE " + accountWhereClause;

            try (PreparedStatement psDeleteAccount = conn.prepareStatement(deleteAccountSql)) {
                for (int i = 0; i < accountWhereParams.length; i++) {
                    psDeleteAccount.setObject(i + 1, accountWhereParams[i]);
                }

                int rows = psDeleteAccount.executeUpdate();

                if (!conn.getAutoCommit()) {
                    conn.commit();
                }

                return rows > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parse simple JSON array like ["name1","name2","name3"]
     *
     * @param json JSON array string
     * @return Array of strings
     */
    private static String[] parseJsonArray(String json) {
        // Remove brackets and quotes
        String cleaned = json.replaceAll("[\\[\\]\"]", "");

        if (cleaned.isEmpty()) {
            return new String[0];
        }

        // Split by comma
        String[] names = cleaned.split(",");

        // Trim whitespace
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].trim();
        }

        return names;
    }


    public static class CascadeDeleteBuilder {
        private String accountWhereClause;
        private final java.util.List<Object> accountWhereParams = new java.util.ArrayList<>();
        private String playerTable = "player";
        private String playerNameColumn = "name";

        public CascadeDeleteBuilder where(String clause, Object... params) {
            this.accountWhereClause = clause;
            Collections.addAll(accountWhereParams, params);
            return this;
        }

        public CascadeDeleteBuilder playerTable(String table) {
            this.playerTable = table;
            return this;
        }

        public CascadeDeleteBuilder playerNameColumn(String column) {
            this.playerNameColumn = column;
            return this;
        }

        public boolean execute() {
            return deleteAccountWithPlayers(
                    accountWhereClause,
                    accountWhereParams.toArray(),
                    playerTable,
                    playerNameColumn
            );
        }
    }

    /**
     *
     * Create a CascadeDeleteBuilder instance
     */
    public static CascadeDeleteBuilder deleteAccount() {
        return new CascadeDeleteBuilder();
    }


    public static String getItemwear(int clazz) {
        switch (clazz) {
            case 0 -> {
                return "[[0,0,8,1,0,0,0,0,[[0,54],[40,120]],0],[80,0,0,1,16,0,0,0,[[14,52],[16,100]],1],[120,0,1,1,24,0,0,0,[[14,18],[25,3]],7]]";

            }
            case 1 -> {
                return
                        "[[5,1,9,1,1,0,0,0,[[0,54],[40,120]],0],[105,1,0,1,21,0,1,0,[[14,52],[20,100]],1],[145,1,1,1,29,0,1,0,[[14,18],[24,3]],7]]";
            }
            case 2 -> {
                return
                        "[[10,2,11,1,2,0,0,0,[[0,50],[40,120]],0],[90,2,0,1,18,0,2,0,[[14,42],[16,200]],1],[50,2,2,1,10,0,2,0,[[7,200],[14,12]],6],[130,2,1,1,26,0,2,0,[[14,12],[26,4]],7]]";
            }
            default -> {
                return "[[15,3,10,1,3,0,0,0,[[0,50],[40,120]],0],[95,3,0,1,19,0,3,0,[[14,44],[16,200]],1],[55,3,2,1,11,0,3,0,[[7,200],[14,14]],6],[135,3,1,1,27,0,3,0,[[14,14],[24,4]],7]]";

            }
        }
    }
}
