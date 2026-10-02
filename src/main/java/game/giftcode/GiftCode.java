package game.giftcode;

import core.Log;
import core.SQL;
import core.Service;
import core.Util;
import client.io.Session;
import game.items.ItemHelper;
import template.Item3;
import template.Item47;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GiftCode {


    public static boolean claimGiftCode(Session s, String giftcode) throws IOException {
        // FIX SQL INJECTION KRITIS: `giftcode` adalah teks yang diketik LANGSUNG oleh
        // player (kode redeem), disambung mentah ke query lewat "+". Siapapun bisa
        // ketik giftcode berisi tanda kutip untuk keluar dari string literal dan
        // menyisipkan SQL (mis. UNION SELECT untuk memalsukan baris giftcode dengan
        // limit/reward apapun yang dia mau, atau merusak/membaca tabel lain).
        // Diganti ke PreparedStatement dengan parameter binding.
        try (Connection connection = SQL.gI().getConnection();
             PreparedStatement st = connection.prepareStatement(
                     "SELECT * FROM `giftcode` WHERE `giftname` = ?;")) {
            st.setString(1, giftcode);
            try (ResultSet rs = st.executeQuery()) {
            byte empty_box = (byte) 0;
            if (!rs.next()) {
                Service.send_notice_box(s, "Giftcode sudah digunakan atau tidak ada");
            } else {
                List<Short> IDs = new ArrayList<>();
                List<Integer> Quants = new ArrayList<>();
                List<Short> Types = new ArrayList<>();
                int limit = rs.getInt("limit");
                boolean isForAllUser = rs.getInt("everyone") == 1;


                if (limit < 1 && s.ac_admin < 4) {
                    Service.send_notice_box(s, "Kuota penggunaan giftcode ini sudah habis");
                } else if (s.p.item.get_bag_able() >= empty_box) {

                    if (!isForAllUser) {
                        remove(giftcode);
                    }else{
                        s.p.giftcode.add(giftcode);
                    }

                    String items = rs.getString("items");
                    if (items == null) {
                        return false;
                    }

                    List<Item3> equipList = ItemHelper.parseItem3(items);
                    List<Item47> potiionList = ItemHelper.parseItem47(items, 4);
                    List<Item47> materialList = ItemHelper.parseItem47(items, 7);
                    if (!equipList.isEmpty()) {
                        for (Item3 item : equipList) {
                            IDs.add(item.id);
                            Quants.add(1);
                            Types.add((short) 3);
                            s.p.item.add_item_bag3(item);
                        }
                    }

                    if (!potiionList.isEmpty()) {
                        for (Item47 item : potiionList) {
                            IDs.add(item.id);
                            Quants.add((int)item.quantity);
                            Types.add((short) 4);
                            s.p.item.add_item_bag47(4, item);
                        }
                    }

                    if (!materialList.isEmpty()) {
                        for (Item47 item : materialList) {
                            IDs.add(item.id);
                            Quants.add((int)item.quantity);
                            Types.add((short) 7);
                            s.p.item.add_item_bag47(7, item);
                        }
                    }

                    long exp = rs.getLong("exp");
                    long gold = rs.getLong("gold");
                    int gem = rs.getInt("gem");
                    if (exp > 0) {
                        s.p.updateExp(exp, false);
                    }

                    s.p.updateGold(gold);
                    s.p.updateGem(gem);
                    if (gold != 0) {
                        IDs.add((short) -1);
                        Quants.add((int) (gold > 2_000_000_000 ? 2_000_000_000 : gold));
                        Types.add((short) 4);
                    }
                    if (gem != 0) {
                        IDs.add((short) -2);
                        Quants.add((int) (Math.min(gem, 2_000_000_000)));
                        Types.add((short) 4);
                    }
                    Log.gI().add_log(s.p.name,
                            "Claim giftcode " + giftcode + " : " + Util.number_format(gold) + " gold");
                    Log.gI().add_log(s.p.name,
                            "Claim giftcode " + giftcode + " : " + Util.number_format(gem) + " gem");

                    short[] ar_id = new short[IDs.size()];
                    int[] ar_quant = new int[Quants.size()];
                    short[] ar_type = new short[Types.size()];
                    for (int i = 0; i < ar_id.length; i++) {
                        ar_id[i] = IDs.get(i);
                        ar_quant[i] = Quants.get(i);
                        ar_type[i] = Types.get(i);
                    }
                    Service.Show_open_box_notice_item(s.p, "Anda mendapatkan", ar_id, ar_quant, ar_type);


                } else {
                    Service.send_notice_box(s,
                            "Tas harus memiliki setidaknya " + empty_box + " slot kosong!");
                }
            }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    public static boolean remove(String giftcode) {
        final String query = "DELETE FROM giftcode WHERE giftname = ?";

        try (Connection connection = SQL.gI().getConnection();

             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, giftcode);
            int rows = ps.executeUpdate();

            connection.commit();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
