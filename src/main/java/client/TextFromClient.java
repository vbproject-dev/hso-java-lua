package client;

import core.GameSrc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import game.event.Event_1;
import game.event.GameEventManager;
import game.event.WorldBoss;
import game.event.spin.GemSpin;
import game.event.spin.GoldSpin;
import game.event.spin.QuickGemSpin;
import game.event.spin.QuickGoldSpin;
import game.giftcode.GiftCode;
import game.guild.Guild;
import game.map.MobInMap;
import core.Log;
import core.Manager;
import core.SQL;
import core.Service;
import core.Util;

import client.io.Message;
import client.io.Session;

import java.util.ArrayList;
import java.util.List;

import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;
import menu.InputDialog;
import template.*;

public class TextFromClient {

    public static void process(Session conn, Message m2) throws IOException {
        short idnpc = m2.reader().readShort();
        short idmenu = m2.reader().readShort();
        byte size = m2.reader().readByte();


        if (conn.p.getCurrentInputDialog() != null) {
            InputDialog inputDialog = conn.p.getCurrentInputDialog();
            String[] values = new String[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = m2.reader().readUTF();
            }
            if (inputDialog.getAction() != null) {
                inputDialog.getAction().accept(conn.p, values);
            }
            conn.p.setCurrentInputDialog(null);
            return;
        }

        if (idmenu != 0) {
            return;
        }
        switch (idnpc) {

            case 30: {
                if (size != 3) {
                    return;
                }
                String value1 = m2.reader().readUTF();
                String value2 = m2.reader().readUTF();
                String value3 = m2.reader().readUTF();

                if (!value1.equals(conn.pass)) {
                    Service.send_notice_box(conn, "Kata sandi salah");

                    return;
                }
                if (value2.equals(value1) || !value2.equals(value3)) {
                    Service.send_notice_box(conn, "Kata sandi baru tidak valid");
                    return;
                }
                // FIX SQL INJECTION KRITIS: sebelumnya `value2` (password baru, input MENTAH
                // dari client tanpa validasi karakter apapun) disambung langsung ke string
                // query lewat "+". Kalau isinya mengandung tanda kutip tunggal ('), pemain
                // bisa keluar dari string literal dan menyisipkan SQL apapun ke tabel
                // `account` -- ubah data akun lain, hapus baris, dst. Diganti ke
                // PreparedStatement dengan parameter binding supaya isi value2 SELALU
                // diperlakukan sebagai data, bukan bagian dari perintah SQL.
                try (Connection connection = SQL.gI().getConnection();
                     PreparedStatement st = connection.prepareStatement(
                             "UPDATE `account` SET `pass` = ? WHERE `user` = ?;")) {
                    st.setString(1, value2);
                    st.setString(2, conn.user);
                    st.executeUpdate();
                    connection.commit();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Service.send_notice_box(conn, "Terjadi kesalahan");
                    return;
                }
                Service.send_notice_box(conn, "Berhasil mengubah kata sandi baru");

                break;
            }
            case 66: {
                if (size != 2) {
                    return;
                }
                String value1 = m2.reader().readUTF();
                String value2 = m2.reader().readUTF();
                if (!(Util.isnumber(value1))) {
                    Service.send_notice_box(conn, "Input bukan angka!!");

                    return;
                }
                int quant = Integer.parseInt(value1);
                Player p0 = GameMap.get_player_by_name(value2);
                if (p0 != null) {
                    if (p0.item.wear[23] != null) {
                        Service.send_notice_box(conn, "Pihak lain sudah berpasangan dengan orang lain!");
                        return;
                    }
                    if (p0.level < 60) {
                        Service.send_notice_box(conn, "Diperlukan level di atas 60");
                        return;
                    }
                    switch (quant) {
                        case 1: {
                            if (conn.p.getGold() < 3_000_000_000L) {
                                Service.send_notice_box(conn, "Tidak cukup 3 miliar emas");

                                return;
                            }
                            conn.p.updateGold(-3_000_000_000L);
                            break;
                        }
                        case 2: {
                            if (conn.p.getGold() < 6_000_000_000L) {
                                Service.send_notice_box(conn, "Tidak cukup 6 miliar emas");
                                return;
                            }
                            conn.p.updateGold(-6_000_000_000L);
                            break;
                        }
                        case 3: {
                            if (conn.p.getGem() < 300_000) {
                                Service.send_notice_box(conn, "Tidak cukup 300k permata");
                                return;
                            }
                            conn.p.updateGem(-300_000);
                            break;
                        }
                        case 4: {
                            if (conn.p.getGem() < 600_000) {
                                Service.send_notice_box(conn, "Tidak cukup 600k permata");
                                return;
                            }
                            conn.p.updateGem(-600_000);
                            break;
                        }
                        default: {
                            Service.send_notice_box(conn, "Pilih cincin 1-4 saja!");

                            return;
                        }
                    }
                    conn.p.item.charInventory(5);
                    p0.in4_wedding = new String[]{"" + quant, conn.p.name};
                    Service.send_box_input_yesno(p0.conn, 110,
                            conn.p.name + " ingin melamarmu, maukah kamu menikah denganku?");
                } else {
                    Service.send_notice_box(conn, "Pihak lain tidak ditemukan!");

                }
                break;
            }
            case 16: {
                if (size != 1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Input bukan angka!!");

                    return;
                }
                int quant = Integer.parseInt(value);
                if (quant > 32_000 || quant <= 0) {
                    Service.send_notice_box(conn, "Jumlah tidak valid!");

                    return;
                }
                int quant_ngoc_can_create = conn.p.item.total_item_by_id(7, conn.p.id_ngoc_tinh_luyen);
                if (quant > quant_ngoc_can_create) {
                    Service.send_notice_box(conn, "Jumlah di tas tidak cukup!");
                    return;
                }
                int vang_required = (int) (((long) quant)
                        * (GameSrc.getGoldPriceForGem(conn.p.id_ngoc_tinh_luyen) / 50_000L) * 1_000_000L);
                if (conn.p.getGold() < vang_required) {
                    Service.send_notice_box(conn, "Tidak cukup " + vang_required + " emas");
                    return;
                }
                if (conn.p.getGold() < vang_required) {
                    Service.send_notice_box(conn, "Pemurnian membutuhkan " + vang_required + " emas!");
                    return;
                }
                conn.p.updateGold(-vang_required);
                Item47 it = new Item47();
                it.id = (short) (conn.p.id_ngoc_tinh_luyen + 30);
                it.quantity = (short) quant;
                conn.p.item.add_item_bag47(7, it);
                conn.p.item.remove(7, conn.p.id_ngoc_tinh_luyen, quant);
                Service.send_notice_box(conn,
                        "Berhasil memurnikan " + quant + " " + ItemTemplate7.item.get(it.id).getName());
                conn.p.id_ngoc_tinh_luyen = -1;
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                break;
            }
            case 0: {

                if (conn.p.level < 100) {
                    Service.send_notice_box(conn, "Membutuhkan lv 100 untuk claim kode gift");
                    return;
                }

                String text = m2.reader().readUTF();
                text = text.toLowerCase();
                Pattern p = Pattern.compile("^[a-zA-Z0-9]{1,15}$");
                if (!p.matcher(text).matches()) {
                    Service.send_notice_box(conn, "Terjadi kesalahan");
                    return;
                }

                // Check kode gift di database user apakah sudah tersimpan di column giftcode
                for (String txt : conn.p.giftcode) {
                    txt = txt.toLowerCase();
                    if (txt.equals((text)) && conn.ac_admin < 4) {
                        Service.send_notice_box(conn, "Kamu sudah menggunakan giftcode ini");
                        return;
                    }
                }

                if (!GiftCode.claimGiftCode(conn, text)) {
                    Service.send_notice_box(conn, "Terjadi Kesalahan");
                    return;
                }

                break;
            }
            case 1: {
                if (conn.ac_admin > 3) {
                    if (size != 3) {
                        return;
                    }
                    String type = m2.reader().readUTF();
                    String id = m2.reader().readUTF();
                    String quantity = m2.reader().readUTF();
                    if (!(Util.isnumber(id) && Util.isnumber(quantity))) {
                        Service.send_notice_box(conn, "Input bukan angka!!");
                        return;
                    }
                    Short sl = Short.parseShort(quantity);
                    if (sl > 32_000 || sl <= 0) {
                        Service.send_notice_box(conn, "Jumlah tidak valid!");
                        return;
                    }
                    if (conn.p.item.get_bag_able() > 0) {
                        switch (type) {
                            case "3": {
                                short iditem = (short) Integer.parseInt(id);
                                if (iditem > (ItemTemplate3.item.size() - 1) || iditem < 0) {
                                    return;
                                }
                                Item3 itbag = new Item3();
                                itbag.id = iditem;
                                itbag.name = ItemTemplate3.item.get(iditem).getName();
                                itbag.clazz = ItemTemplate3.item.get(iditem).getClazz();
                                itbag.type = ItemTemplate3.item.get(iditem).getType();
                                itbag.level = ItemTemplate3.item.get(iditem).getLevel();
                                itbag.icon = ItemTemplate3.item.get(iditem).getIcon();
                                itbag.op = new ArrayList<>();
                                itbag.op.addAll(ItemTemplate3.item.get(iditem).getOp());
                                itbag.color = ItemTemplate3.item.get(iditem).getColor();
                                itbag.part = ItemTemplate3.item.get(iditem).getPart();
                                itbag.tier = 0;
                                itbag.islock = false;
                                itbag.time_use = 0;
                                conn.p.item.add_item_bag3(itbag);
                                conn.p.item.charInventory(3);
                                break;
                            }
                            case "4": {
                                short iditem = (short) Integer.parseInt(id);
                                if (iditem > (ItemTemplate4.item.size() - 1) || iditem < 0) {
                                    return;
                                }
                                Item47 itbag = new Item47();
                                itbag.id = iditem;
                                itbag.quantity = sl;
                                itbag.category = 4;
                                conn.p.item.add_item_bag47(4, itbag);
                                conn.p.item.charInventory(4);
                                break;
                            }
                            case "7": {
                                short iditem = (short) Integer.parseInt(id);
                                if (iditem > (ItemTemplate7.item.size() - 1) || iditem < 0) {
                                    return;
                                }
                                Item47 itbag = new Item47();
                                itbag.id = iditem;
                                itbag.quantity = Short.parseShort(quantity);
                                itbag.category = 7;
                                conn.p.item.add_item_bag47(7, itbag);
                                conn.p.item.charInventory(7);
                                break;
                            }
                        }
                        Service.send_notice_box(conn, "Berhasil menerima item");
                    }
                }
                break;
            }
            case 2: {
                if (conn.ac_admin > 3) {
                    if (size != 1) {
                        return;
                    }
                    String level = m2.reader().readUTF();
                    if (!(Util.isnumber(level))) {
                        Service.send_notice_box(conn, "Input bukan angka!!");
                        return;
                    }
                    int levelchange = Integer.parseInt(level);
                    if (levelchange > 32000 || levelchange <= 0) {
                        Service.send_notice_box(conn, "Jumlah tidak valid!");
                        return;
                    }
                    if (levelchange < 2) {
                        levelchange = 2;
                    }
                    if (levelchange > Manager.gI().lvmax) {
                        levelchange = Manager.gI().lvmax;
                    }
                    conn.p.level = (short) (levelchange - 1);
                    conn.p.exp = Level.entrys.get(levelchange - 2).exp - 1;
                    conn.p.tiemnang = (short) (1 + Level.get_tiemnang_by_level(conn.p.level - 1));
                    conn.p.kynang = (short) (1 + Level.get_kynang_by_level(conn.p.level - 1));
                    conn.p.point1 = (short) (4 + conn.p.level);
                    conn.p.point2 = (short) (4 + conn.p.level);
                    conn.p.point3 = (short) (4 + conn.p.level);
                    conn.p.point4 = (short) (4 + conn.p.level);
                    conn.p.updateExp(1, false);
                    Service.sendMainCharInfo(conn.p);
                    for (int i = 0; i < conn.p.map.players.size(); i++) {
                        Player p0 = conn.p.map.players.get(i);
                        if (p0.objectId != conn.p.objectId && (Math.abs(p0.x - conn.p.x) < 200)
                                && (Math.abs(p0.y - conn.p.y) < 200)) {
                            MapService.send_in4_other_char(p0.map, p0, conn.p);
                        }
                    }
                    Service.send_notice_box(conn, "Berhasil naik level");
                }
                break;
            }
            case 3: {

                if (size != 1) {
                    return;
                }

                String betAmountStr = m2.reader().readUTF();
                if (!(Util.isnumber(betAmountStr))) {
                    Service.send_notice_box(conn, "Input bukan angka!!");
                    return;
                }
                int betAmount = Integer.parseInt(betAmountStr);
                GoldSpin goldGamble = GameEventManager.gI().getEvent(GoldSpin.class);
                if (goldGamble.join(conn.p, betAmount)) {
                    conn.p.sendNoticeBox("Berhasil bertaruh");
                }
                break;
            }
            case 4: {
                if (conn.ac_admin < 10) {
                    // "Bạn không đủ quyền!" -> "Anda tidak memiliki wewenang yang cukup!"
                    Service.send_notice_box(conn, "Anda tidak memiliki wewenang yang cukup!");
                    return;
                }
                if (size != 1) {
                    return;
                }
                String xp = m2.reader().readUTF();
                if (!(Util.isnumber(xp))) {
                    // "Dữ liệu nhập không phải số!!" -> "Input bukan angka!!"
                    Service.send_notice_box(conn, "Input bukan angka!!");
                    return;
                }
                int xp_ = Integer.parseInt(xp);
                if (xp_ <= 0 || xp_ > 2000000000) {
                    // "Số lượng nhập vào không hợp lệ!" -> "Jumlah yang dimasukkan tidak valid!"
                    Service.send_notice_box(conn, "Jumlah yang dimasukkan tidak valid!");
                    return;
                }
                // xp_ udah dipastikan >= 1 di pengecekan validasi di atas.
                // REVERT: samain skala sama menu "Setting Exp" — ketik "1"
                // (maksud x1) disimpan sebagai "100" (skala persen, 100 = x1
                // normal di Player.updateExp()).
                Manager.gI().exp = xp_ * 100;
                // "Thay đổi xp thành công x" -> "Berhasil mengubah XP menjadi x"
                Service.send_notice_box(conn, "Berhasil mengubah XP menjadi x" + Util.number_format(xp_));

                break;
            }
            case 5: {
                if (size != 1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    // "Dữ liệu nhập không phải số!!" -> "Input bukan angka!!"
                    Service.send_notice_box(conn, "Input bukan angka!!");
                    return;
                }
                int coin_exchange = Integer.parseInt(value);
                if (coin_exchange < 1000 || coin_exchange > 300_000) {

                    Service.send_notice_box(conn, "Hanya bisa menukar minimal 1k dan maksimal 300k");
                    return;
                }
                if (conn.p.update_coin(-coin_exchange)) {
                    conn.p.updateGem((int) (coin_exchange / 2));
                    conn.p.item.charInventory(5);
                    // "Đổi thành công" -> "Berhasil menukar"
                    Service.send_notice_box(conn, "Berhasil menukar");

                } else {
                    // "Thất bại xin hãy thử lại" -> "Gagal, silakan coba lagi"
                    Service.send_notice_box(conn, "Gagal, silakan coba lagi");
                }
                break;
            }
            case 6: {
                if (size != 2) {
                    return;
                }
                String value1 = m2.reader().readUTF();
                String value2 = m2.reader().readUTF();
                Pattern p = Pattern.compile("^[a-zA-Z0-9]{5,15}$");
                if (!p.matcher(value1).matches() || !p.matcher(value2).matches()) {
                    Service.send_notice_box(conn, "Karakter tidak valid, silakan coba lagi");
                    return;
                }
                //
                // try (Connection connnect = SQL.gI().getConnection();
                // PreparedStatement ps = connnect.prepareStatement(
                // "INSERT INTO `sm_hso2`.`account` (`user`, `pass`, `char`, `status`, `lock`,
                // `coin`) VALUES ('"
                // + value1 + "', '" + value2 + "', '[]', 0, 0, 0)")) {
                // if (!ps.execute()) {
                // connnect.commit();
                // }
                // } catch (SQLException e) {
                // e.printStackTrace();
                // return;
                // }
                String query = "UPDATE `account` SET `user` = '" + value1 + "', `pass` = '" + value2
                        + "', `status` = 1 WHERE `user` = '" + conn.user + "' LIMIT 1";
                try (Connection connect = SQL.gI().getConnection();
                     Statement statement = connect.createStatement()) {

                    connect.setAutoCommit(false);
                    int rowsAffected = statement.executeUpdate(query);

                    if (rowsAffected > 0) {
                        connect.commit();

                        Message md = new Message(31);
                        md.writer().writeUTF(value1);
                        md.writer().writeUTF(value2);
                        conn.addmsg(md);
                        md.cleanup();
                        conn.user = value1;
                        conn.pass = value2;
                        Service.send_notice_box(conn,
                                "Pendaftaran akun berhasil:\n Nama pengguna: " + value1 + "\nKata sandi: " + value2);

                    } else {
                        connect.rollback();
                        Service.send_notice_box(conn, "Tidak ada data yang diupdate");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    Service.send_notice_box(conn, "Terjadi kesalahan atau nama sudah digunakan, silakan coba lagi");
                    return;
                }

                break;
            }
            case 7: {
                if (size != 1 || conn.p.fusion_material_medal_id == -1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int quant = Integer.parseInt(value);
                if (quant > 32000 || quant <= 0) {
                    Service.send_notice_box(conn, "Jumlah tidak valid");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tas penuh!");
                    return;
                }
                int quant_inbag = conn.p.item.total_item_by_id(7, conn.p.fusion_material_medal_id);
                int quant_real = quant_inbag / 5;
                short id_next_material = (short) (conn.p.fusion_material_medal_id + 100);
                String name_next_material = ItemTemplate7.item.get(id_next_material).getName();
                if ((quant_real - quant) >= 0) {
                    if ((quant * 5000) > conn.p.getGold()) {
                        Service.send_notice_box(conn, "Emas tidak cukup!");
                        return;
                    }

                    conn.p.updateGold(-(quant * 5000));
                    conn.p.item.remove(7, conn.p.fusion_material_medal_id, (quant * 5));
                    Item47 it = new Item47();
                    it.id = id_next_material;
                    it.quantity = (short) quant;
                    conn.p.item.add_item_bag47(7, it);
                    conn.p.item.charInventory(7);
                    //
                    Message m = new Message(-105);
                    m.writer().writeByte(2);
                    m.writer().writeByte(3);
                    m.writer().writeUTF("Selamat! Anda mendapatkan " + quant + " " + name_next_material);
                    m.writer().writeShort(id_next_material);
                    m.writer().writeByte(7);
                    conn.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_notice_box(conn,
                            "Anda hanya bisa menggabungkan maksimal " + quant_real + " " + name_next_material);
                }
                conn.p.fusion_material_medal_id = -1;
                break;
            }
            case 8: {
                // if (size != 1) {
                // return;
                // }
                // String value = m2.reader().readUTF();
                // if (!(Util.isnumber(value))) {
                // Service.send_notice_box(conn, "Dữ liệu nhập không phải số!!");
                // return;
                // }
                // int coin_exchange = Integer.parseInt(value);
                // if (coin_exchange <= 0 || coin_exchange > 1_000_000_000) {
                // Service.send_notice_box(conn, "Số nhập không hợp lệ, hãy thử lại");
                // return;
                // }
                // if (conn.p.update_coin(-coin_exchange)) {
                // conn.p.update_vang(coin_exchange * 5000);
                // conn.p.item.char_inventory(5);
                // Service.send_notice_box(conn, "Đổi thành công");
                // }
                // break;
            }
            case 9: {
                if (size != 1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int quant = Integer.parseInt(value);
                if (quant > 2_000_000_000 || quant <= 0) {
                    Service.send_notice_box(conn, "Jumlah tidak valid!");
                    return;
                }
                if (idnpc == 8) {
                    if (quant > conn.p.getGold()) {
                        Service.send_notice_box(conn, "Emas tidak cukup!");
                        return;
                    }
                    conn.p.myclan.contributeGold(conn, quant);
                } else {
                    if (quant > conn.p.getGem()) {
                        Service.send_notice_box(conn, "Permata tidak cukup!");
                        return;
                    }
                    conn.p.myclan.contributeGem(conn, quant);
                }
                break;
            }
            case 10: {
                if (Manager.gI().event == 1) {
                    if (size != 1) {
                        return;
                    }
                    String value = m2.reader().readUTF();
                    if (!(Util.isnumber(value))) {
                        Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                        return;
                    }
                    int quant = Integer.parseInt(value);
                    if (quant > 500 || quant <= 0) {
                        Service.send_notice_box(conn, "Jumlah tidak valid!");
                        return;
                    }
                    //
                    if (conn.p.getGold() < (quant * 20_000)) {
                        Service.send_notice_box(conn, "Emas tidak cukup!");
                        return;
                    }
                    short[] id = new short[]{118, 119, 120, 121, 122};
                    for (int i = 0; i < id.length; i++) {
                        if (conn.p.item.total_item_by_id(4, id[i]) < (quant * 50)) {
                            Service.send_notice_box(conn, (ItemTemplate4.item.get(id[i]).getName() + " tidak cukup!"));
                            return;
                        }
                    }
                    conn.p.updateGold(-(quant * 20_000));
                    for (int i = 0; i < id.length; i++) {
                        conn.p.item.remove(4, id[i], quant * 50);
                    }
                    Item47 it = new Item47();
                    it.category = 4;
                    it.id = (short) 158;
                    it.quantity = (short) quant;
                    conn.p.item.add_item_bag47(4, it);
                    //
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    conn.p.item.charInventory(3);
                    //
                    Service.send_notice_box(conn, "Berhasil menukar " + quant + " kotak mainan");
                }
                break;
            }
            case 11: {
                if (Manager.gI().event == 1) {
                    if (size != 1) {
                        return;
                    }
                    String value = m2.reader().readUTF();
                    if (!(Util.isnumber(value))) {
                        Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                        return;
                    }
                    int quant = Integer.parseInt(value);
                    if (quant > 500 || quant <= 0) {
                        Service.send_notice_box(conn, "Jumlah tidak valid!");
                        return;
                    }
                    //
                    short[] id = new short[]{153, 154, 155, 156};
                    for (int i = 0; i < id.length; i++) {
                        if (conn.p.item.total_item_by_id(4, id[i]) < (quant)) {
                            Service.send_notice_box(conn, (ItemTemplate4.item.get(id[i]).getName() + " tidak cukup!"));
                            return;
                        }
                    }
                    for (int i = 0; i < id.length; i++) {
                        conn.p.item.remove(4, id[i], quant);
                    }
                    Event_1.add_material(conn.p.name, quant);
                    //
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    conn.p.item.charInventory(3);
                    //
                    Service.send_notice_box(conn, "Berhasil menyumbang bahan untuk membuat " + quant + " permen");
                }
                break;
            }
            case 12: {
                if (Manager.gI().event == 1) {
                    if (size != 1) {
                        return;
                    }
                    String value = m2.reader().readUTF();
                    if (!(Util.isnumber(value))) {
                        Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                        return;
                    }
                    int quant = Integer.parseInt(value);
                    if (quant > 500 || quant <= 0) {
                        Service.send_notice_box(conn, "Jumlah tidak valid!");
                        return;
                    }
                    //
                    if (conn.p.getGold() < (quant * 50_000)) {
                        Service.send_notice_box(conn, "Emas tidak cukup!");
                        return;
                    }
                    if (conn.p.item.total_item_by_id(4, 162) < (quant * 5)) {
                        Service.send_notice_box(conn, (ItemTemplate4.item.get(162).getName() + " tidak cukup!"));
                        return;
                    }
                    conn.p.updateGold(-(quant * 50_000));
                    conn.p.item.remove(4, 162, quant * 5);
                    //
                    Item47 it = new Item47();
                    it.category = 4;
                    it.id = 157;
                    it.quantity = (short) quant;
                    conn.p.item.add_item_bag47(4, it);
                    //
                    conn.p.item.charInventory(4);
                    conn.p.item.charInventory(7);
                    conn.p.item.charInventory(3);
                    //
                    Service.send_notice_box(conn, "Berhasil menukar " + quant + " kantong permen");
                }
                break;
            }
            case 13: {
                if (size != 1) {
                    return;
                }
                String name = m2.reader().readUTF();
                Pattern p = Pattern.compile("^[a-zA-Z0-9]{6,10}$");
                if (!p.matcher(name).matches()) {
                    Service.send_notice_box(conn, "Nama tidak valid, coba masukkan lagi!!");
                    return;
                }
                if (conn.p.myclan != null && !conn.p.myclan.members.get(0).name.equals(name)) {

                    conn.p.name_mem_clan_to_appoint = name;
                    Service.send_box_input_yesno(conn, 113, "Konfirmasi penyerahan posisi ketua kepada " + name);
                }
                break;
            }
            case 14: {
                if (size != 1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int coin_exchange = Integer.parseInt(value);
                if (coin_exchange < 1000 || coin_exchange > 300_000) {
                    Service.send_notice_box(conn, "Hanya bisa menukar minimal 1k dan maksimal 300k");
                    return;
                }
                if (conn.p.update_coin(-coin_exchange)) {
                    conn.p.updateGold((long) ((coin_exchange / 2) * 10_000));
                    conn.p.item.charInventory(5);
                    Service.send_notice_box(conn, "Tukar berhasil");
                    // Log.gI().add_log(conn.p.name,
                    // "đổi coin sang ngọc " + text + " : " + Util.number_format(ngoc_up) + "
                    // ngọc");
                } else {
                    Service.send_notice_box(conn, "Gagal, silakan coba lagi");
                }
                break;
            }
            case 15: {
                if (size != 1) {
                    return;
                }
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int quant = Integer.parseInt(value);
                if (quant > 32_000 || quant <= 0) {
                    Service.send_notice_box(conn, "Jumlah tidak valid!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tas penuh!");
                    return;
                }
                int quant_ngoc_can_create = conn.p.item.total_item_by_id(7, conn.p.id_hop_ngoc) / 5;
                if (quant > quant_ngoc_can_create) {
                    Service.send_notice_box(conn, "Jumlah item di tas tidak cukup!");
                    return;
                }
                int vang_required = GameSrc.getGoldPriceForGem(conn.p.id_hop_ngoc) * quant;
                if (conn.p.getGold() < vang_required) {
                    Service.send_notice_box(conn, "Emas tidak cukup: " + vang_required + " emas");
                    return;
                }
                conn.p.updateGold(-vang_required);
                conn.p.item.remove(7, conn.p.id_hop_ngoc, (quant * 5));
                Item47 itbag = new Item47();
                itbag.id = (short) (conn.p.id_hop_ngoc + 1);
                itbag.quantity = (short) quant;
                itbag.category = 7;
                conn.p.item.add_item_bag47(7, itbag);
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
                conn.p.id_hop_ngoc = -1;
                //
                Message m = new Message(-100);
                m.writer().writeByte(3);
                m.writer().writeUTF("Anda mendapatkan " + quant + " " + ItemTemplate7.item.get(itbag.id).getName());
                m.writer().writeShort(itbag.id);
                m.writer().writeByte(7);
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 17: {
                if (size != 1) {
                    return;
                }
                String betAmountStr = m2.reader().readUTF();
                if (!(Util.isnumber(betAmountStr))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int betAmount = Integer.parseInt(betAmountStr);

                GemSpin diamondGamble = GameEventManager.gI().getEvent(GemSpin.class);

                if (betAmount < diamondGamble.getMinBet() || conn.p.getGem() < betAmount) {
                    Service.send_notice_box(conn, "Permata tidak cukup!");
                    return;
                }
                if (betAmount > diamondGamble.getMaxBet()) {
                    Service.send_notice_box(conn, "Maksimal " + Util.number_format(diamondGamble.getMaxBet()));
                    return;
                }

                if (diamondGamble.join(conn.p, betAmount)) {
                    conn.p.sendNoticeBox("Berhasil bertaruh");
                }
                break;
            }
            case 18: {
                // FIX: sebelumnya ac_admin<=3 (khusus tier 4+). Sekarang mute
                // juga bisa dipakai SMOD (tier 1+) lewat menu chat "smod".
                if (conn.ac_admin < 2) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                String nameUser = m2.reader().readUTF();
                // Pattern p = Pattern.compile("^[a-zA-Z0-9@.]{1,15}$");
                // if ( !p.matcher(nameUser).matches() ) {
                // Service.send_notice_box(conn,"ký tự nhập vào không hợp lệ!!");
                // return;
                // }
                for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                    Session s = Session.SESSION_LIST.get(i);
                    if (s != null && s.p != null && s.p.name != null && s.p.name.equals(nameUser)) {
                        Session.SESSION_LIST.get(i).p.timeBlockCTG = utils._Time.GetTimeNextDay();
                        Service.send_notice_box(conn, "Berhasil membisukan karakter " + nameUser + " selama 1 hari.");
                        return;
                    }
                }
                Service.send_notice_box(conn, "Karakter tidak ditemukan atau tidak sedang online");

                break;
            }
            case 19: {
                // FIX: samain sama case 18 — sekarang SMOD (tier 1+) juga
                // boleh unmute, konsisten sama akses mute-nya.
                if (conn.ac_admin < 2) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                String nameUser = m2.reader().readUTF();
                for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                    Session s = Session.SESSION_LIST.get(i);
                    if (s != null && s.p != null && s.p.name != null && s.p.name.equals(nameUser)) {
                        Session.SESSION_LIST.get(i).p.timeBlockCTG = 0;
                        Service.send_notice_box(conn, "Berhasil melepas bisu dari karakter " + nameUser);
                        return;
                    }
                }
                break;
            }
            case 20: {
                String namePlayer = m2.reader().readUTF();
                conn.p.Store_Sell_ToPL = namePlayer;
                Service.send_notice_box(conn, "Berhasil mengatur penjualan hanya untuk karakter " + namePlayer);
                break;
            }
            case 21: {
                if (conn.ac_admin > 3) {
                    if (size != 3) {
                        return;
                    }
                    try {
                        Vgo v = new Vgo();
                        v.toMap = Byte.parseByte(m2.reader().readUTF());
                        v.toX = Short.parseShort(m2.reader().readUTF());
                        v.toY = Short.parseShort(m2.reader().readUTF());
                        conn.p.changeMap(conn.p, v);
                    } catch (Exception e) {
                        Service.send_notice_box(conn, "Terjadi kesalahan!");
                    }

                }
                break;
            }
            case 22: {
                if (size != 1) {
                    return;
                }
                String thue = m2.reader().readUTF();
                if (!(Util.isnumber(thue))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int thuechange = Byte.parseByte(thue);
                if (thuechange < 0 || thuechange > 5) {
                    Service.send_notice_box(conn, "Hanya bisa mengatur pajak antara 0 hingga 5%");
                    return;
                }
                if (conn.p.myclan == null || Manager.guildThue == null || !conn.p.myclan.equals(Manager.guildThue)) {
                    Service.send_notice_box(conn, "Hanya klan yang berhasil merebut benteng yang bisa mengatur pajak!");
                } else if (!Manager.guildThue.members.get(0).name.equals(conn.p.name)) {
                    Service.send_notice_box(conn, "Hanya ketua klan yang berhak melakukan tindakan ini!");
                } else {
                    Manager.thue = (byte) thuechange;
                    Service.send_notice_box(conn, "Anda telah mengubah tingkat pajak menjadi " + Manager.thue + " %");
                }
                break;
            }
            case 23: {
                String[] value = new String[]{m2.reader().readUTF(), m2.reader().readUTF()};
                if (!value[0].equals("") && !value[1].equals("")) {
                    // Service.send_notice_box(conn, "Bạn đã đặt tên bang là \"" + value[0] + "\" và
                    // tên viết tắt là \""
                    // + value[1] + "\" đúng không?\n đang test thôi nên éo có bang đâu kkk :v");
                    if (value[0].contains("_") || value[0].contains("-") || value[0].contains("@")
                            || value[0].contains("#")
                            || value[0].contains("^") || value[0].contains("$") || value[0].length() > 20
                            || value[0].length() < 4) {
                        Service.send_notice_box(conn, "Nama yang dimasukkan tidak valid");
                        return;
                    }
                    Pattern p = Pattern.compile("^[a-zA-Z0-9]{3,3}$");
                    if (!p.matcher(value[1]).matches()) {
                        Service.send_notice_box(conn, "Nama singkatan yang dimasukkan tidak valid");
                        return;
                    }
                    if (conn.p.getGem() < 20000) {
                        Service.send_notice_box(conn, "Permata tidak cukup, butuh 20k!");
                        return;
                    }
                    if (Guild.create_clan(conn, value[0], value[1])) {
                        conn.p.updateGem(-20000);
                        Log.gI().add_log(conn.p.name, "Membuat guild menghabiskan 20000 permata");
                        conn.p.item.charInventory(5);
                        Service.send_box_UI(conn, 20);
                        Service.send_notice_box(conn, "Silakan pilih ikon apa pun untuk dijadikan simbol");
                    }
                } else {
                    Service.send_notice_box(conn, "Tidak bisa membuat guild jika ada kolom kosong, kan?");
                }
                break;
            }
            case 24: {
                if (conn.ac_admin <= 3) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                try {
                    String type = m2.reader().readUTF();
                    String nameUser = m2.reader().readUTF();
                    if (type == null || type.isEmpty() || nameUser == null || nameUser.isEmpty()) {
                        Service.send_notice_box(conn, "Kolom data tidak boleh kosong!");
                        return;
                    }
                    int count = 0;
                    switch (type) {
                        case "1":
                            for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                                Session s = Session.SESSION_LIST.get(i);
                                if (s != null && s.user != null
                                        && s.user.toLowerCase().equals(nameUser.toLowerCase())) {
                                    count++;
                                    System.out.println("=============close session " + s.user);
                                    Session.SESSION_LIST.get(i).close();
                                }
                            }
                            Service.send_notice_box(conn,
                                    "Berhasil memutuskan " + count + " sesi dengan nama akun: " + nameUser);
                            break;
                        case "2":
                            for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                                Session s = Session.SESSION_LIST.get(i);
                                if (s != null && s.p != null && s.p.name != null
                                        && s.p.name.toLowerCase().equals(nameUser.toLowerCase())) {
                                    count++;
                                    System.out.println("=============close session " + s.user);
                                    Session.SESSION_LIST.get(i).close();
                                }
                            }
                            Service.send_notice_box(conn,
                                    "Berhasil memutuskan " + count + " sesi dengan nama karakter: " + nameUser);
                            break;
                        default:
                            Service.send_notice_box(conn, "Format jenis tidak benar:\n1: Nama akun \n2: Nama karakter");
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                    Service.send_notice_box(conn, "Terjadi kesalahan!");
                }

                break;
            }
            case 25:
            case 26:
            case 27: {
                String value = m2.reader().readUTF();
                if (!(Util.isnumber(value))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int quant = Integer.parseInt(value);
                if (quant > 200 || quant <= 0) {
                    Service.send_notice_box(conn, "Jumlah tidak valid!");
                    return;
                }
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_box(conn, "Tas penuh!");
                    return;
                }
                short id_cu = 306, id_moi = 307, chuyendoi = 30;
                long vag = quant * 100_000;
                if (idnpc == 26) {
                    id_cu = 306;
                    id_moi = 304;
                    chuyendoi = 10;
                    vag = quant * 25_000;
                } else if (idnpc == 27) {
                    id_cu = 304;
                    id_moi = 305;
                    chuyendoi = 5;
                    vag = quant * 500;
                }
                if (idnpc == 27 && vag > conn.p.getGem()) {
                    Service.send_notice_box(conn,
                            "Permata tidak cukup: " + vag + " permata untuk menukar " + quant + " ikat teratai");
                    return;
                } else if (vag > conn.p.getGold()) {
                    Service.send_notice_box(conn,
                            "Emas tidak cukup: " + vag + " emas untuk menukar " + quant + " ikat teratai");
                    return;
                }
                if (id_cu > (ItemTemplate4.item.size() - 1) || id_cu < 0 || id_moi > (ItemTemplate4.item.size() - 1)
                        || id_moi < 0) {
                    Service.send_notice_box(conn, "Terjadi kesalahan...");
                    return;
                }
                int quant_inbag = conn.p.item.total_item_by_id(4, id_cu);
                int quant_real = quant_inbag / chuyendoi;
                if (quant_real < quant) {
                    Service.send_notice_box(conn, "Hanya bisa menukar maksimal " + quant_real + " "
                            + ItemTemplate4.item.get(id_moi).getName());
                    return;
                }

                if (idnpc == 27) {
                    conn.p.updateGem(-(vag));
                } else {
                    conn.p.updateGold(-(vag));
                }
                Item47 itbag = new Item47();
                itbag.id = id_moi;
                itbag.quantity = (short) quant;
                itbag.category = 4;
                conn.p.item.remove(4, id_cu, quant * chuyendoi);
                conn.p.item.add_item_bag47(4, itbag);
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(5);

                Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", new short[]{id_moi},
                        new int[]{quant}, new short[]{4});
                break;
            }
            case 28: {
                String namep = m2.reader().readUTF();
                Player p0 = null;
                for (Player p1 : conn.p.map.players) {
                    if (p1.conn != null && p1.conn.connected && p1.name.equals(namep) && Math.abs(conn.p.x - p1.x) < 70
                            && Math.abs(conn.p.y - p1.y) < 70) {
                        p0 = p1;
                        break;
                    }
                }
                if (p0 == null) {
                    Service.send_notice_box(conn, "Anda dan orang yang ingin dilepaskan harus berdiri berdekatan");
                    break;
                }
                if (conn.p.item.get_bag_able() < 3) {
                    Service.send_notice_box(conn, "Anda membutuhkan 3 ruang kosong di tas!");
                    return;
                }
                if (conn.p.item.total_item_by_id(4, 303) > 0) {
                    // try{
                    conn.p.item.remove(4, 303, 1);
                    List<box_item_template> ids = new ArrayList<>();

                    List<Integer> it7 = new ArrayList<>(java.util.Arrays.asList(12, 13, 11, 3, 4, 8, 9, 10));
                    List<Integer> it7_vip = new ArrayList<>(java.util.Arrays.asList(14, 471, 346, 33));

                    List<Integer> it4 = new ArrayList<>(java.util.Arrays.asList(294, 275, 52, 18));
                    List<Integer> it4_vip = new ArrayList<>(java.util.Arrays.asList(206, 147, 131, 304, 306));
                    for (int i = 0; i < Util.random(1, 4); i++) {
                        int ran = Util.random(100);
                        if (ran < 0) {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(2, 5);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 2) { // sách
                            short idsach = (short) Util.random(4577, 4585);
                            ids.add(new box_item_template(idsach, (short) 1, (byte) 3));
                            conn.p.item.add_item_bag3_default(idsach, 0, false);
                        } else if (ran < 5) {// nlmd vang tim
                            short id = (short) Util.random(126, 146);
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 15) { // nltt
                            short id = (short) Util.random(417, 464);
                            short quant = (short) Util.random(2);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 28) {
                            short id = Util.random(it7_vip, new ArrayList<>()).shortValue();
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 45) {
                            short id = Util.random(it4_vip, new ArrayList<>()).shortValue();
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else if (ran < 70) {
                            short id = Util.random(it4, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 3);
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 3);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        }
                    }
                    ev_he.Event_3.add_DoiQua(conn.p.name, 1);
                    Service.Show_open_box_notice_item(conn.p, "Anda mendapatkan", ids);
                    // }catch(Exception e){e.printStackTrace();}
                }

                if (p0.item.get_bag_able() < 3) {
                    Service.send_notice_box(p0.conn,
                            "Butuh 3 ruang kosong di tas untuk menerima hadiah lampion dari " + conn.p.name);
                    return;
                }

                List<box_item_template> ids = new ArrayList<>();

                List<Integer> it7 = new ArrayList<>(java.util.Arrays.asList(1, 2, 3));
                List<Integer> it7_vip = new ArrayList<>(java.util.Arrays.asList(12, 8, 9, 10));
                List<Integer> it4 = new ArrayList<>(java.util.Arrays.asList(48, 49, 50, 51, 18, 10));
                List<Integer> it4_vip = new ArrayList<>(java.util.Arrays.asList(205, 207, 24, 52, 275, 84));
                for (int i = 0; i < Util.random(1, 3); i++) {
                    int ran = Util.random(100);
                    if (ran < 0) {
                        short id = Util.random(it7, new ArrayList<>()).shortValue();
                        short quant = (short) Util.random(2, 5);
                        ids.add(new box_item_template(id, quant, (byte) 7));
                        p0.item.add_item_bag47(id, quant, (byte) 7);
                    } else if (ran < 2) { // nltt
                        short id = (short) Util.random(417, 464);
                        short quant = (short) Util.random(2);
                        ids.add(new box_item_template(id, quant, (byte) 7));
                        p0.item.add_item_bag47(id, quant, (byte) 7);
                    } else if (ran < 12) {
                        short id = Util.random(it4_vip, new ArrayList<>()).shortValue();
                        short quant = (short) 1;
                        ids.add(new box_item_template(id, quant, (byte) 4));
                        p0.item.add_item_bag47(id, quant, (byte) 4);
                    } else if (ran < 27) {
                        short id = Util.random(it7_vip, new ArrayList<>()).shortValue();
                        short quant = (short) 1;
                        ids.add(new box_item_template(id, quant, (byte) 7));
                        p0.item.add_item_bag47(id, quant, (byte) 7);
                    } else if (ran < 45) {
                        short id = Util.random(it4, new ArrayList<>()).shortValue();
                        short quant = (short) Util.random(1, 3);
                        ids.add(new box_item_template(id, quant, (byte) 4));
                        p0.item.add_item_bag47(id, quant, (byte) 4);
                    } else if (ran < 70) {
                        short id = Util.random(it7, new ArrayList<>()).shortValue();
                        short quant = (short) Util.random(1, 3);
                        ids.add(new box_item_template(id, quant, (byte) 7));
                        p0.item.add_item_bag47(id, quant, (byte) 7);
                    } else {
                        short id = (short) Util.random(new int[]{2, 5});
                        short quant = (short) Util.random(100, 300);
                        ids.add(new box_item_template(id, quant, (byte) 4));
                        p0.item.add_item_bag47(id, quant, (byte) 4);
                    }
                }
                Service.Show_open_box_notice_item(p0, "Hadiah lampion dari " + conn.p.name, ids);
                break;

            }
            case 29: {
                String text = m2.reader().readUTF();
                if (!Util.isnumber(text)) {
                    Service.send_notice_box(conn, "Input bukan angka");
                    return;
                }


                short mobId = Short.parseShort(text);

                WorldBoss wb = GameEventManager.gI().getEvent(WorldBoss.class);
                if (wb == null) {
                    Service.send_notice_box(conn, "WorldBoss tidak terdaftar di event");
                    return;
                }

                if (conn.p.map.ismaplang) {
                    Service.send_notice_box(conn, "Ya engga dikota juga kali");
                    return;
                }

                MobInMap mob = wb.getBoss(mobId);
                if (mob != null) {
                    GameMap[] gGameMap = GameMap.getMapById(conn.p.map.mapId);
                    if (gGameMap == null) return;

                    MobInMap mobMap = new MobInMap();
                    mobMap.template = mob.template;
                    mobMap.name = mob.template.name;
                    mobMap.zone_id = gGameMap[conn.p.zone_id].zoneId;
                    mobMap.map_id = gGameMap[conn.p.zone_id].mapId;
                    mobMap.hp = mob.getMaxHP();
                    mobMap.setMaxHP(mob.getMaxHP());
                    mobMap.x = conn.p.x;
                    mobMap.y = conn.p.y;
                    mobMap.setBoss(true);
                    mobMap.isdie = false;
                    mobMap.is_boss_active = true;
                    mobMap.timeBossRecive = -1;
                    mobMap.item3.putAll(mob.item3);
                    mobMap.item4.putAll(mob.item4);
                    mobMap.item7.putAll(mob.item7);
                    gGameMap[conn.p.zone_id].Boss_entrys.add(mobMap);
                    Manager.gI().chatKTGprocess("" + mobMap.name + " Telah muncul di " + gGameMap[mobMap.zone_id].name);
                } else {
                    Service.send_notice_box(conn, "Mob tidak ditemukan");
                }
                break;

            }
            case 31: {
                String text = m2.reader().readUTF();
                if (!Util.isnumber(text)) {
                    Service.send_notice_box(conn, "Input bukan angka");
                    return;
                }

                byte mapId = Byte.parseByte(text);
                if (mapId > GameMap.entrys.size()) {
                    Service.send_notice_box(conn, "Mau kemana ?");
                    return;
                }

                Vgo v = new Vgo();
                v.toMap = mapId;
                v.toX = mapId == 102 ? 9 * 24 : conn.p.x;
                v.toY = mapId == 102 ? 9 * 24 : conn.p.y;
                conn.p.typepk = 2;
                conn.p.changeMap(conn.p, v);
                break;

            }
            case 32: {
                if (size != 1) {
                    return;
                }
                String betAmountStr = m2.reader().readUTF();
                if (!(Util.isnumber(betAmountStr))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int betAmount = Integer.parseInt(betAmountStr);

                QuickGoldSpin regularGoldSpin = GameEventManager.gI().getEvent(QuickGoldSpin.class);

                if (betAmount < regularGoldSpin.getMinBet() || conn.p.getGem() < betAmount) {
                    Service.send_notice_box(conn, "Gold tidak cukup!");
                    return;
                }
                if (betAmount > regularGoldSpin.getMaxBet()) {
                    Service.send_notice_box(conn, "Maksimal " + Util.number_format(regularGoldSpin.getMaxBet()));
                    return;
                }

                if (regularGoldSpin.join(conn.p, betAmount)) {
                    conn.p.sendNoticeBox("Berhasil bertaruh");
                }
                break;
            }
            case 33: {
                if (size != 1) {
                    return;
                }
                String betAmountStr = m2.reader().readUTF();
                if (!(Util.isnumber(betAmountStr))) {
                    Service.send_notice_box(conn, "Data yang dimasukkan bukan angka!!");
                    return;
                }
                int betAmount = Integer.parseInt(betAmountStr);

                QuickGemSpin regularDiamondSpin = GameEventManager.gI().getEvent(QuickGemSpin.class);

                if (betAmount < regularDiamondSpin.getMinBet() || conn.p.getGem() < betAmount) {
                    Service.send_notice_box(conn, "Permata tidak cukup!");
                    return;
                }
                if (betAmount > regularDiamondSpin.getMaxBet()) {
                    Service.send_notice_box(conn, "Maksimal " + Util.number_format(regularDiamondSpin.getMaxBet()));
                    return;
                }

                if (regularDiamondSpin.join(conn.p, betAmount)) {
                    conn.p.sendNoticeBox("Berhasil bertaruh");
                }
                break;
            }

            case 70: {
                // Broadcast Server Wide - dipanggil dari Menu_Admin case 29
                // atau Menu_Smod case 6.
                // FIX: sebelumnya ac_admin<4 (khusus tier 4+). Sekarang SMOD
                // (tier 1+) juga bisa broadcast lewat menu chat "smod".
                if (conn.ac_admin < 1) {
                    Service.send_notice_box(conn, "Anda tidak memiliki cukup izin!");
                    return;
                }
                if (size != 2) {
                    Service.send_notice_box(conn, "Format salah! Harus 2 kolom: Tipe & Pesan.");
                    return;
                }
                String tipeStr = m2.reader().readUTF().trim();
                String pesan   = m2.reader().readUTF().trim();

                if (pesan.isEmpty()) {
                    Service.send_notice_box(conn, "Pesan broadcast tidak boleh kosong!");
                    return;
                }
                if (pesan.length() > 200) {
                    Service.send_notice_box(conn, "Pesan terlalu panjang! Maksimal 200 karakter.");
                    return;
                }

                int tipe = 1;
                if (Util.isnumber(tipeStr)) {
                    tipe = Integer.parseInt(tipeStr);
                }
                if (tipe < 1 || tipe > 3) {
                    Service.send_notice_box(conn, "Tipe tidak valid!\n1 = Kotak\n2 = Kuning\n3 = Putih");
                    return;
                }

                // Label beda sesuai tier — biar player bisa bedain broadcast
                // dari SMOD (moderator junior) vs full admin.
                String label = conn.ac_admin >= 4 ? "[Admin]" : "[Moderator]";
                String pesanFinal = label + " " + pesan;
                int jumlahPenerima = 0;

                for (int i = 0; i < Session.SESSION_LIST.size(); i++) {
                    Session target = Session.SESSION_LIST.get(i);
                    if (target == null || target.p == null) continue;
                    try {
                        switch (tipe) {
                            case 1 -> Service.send_notice_box(target, pesanFinal);
                            case 2 -> Service.send_notice_nobox_yellow(target, pesanFinal);
                            case 3 -> Service.send_notice_nobox_white(target, pesanFinal);
                        }
                        jumlahPenerima++;
                    } catch (Exception ignored) {}
                }

                // Log aksi broadcast
                Log.gI().add_log(conn.p.name,
                        "BROADCAST [Tipe=" + tipe + "] ke " + jumlahPenerima + " pemain: " + pesan);

                Service.send_notice_box(conn,
                        "✅ Broadcast berhasil dikirim ke " + jumlahPenerima + " pemain online.\n"
                        + "Tipe: " + (tipe == 1 ? "Kotak" : tipe == 2 ? "Kuning" : "Putih") + "\n"
                        + "Pesan: " + pesan);
                break;
            }

            case 71: {
                // Cek Info Player - dipanggil dari Menu_Smod case 0
                if (conn.ac_admin < 1) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                String name = m2.reader().readUTF();
                Player target = GameMap.get_player_by_name(name);
                if (target == null || !target.isOnline) {
                    Service.send_notice_box(conn, "Player tidak ditemukan atau sedang offline");
                    return;
                }
                Service.send_notice_box(conn, String.format(
                        "Nama: %s\nLevel: %d\nMap: %d\nPosisi: %d, %d",
                        target.name, target.level,
                        target.map != null ? target.map.mapId : -1,
                        target.x, target.y));
                break;
            }
            case 72: {
                // Teleport ke Player - dipanggil dari Menu_Smod case 1
                if (conn.ac_admin < 2) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                String name = m2.reader().readUTF();
                Player target = GameMap.get_player_by_name(name);
                if (target == null || !target.isOnline || target.map == null) {
                    Service.send_notice_box(conn, "Player tidak ditemukan atau sedang offline");
                    return;
                }
                conn.p.changeMap(conn.p, Vgo.create(target.map.mapId, target.x, target.y));
                break;
            }
            case 73: {
                // Panggil Player - dipanggil dari Menu_Smod case 2
                if (conn.ac_admin < 2) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                String name = m2.reader().readUTF();
                Player target = GameMap.get_player_by_name(name);
                if (target == null || !target.isOnline || conn.p.map == null) {
                    Service.send_notice_box(conn, "Player tidak ditemukan atau sedang offline");
                    return;
                }
                target.changeMap(target, Vgo.create(conn.p.map.mapId, conn.p.x, conn.p.y));
                Service.send_notice_box(conn, "Player berhasil dipanggil");
                break;
            }
            case 74: {
                // Kirim Peringatan - dipanggil dari Menu_Smod case 3
                if (conn.ac_admin < 2) {
                    Service.send_notice_box(conn, "Tidak ada wewenang!");
                    return;
                }
                if (size != 2) {
                    Service.send_notice_box(conn, "Format salah! Harus 2 kolom: Nickname & Pesan.");
                    return;
                }
                String name = m2.reader().readUTF();
                String pesan = m2.reader().readUTF();
                Player target = GameMap.get_player_by_name(name);
                if (target == null || !target.isOnline) {
                    Service.send_notice_box(conn, "Player tidak ditemukan atau sedang offline");
                    return;
                }
                target.sendNoticeBox("[Peringatan dari Moderator]\n" + pesan);
                Service.send_notice_box(conn, "Peringatan berhasil dikirim");
                break;
            }

            default: {
                Service.send_notice_box(conn, "Terjadi kesalahan");
                break;
            }

        }
    }
}