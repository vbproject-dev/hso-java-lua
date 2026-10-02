package client;

import core.GameSrc;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import core.Manager;
import core.SaveData;
import core.ServerManager;
import core.Service;
import core.Util;
import event_daily.CastleSiegeManager;
import event_daily.Wedding;

import client.io.Message;
import client.io.Session;
import game.guild.Guild;
import lombok.extern.slf4j.Slf4j;
import game.map.Dungeon;
import game.map.DungeonManager;
import game.map.GameMap;
import game.map.MapService;
import menu.Dialog;
import template.ClanMember;
import template.Item3;
import template.Item47;
import template.Level;
import template.Option;
@Slf4j
public class Process_Yes_no_box {

    public static void process(Session conn, Message m) throws IOException {
        short id = m.reader().readShort(); // id
        if (id != conn.p.objectId) {
            return;

        }

        byte type = m.reader().readByte(); // type
        byte value = m.reader().readByte(); // value

        Dialog dialog = conn.p.getCurrentDialog();
        if (dialog != null && dialog.getId() == type) {
            dialog.perform(conn.p, value == 1, dialog.getArgs());
            log.info("Perform Dialog ID {}", type);
            return;
        }

        System.out.println("ID " + id + " type " + type + " value: " + value);
        if (value != 1) {
            switch (type) {
                case 110: {
                    Player p0 = GameMap.get_player_by_name(conn.p.in4_wedding[1]);
                    // "rất tiếc " + conn.p.name + " đã từ chối lời cầu hôn của bạn" -> "Maaf " + conn.p.name + " telah menolak lamaran Anda"
                    Service.send_notice_box(p0.conn, "Maaf " + conn.p.name + " telah menolak lamaran Anda");
                    conn.p.in4_wedding = null;
                    break;
                }

                case 115: {
                    conn.p.id_remove_time_use = -1;
                    break;
                }
                case 70: {
                    // "Cần 20k ngọc!" -> "Butuh 20k permata!"
                    Service.send_notice_box(conn, "Butuh 20k permata!");
                    break;
                }
                case 126: {
                    conn.p.id_buffer_126 = -1;
                    conn.p.id_index_126 = -1;
                    break;
                }
                case 114: {
                    conn.p.id_wing_split = -1;
                    break;
                }
                case 113: {
                    conn.p.name_mem_clan_to_appoint = "";
                    break;
                }
            }
        } else {
            switch (type) {
                case 112: {
                    Wedding temp = Wedding.get_obj(conn.p.name);
                    if (temp.it.tier >= 30) {
                        Service.send_notice_box(conn, "Cincin pasangan sudah mencapai level maksimum +30!");
                        return;
                    }
                    if (temp.exp < Level.entrys.get(temp.it.tier).exp) {
                        // "chưa đủ 100% exp!" -> "EXP belum 100%!"
                        Service.send_notice_box(conn, "EXP belum 100%!");
                        return;
                    }
                    long vang_req = (2 * (temp.it.tier + 1)) * 2_000_000L;
                    int ngoc_req = (2 * (temp.it.tier + 1)) * 1_000;
                    if (conn.p.getGold() < vang_req) {
                        // "chưa đủ " + vang_req + " vàng!" -> "belum punya cukup " + vang_req + " emas!"
                        Service.send_notice_box(conn, "belum punya cukup " + vang_req + " emas!");
                        return;
                    }
                    if (conn.p.getGem() < ngoc_req) {
                        // "chưa đủ " + ngoc_req + " ngọc!" -> "belum punya cukup " + ngoc_req + " permata!"
                        Service.send_notice_box(conn, "belum punya cukup " + ngoc_req + " permata!");
                        return;
                    }
                    conn.p.updateGold(-vang_req);
                    conn.p.updateGem(-ngoc_req);
                    conn.p.item.charInventory(5);
                    boolean suc = 80 > Util.random(100);
                    if (suc) {
                        temp.exp -= Level.entrys.get(temp.it.tier).exp;
                        temp.it.tier++;
                        // FIX BUG 2: simpan perubahan tier & exp ke DB agar tidak reset setelah server restart
                        temp.saveItem();
                        // "nâng cấp thành công lên +\" + temp.it.tier -> "berhasil meningkatkan ke +" + temp.it.tier
                        Service.send_notice_box(conn, "berhasil meningkatkan ke +" + temp.it.tier);
                        conn.p.item.wear[23] = temp.it;
                        Service.sendPlayerWear(conn.p);
                        Service.sendMainCharInfo(conn.p);
                        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                        //
                        Player p0 = GameMap.get_player_by_name(temp.name_1.equals(conn.p.name) ? temp.name_2 : temp.name_1);
                        if (p0 != null) {
                            p0.item.wear[23] = temp.it;
                            Service.sendPlayerWear(p0);
                            Service.sendMainCharInfo(p0);
                            MapService.broadcastMainCharInfo(p0.map, p0);
                        }
                    } else {
                        // Penalti gagal upgrade: kurangi 10% exp dari threshold tier saat ini
                        long penalty = Level.entrys.get(temp.it.tier).exp / 10;
                        temp.exp = Math.max(0, temp.exp - penalty);
                        temp.saveItem();
                        Service.send_notice_box(conn, "peningkatan gagal! EXP berkurang " + penalty + " poin.");
                    }
                    break;
                }
                case 111: {
                    Wedding temp = Wedding.get_obj(conn.p.name);
                    conn.p.item.wear[23] = null;
                    Service.sendPlayerWear(conn.p);
                    Service.sendMainCharInfo(conn.p);
                    MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                    Service.send_notice_box(conn, "Berhasil bercerai");
                    conn.p.it_wedding = null;
                    if (temp != null) {
                        Player p0 = GameMap.get_player_by_name(temp.name_1.equals(conn.p.name) ? temp.name_2 : temp.name_1);
                        if (p0 != null) {
                            p0.item.wear[23] = null;
                            Service.sendPlayerWear(p0);
                            Service.sendMainCharInfo(p0);
                            MapService.broadcastMainCharInfo(p0.map, p0);
                            Service.send_notice_box(p0.conn, conn.p.name + " telah meninggalkan Anda");
                            p0.it_wedding = null;
                        }
                        Wedding.remove_wed(temp); // hapus dari list + DELETE dari DB
                    }
                    break;
                }
                case 110: {
                    Player p0 = GameMap.get_player_by_name(conn.p.in4_wedding[1]);
                    Wedding.add_new(Integer.parseInt(conn.p.in4_wedding[0]), p0, conn.p);
                    conn.p.in4_wedding = null;
                    // "chúc mừng " + conn.p.name + " trở thành bạn đời của bạn" -> "selamat " + conn.p.name + " telah menjadi pasangan Anda"
                    Service.send_notice_box(p0.conn, "selamat " + conn.p.name + " telah menjadi pasangan Anda");
                    // "chúc mừng " + p0.name + " trở thành bạn đời của bạn" -> "selamat " + p0.name + " telah menjadi pasangan Anda"
                    Service.send_notice_box(conn, "selamat " + p0.name + " telah menjadi pasangan Anda");
                    break;
                }
                case 97: {
                    conn.p.Store_Sell_ToPL = "no name";
                    // "Bán riêng cho nhân vật", new String[]{"Tên nhân vật"} -> "Jual secara pribadi kepada karakter", new String[]{"Nama karakter"}
                    Service.sendBoxInputText(conn, 20, "Jual secara pribadi kepada karakter", new String[]{"Nama karakter"});
                    break;
                }
                case 113: {
                    if (conn.p.name_mem_clan_to_appoint.isEmpty()) {
                        return;
                    }
                    if (conn.p.myclan != null && conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                        boolean suc = false;
                        for (int i = 1; i < conn.p.myclan.members.size(); i++) {
                            if (conn.p.myclan.members.get(i).name.equals(conn.p.name_mem_clan_to_appoint)) {
                                ClanMember temp = conn.p.myclan.members.get(0);
                                //
                                conn.p.myclan.members.get(i).memberType = 127;
                                conn.p.myclan.members.get(0).memberType = 122;
                                //
                                conn.p.myclan.members.set(0, conn.p.myclan.members.get(i));
                                conn.p.myclan.members.set(i, temp);

                                //
                                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                                MapService.send_in4_other_char(conn.p.map, conn.p, conn.p);
                                Service.sendMainCharInfo(conn.p);

                                Player p0 = GameMap.get_player_by_name(conn.p.myclan.members.get(0).name);
                                if (p0 != null) {
                                    MapService.broadcastMainCharInfo(p0.map, p0);
                                    MapService.send_in4_other_char(p0.map, p0, p0);
                                    Service.sendMainCharInfo(p0);
                                }
                                //
                                suc = true;
                                break;
                            }
                        }
                        if (suc) {
                            // "Thành công!" -> "Berhasil!"
                            Service.send_notice_box(conn, "Berhasil!");
                        } else {
                            // "Tên không tồn tại" -> "Nama tidak ditemukan"
                            Service.send_notice_box(conn, "Nama tidak ditemukan");
                        }
                    } else {
                        // "Đã xảy ra lỗi" -> "Terjadi sebuah kesalahan"
                        Service.send_notice_box(conn, "Terjadi sebuah kesalahan");
                    }
                    break;
                }
                case 114: {
                    Item3 item = null;
                    int count = 0;
                    for (int i = 0; i < conn.p.item.bag3.length; i++) {
                        Item3 it = conn.p.item.bag3[i];
                        if (it != null && it.type == 7 && it.tier > 0) {
                            if (count == conn.p.id_wing_split) {
                                item = it;
                                break;
                            }
                            count++;
                        }
                    }
                    if (item != null) {

                        int quant1 = 40;
                        int quant2 = 10;
                        int quant3 = 50;
                        for (int i = 0; i < item.tier; i++) {
                            quant1 += GameSrc.wing_upgrade_material_long_khuc_xuong[i];
                            quant2 += GameSrc.wing_upgrade_material_kim_loai[i];
                            quant3 += GameSrc.wing_upgrade_material_da_cuong_hoa[i];
                            if ((i + 1) == 10 || (i + 1) == 20 || (i + 1) == 30) {
                                item.part--;
                            }
                        }
                        if (item.tier > 15) {
                            quant1 /= 2;
                            quant2 /= 2;
                            quant3 /= 2;
                        } else {
                            quant1 /= 3;
                            quant2 /= 3;
                            quant3 /= 3;
                        }
                        short[] id_ = new short[]{8, 9, 10, 11, 3, 0};
                        int[] quant_ = new int[]{quant1, quant1, quant1, quant1, quant2, quant3};
                        for (int i = 0; i < id_.length; i++) {
                            Item47 it = new Item47();
                            it.category = 7;
                            it.id = id_[i];
                            it.quantity = (short) quant_[i];
                            conn.p.item.add_item_bag47(7, it);
                        }
                        //
//                                                item = null;
                        count = 0;
                        for (int i = 0; i < conn.p.item.bag3.length; i++) {

                            Item3 it = conn.p.item.bag3[i];
                            if (it != null && it.type == 7 && it.tier > 0) {
                                if (count == conn.p.id_wing_split) {
                                    conn.p.item.bag3[i] = null;
                                    break;
                                }
                                count++;
                            }
                        }
                        conn.p.id_wing_split = -1;
//                                                for (int i = 0; i < item.op.size(); i++) {
//                                                if ((item.op.get(i).id > 26 || item.op.get(i).id < 23)
//                          && (item.op.get(i).id != 41 && item.op.get(i).id != 42)) {
//                                                    item.op.remove(i--);
//                                                }
//                                            }
                        //
                        conn.p.item.charInventory(4);
                        conn.p.item.charInventory(7);
                        conn.p.item.charInventory(3);
                        // "Thành công" -> "Berhasil"
                        Service.send_notice_box(conn, "Berhasil");
                    } else {
                        // "Có lỗi xảy ra, hãy thử lại!" -> "Terjadi kesalahan, silakan coba lagi!"
                        Service.send_notice_box(conn, "Terjadi kesalahan, silakan coba lagi!");
                        conn.p.id_wing_split = -1;
                    }
                    break;
                }
                case 9: {
                    if (conn.p.map.isMapChienTruong()) {

                    } else if (conn.p.map.isCastleSiegeMap()) {
                        CastleSiegeManager.ActionHoiSinh(conn.p.map, conn.p);
                    } else {
                        if (conn.p.getGem() >= 5) {
                            conn.p.isdie = false;
                            conn.p.hp = conn.p.body.getMaxHP();
                            conn.p.mp = conn.p.body.getMaxMP();
                            conn.p.updateGem(-5);
                            conn.p.item.charInventory(5);
                            Service.sendMainCharInfo(conn.p);
                            // chest in4 -> info peti
                            Service.send_combo(conn);
                            Service.usepotion(conn.p, 0, conn.p.body.getMaxHP());
                            Service.usepotion(conn.p, 1, conn.p.body.getMaxMP());
                        } else {
                            // "Không đủ ngọc để thực hiện" -> "Permata tidak cukup untuk melakukan ini"
                            Service.send_notice_box(conn, "Permata tidak cukup untuk melakukan ini");
                        }
                    }
                    break;
                }
                case 86: {
                    Manager.gI().vxmm.send_in4(conn.p);
                    break;
                }
                case 87: {
                    Manager.gI().vxkc.send_in4(conn.p);
                    break;
                }
                case 94: {
                    GameSrc.ChangeCS_Medal(conn, 94);
                    break;
                }
                case 98: {
                    GameSrc.ChangeCS_Medal(conn, 98);
                    break;
                }

                case 115: {
                    if (conn.p.id_remove_time_use != -1) {
                        Item3 it = conn.p.item.bag3[conn.p.id_remove_time_use];
                        if (it != null && it.time_use > 0) {
                            int ngoc_ = conn.p.getGem();
                            if (ngoc_ > 4) {
                                long price = it.time_use - System.currentTimeMillis();
                                price /= 30_600_000;
                                price = (price > 4) ? (price + 1) : 5;
                                boolean ch = false;
                                if (ngoc_ >= price) {
                                    ch = true;
                                } else {
                                    price = ngoc_;
                                }
                                it.time_use -= (price * 30_600_000);
                                conn.p.updateGem(-price);
                                conn.p.item.charInventory(4);
                                conn.p.item.charInventory(7);
                                conn.p.item.charInventory(3);
                                conn.p.id_remove_time_use = -1;
                                if (ch) {
                                    // "Nhận được " + it.name + " +" + it.tier + "!" -> "Menerima " + it.name + " +" + it.tier + "!"
                                    Service.send_notice_box(conn, "Menerima " + it.name + " +" + it.tier + "!");
                                }
                            } else {
                                // "Tối thiểu 5 ngọc!" -> "Minimal 5 permata!"
                                Service.send_notice_box(conn, "Minimal 5 permata!");
                            }
                        }
                    }
                    break;
                }
                case 116: {
                    if (conn.p.myclan != null && conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                        conn.p.myclan.remove_all_mem();
                        conn.p.myclan.remove_mem(conn.p.name);
                        conn.p.myclan = null;
                        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                        MapService.send_in4_other_char(conn.p.map, conn.p, conn.p);
                        Service.sendMainCharInfo(conn.p);
                        // "Hủy bang thành công" -> "Berhasil membubarkan klan"
                        Service.send_notice_box(conn, "Berhasil membubarkan klan");
                    }
                    break;
                }
                case 117: {
                    if (conn.p.myclan != null) {
                        conn.p.myclan.remove_mem(conn.p.name);
                        conn.p.myclan = null;
                        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                        MapService.send_in4_other_char(conn.p.map, conn.p, conn.p);
                        Service.sendMainCharInfo(conn.p);
                        // "Rời bang thành công" -> "Berhasil meninggalkan klan"
                        Service.send_notice_box(conn, "Berhasil meninggalkan klan");
                    }
                    break;
                }
                case 118: {
                    if (conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                        if ((Guild.vang_upgrade[1] * conn.p.myclan.level) > conn.p.myclan.get_vang()) {
                            // "Không đủ vàng để thực hiện" -> "Emas tidak cukup untuk melakukan ini"
                            Service.send_notice_box(conn, "Emas tidak cukup untuk melakukan ini");
                            return;
                        }
                        if ((Guild.ngoc_upgrade[1] * conn.p.myclan.level) > conn.p.myclan.get_ngoc()) {
                            // "Không đủ ngọc để thực hiện" -> "Permata tidak cukup untuk melakukan ini"
                            Service.send_notice_box(conn, "Permata tidak cukup untuk melakukan ini");
                            return;
                        }
                        conn.p.myclan.updateGold(-Guild.vang_upgrade[1] * conn.p.myclan.level);
                        conn.p.myclan.updateGem(-Guild.ngoc_upgrade[1] * conn.p.myclan.level);
                        conn.p.myclan.level++;
                        conn.p.myclan.exp = 0;
                        if (conn.p.myclan.maxMember < 45 && conn.p.myclan.level % 5 == 0) {
                            conn.p.myclan.maxMember += 5;
                        }
                        // "Nâng bang lên cấp " + conn.p.myclan.level + " thành công" -> "Berhasil meningkatkan klan ke level " + conn.p.myclan.level
                        Service.send_notice_box(conn, "Berhasil meningkatkan guild ke level " + conn.p.myclan.level);
                    }
                    break;
                }
                case 119: {
                    if (conn.p.point_active[0] != 10) {
                        if (conn.p.getGem() < 30) {
                            // "Bạn không đủ ngọc để tham gia!" -> "Permata Anda tidak cukup untuk bergabung!"
                            Service.send_notice_box(conn, "Permata Anda tidak cukup untuk bergabung!");
                            return;
                        }
                        conn.p.updateGem(-30);
                        conn.p.item.char_chest(5);
                    }
                    Dungeon d = DungeonManager.get_list(conn.p.name);
                    if (d == null) {
                        try {
                            d = new Dungeon();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (conn.p.point_active[0] <= 0)
                            // "Hãy quay lại vào ngày hôm sau!" -> "Kembali lagi besok!"
                            Service.send_notice_box(conn, "Kembali lagi besok!");
                        else if (d != null) {
                            conn.p.point_active[0]--;
                            //
                            d.name_party = conn.p.name;
                            d.setMode(0);
                            //
                            MapService.leave(conn.p.map, conn.p);
                            conn.p.map = d.template;
                            conn.p.x = 584;
                            conn.p.y = 672;
                            MapService.enter(conn.p.map, conn.p);
                            d.send_map_data(conn.p);
                            //
                            DungeonManager.add_list(d);
                        } else {
                            // "Lỗi, hãy thử lại sau!" -> "Kesalahan, silakan coba lagi nanti!"
                            Service.send_notice_box(conn, "Kesalahan, silakan coba lagi nanti!");
                        }
                    } else {
                        MapService.leave(conn.p.map, conn.p);
                        conn.p.map = d.template;
                        MapService.enter(conn.p.map, conn.p);
                        d.send_map_data(conn.p);
                        d.send_mob_move_when_exit(conn.p);
                    }
                    break;
                }
                case 70: {
                    if (conn.p.getGem() < 20000) {
                        // "20k ngọc còn không có thì không xứng đáng làm anh hùng!" -> "Jika Anda tidak punya 20k permata, Anda tidak pantas menjadi pahlawan!"
                        Service.send_notice_box(conn, "Jika Anda tidak punya 20k permata, Anda tidak pantas menjadi pahlawan!");
                        return;
                    }
                    // "Bang hội" -> "Klan"
                    // "Tên (4-20 ký tự) :", "Tên viết tắt (3 ký tự) :" -> "Nama (4-20 karakter) :", "Nama singkatan (3 karakter) :"
                    Service.sendBoxInputText(conn, 23, "Klan", new String[]{"Nama (4-20 karakter) :", "Nama singkatan (3 karakter) :"});
                    break;
                }
                case 122: {
                    int fee = 100 * conn.p.item.bag3[conn.p.item_replace].tier;
                    if (conn.p.getGem() < fee) {
                        // "Không đủ " + fee + " ngọc!" -> "Tidak cukup " + fee + " permata!"
                        Service.send_notice_box(conn, "Tidak cukup " + fee + " permata!");
                        conn.p.item_replace = -1; // kondom 
                        conn.p.item_replace2 = -1; // kondom
                        return;
                    }
                    conn.p.item.bag3[conn.p.item_replace2].tier = conn.p.item.bag3[conn.p.item_replace].tier;
                    conn.p.item.bag3[conn.p.item_replace].tier = 0;
                    if (conn.p.item.bag3[conn.p.item_replace2].type == 5
                        && conn.p.item.bag3[conn.p.item_replace2].tier >= 9) {
                            for (Option op_ : conn.p.item.bag3[conn.p.item_replace2].op) {
                                if (op_.id == 37 && op_.getParam(conn.p.item.bag3[conn.p.item_replace2].tier) > 1 
                            ) {
                               op_.setParam(op_.getParam(conn.p.item.bag3[conn.p.item_replace2].tier));     
                                }
                            } 
                        }
                    conn.p.updateGem(-fee);
                    conn.p.item.charInventory(3);
                    // "Chuyển hóa thành công!" -> "Berhasil bertransformasi!"
                    Service.send_notice_box(conn, "Berhasil bertransformasi!");
                    //
                    Message m3 = new Message(73);
                    m3.writer().writeByte(0);
                    m3.writer().writeShort(conn.p.item_replace2);
                    m3.writer().writeByte(0);
                    conn.addmsg(m3);
                    m3.cleanup();
                    //
                    m3 = new Message(73);
                    m3.writer().writeByte(0);
                    m3.writer().writeShort(conn.p.item_replace);
                    m3.writer().writeByte(1);
                    conn.addmsg(m3);
                    m3.cleanup();
                    //
                    conn.p.item_replace = -1; // kondom
                    conn.p.item_replace2 = -1; // kondom
                    break;
                }
                case 123: {
                    break;
                }
                case 124: {
                    conn.p.rest_skill_point();
                    conn.p.item.remove(4, 7, 1);
                    conn.p.item.charInventory(4);
                    // "Tẩy điểm kỹ năng thành công" -> "Berhasil mereset poin skill"
                    Service.send_notice_box(conn, "Berhasil mereset poin skill");
                    break;
                }
                case 125: {
                    conn.p.rest_potential_point();
                    conn.p.item.remove(4, 6, 1);
                    conn.p.item.charInventory(4);
                    // "Tẩy điểm tiềm năng thành công" -> "Berhasil mereset poin potensi"
                    Service.send_notice_box(conn, "Berhasil mereset poin potensi");
                    break;
                }
                case 126: {
                    if (conn.p.id_buffer_126 != -1) {
                        Item3 temp3 = conn.p.item.bag3[conn.p.id_buffer_126];
                        temp3.islock = true;
                        switch (temp3.type) {
                            case 0: // coat -> baju
                            case 1: // pant -> celana
                            case 2: // crown -> mahkota
                            case 3: // grove -> sarung tangan
                            case 4: // ring -> cincin
                            case 5: // chain -> kalung
                            case 6: // shoes -> sepatu
                            case 7: // wing -> sayap
                            case 15:
                            case 8:
                            case 9:
                            case 10:
                            case 16:
                            case 21:
                            case 22:
                            case 23:
                            case 24:
                            case 25:
                            case 26:
                            case 29:
                            case 11: { // weapon -> senjata
                                conn.p.playerWear(temp3, conn.p.id_buffer_126, conn.p.id_index_126);
                                break;
                            }
                            default: {
                                // "Ấn 2 lần mới có thể sử dụng" -> "Tekan 2 kali untuk bisa menggunakan"
                                Service.send_notice_nobox_white(conn, "Tekan 2 kali untuk bisa menggunakan");
                                break;
                            }
                        }
                    }
                    conn.p.id_buffer_126 = -1;
                    conn.p.id_index_126 = -1;
                    break;
                }
                case 88: {
                    if (conn.ac_admin < 10) {

                        Service.send_notice_box(conn, "Anda tidak memiliki wewenang yang cukup untuk melakukan ini!");
                        return;
                    }
                    Service.send_notice_nobox_yellow(conn, "Tombol Aktivasi Nuklir telah di tekan. Server akan dimatikan dalam 1 menit");
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

                    scheduler.schedule(() -> {
                        try {
                            ServerManager.gI().close();
                            SaveData.process();
                            for (int k = Session.SESSION_LIST.size() - 1; k >= 0; k--) {

                                try {
                                    Session.SESSION_LIST.get(k).p = null;
                                    Session.SESSION_LIST.get(k).close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Manager.gI().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, 1, TimeUnit.MINUTES);
                    scheduler.shutdown();
                    break;
                }
            }
        }
    }
}