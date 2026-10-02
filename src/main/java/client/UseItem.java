package client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.Manager;
import core.Service;
import core.Util;
import client.io.Message;
import client.io.Session;

import game.items.ItemManager;
import game.items.box.Box;
import game.items.box.ItemBoxManager;
import game.items.box.Reward;
import game.items.models.DisabledItem;
import lombok.extern.slf4j.Slf4j;
import game.map.LeaveItemMap;
import game.map.GameMap;
import game.map.MapService;
import model.map.Vgo;
import menu.Dialog;
import menu.InputDialog;
import game.mount.Mount;
import template.EffTemplate;
import template.Item3;
import template.Item47;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.Level;
import template.Option;
import template.Pet_di_buon;
import template.Pet_di_buon_manager;
import template.box_item_template;
import utils.SQLHelper;

@Slf4j
public class UseItem {

    public static void ProcessItem4(Session conn, Message m2) throws IOException {
        short id = m2.reader().readShort();

        DisabledItem disabled = ItemManager.getInstance().getDisabled(id, 4);
        if (disabled != null) {
            conn.p.sendNoticeBox(disabled.getReason());
            return;
        }

        // Cooldown 1.5 detik antar penggunaan box — cegah spam packet
        Box boxCheck = ItemBoxManager.gI().getBoxById(id);
        if (boxCheck != null) {
            if (conn.p.time_use_box > System.currentTimeMillis()) {
                return;
            }
            conn.p.time_use_box = System.currentTimeMillis() + 1500L;
        }

        if (conn.p.item.total_item_by_id(4, id) > 0) {
            if (ItemTemplate4.item.get(id).getType() == 44) {
                if (conn.p.mount != null) {
                    conn.p.takeOffMount();
                }
                use_item_mount(conn, id);
            } else {
                use_item4_default(conn, id);
            }
            conn.p.item.charInventory(4);
            conn.p.item.charInventory(7);
            conn.p.item.charInventory(3);
        } else if (conn.p.myclan != null && conn.p.myclan.check_id(id)) {
            if (ItemTemplate4.item.get(id).getType() == 44) {
                if (conn.p.mount != null) {
                    conn.p.takeOffMount();
                }
                use_item_mount(conn, id);
            } else {
                use_item4_default(conn, id);
                conn.p.item.charInventory(4);
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(3);
            }
        }

    }

    private static void use_item4_default(Session conn, short id_potion) throws IOException {

        // FIX: item BTF/arena (57-60, termasuk "ubah musuh jadi babi" id 58)
        // sebelumnya dicek BELAKANGAN, sesudah lookup ItemBoxManager.getBoxById()
        // di bawah. Kalau id item arena ini kebetulan juga kedaftar sebagai box
        // di ItemBoxManager (data box & item arena sama-sama dikelola manual per
        // ID, gampang bentrok tanpa sengaja), pemakaiannya bakal "dibajak" jadi
        // alur buka-box (dialog quantity / openBox) - kode arena (case 57-60 di
        // switch bawah) jadi TIDAK PERNAH tercapai sama sekali, item kelihatan
        // gak berefek apa-apa waktu dipakai. Cek & tangani item arena duluan di
        // sini, sebelum box, supaya prioritasnya pasti benar apapun isi data
        // box-nya nanti.
        if (id_potion == 57 || id_potion == 58 || id_potion == 59 || id_potion == 60) {
            if (GameMap.isBattlefieldMap(conn.p.map.mapId)) {
                MapService.use_item_arena(conn.p.map, conn.p, id_potion);
            } else {
                conn.p.sendNoticeBox("Item ini hanya bisa dipakai di area BTF/battlefield");
            }
            return;
        }

        Box box = ItemBoxManager.gI().getBoxById(id_potion);
        if (box != null) {
            Item47 potion = conn.p.item.getPotion(id_potion);
            if (potion == null) {
                conn.p.sendNoticeBox("Item tidak ditemukan");
                return;
            }

            if (potion.quantity > 1) {
                // Snapshot stok saat dialog dibuka, dibatasi maksimal 100
                final int stockAtOpen = Math.min(conn.p.item.total_item_by_id(4, id_potion), 100);
                InputDialog dialog = InputDialog
                        .builder()
                        .npcId(id_potion)
                        .title("Batch Open (Stok: " + conn.p.item.total_item_by_id(4, id_potion) + ", maks 100)")
                        .fields(List.of("Quantity (1 - " + stockAtOpen + ")"))
                        .action((player, arr) -> {
                            try {

                                int quantity = Integer.parseInt(arr[0]);

                                if (quantity <= 0) {
                                    player.sendNoticeBox("Jumlah tidak valid");
                                    return;
                                }

                                // Batas keras maksimal 100 per sekali buka
                                if (quantity > 100) {
                                    player.sendNoticeBox("Maksimal batch open adalah 100 box sekaligus.");
                                    return;
                                }

                                // Validasi stok aktual saat submit — cegah exploit bank NPC
                                int actualStock = player.item.total_item_by_id(4, id_potion);
                                if (actualStock <= 0) {
                                    player.sendNoticeBox("Item tidak ditemukan di inventory.");
                                    return;
                                }
                                if (quantity > actualStock) {
                                    player.sendNoticeBox("Jumlah melebihi stok! Stok saat ini: " + actualStock);
                                    return;
                                }
                                // Batasi juga dengan snapshot saat dialog dibuka
                                if (quantity > stockAtOpen) {
                                    player.sendNoticeBox("Jumlah melebihi batas saat box dibuka (" + stockAtOpen + ").");
                                    return;
                                }

                                if (player.item.get_bag_able() < 1) {
                                    player.sendNoticeBox("Inventory penuh! Harap kosongkan slot terlebih dahulu.");
                                    return;
                                }

                                if (player.item.get_bag_able() < quantity) {
                                    player.sendNoticeBox("Slot inventory tidak cukup untuk membuka " + quantity + " box. Slot tersedia: " + player.item.get_bag_able());
                                    return;
                                }

                                int opened = 0;
                                for (int i = 0; i < quantity; i++) {
                                    // Re-cek stok di setiap iterasi agar tidak overshoot jika item berubah
                                    if (player.item.total_item_by_id(4, id_potion) <= 0) {
                                        player.sendNoticeBox("Stok habis! Berhasil membuka " + opened + " dari " + quantity + " box.");
                                        break;
                                    }
                                    if (player.item.get_bag_able() < 1) {
                                        player.sendNoticeBox("Inventory penuh! Berhasil membuka " + opened + " dari " + quantity + " box.");
                                        break;
                                    }
                                    player.item.remove(4, id_potion, 1);
                                    openBox(player.conn, id_potion);
                                    opened++;
                                }

                                player.setCurrentInputDialog(null);
                            } catch (Exception ignore) {
                                player.sendNoticeBox("Terjadi kesalahan");
                            }

                        })
                        .build();

                conn.p.openInput(dialog);

                return;
            }
            if (conn.p.item.get_bag_able() < 1) {
                conn.p.sendNoticeBox("Inventory penuh! Harap kosongkan slot terlebih dahulu.");
                return;
            }
            // Validasi ulang stok sebelum open (cegah spam packet single box)
            if (conn.p.item.total_item_by_id(4, id_potion) <= 0) {
                conn.p.sendNoticeBox("Item tidak ditemukan di inventory.");
                return;
            }
            conn.p.item.remove(4, id_potion, 1);
            openBox(conn, id_potion);
            return;
        }


        switch (id_potion) {

            case 181, 182 -> {
                if (id_potion == 181) {
                    conn.p.point1 += 10;
                    conn.p.point2 += 10;
                } else {
                    conn.p.point3 += 10;
                    conn.p.point4 += 10;
                }

                conn.p.item.remove(4, id_potion, 1);
                Service.sendMainCharInfo(conn.p);

            }
            case 27, 90, 91 -> {
                int currentLevel = conn.p.level;
                if (currentLevel > 290) { 
                    conn.p.sendNoticeBox("Level sudah maksimal, tidak bisa menggunakan item ini.");
                    return;
                }
                if (currentLevel - 1 >= 0 && currentLevel - 1 < Level.entrys.size()) {
                    // FIX: pakai (currentLevel - 1), bukan currentLevel langsung.
                    // Konvensi di seluruh codebase: kebutuhan exp buat level L ada di
                    // Level.entrys.get(L - 1) (index 0 = level 1). Sebelumnya kode ini
                    // ambil entrys.get(currentLevel) tanpa -1, jadi malah ngitung bonus
                    // exp dari kebutuhan exp SATU LEVEL DI ATAS level pemain sekarang
                    // -> bonusExp yang dikasih ke player jadi kegedean & makin ngaco
                    // di level tinggi.
                    long totalExp = Level.entrys.get(currentLevel - 1).exp;
                    long bonusExp = Math.round(totalExp * 0.01); // 1% of total exp
                    if (id_potion == 90) {
                        bonusExp = Math.round(totalExp * 0.20); // 20% of total exp
                    } else if (id_potion == 91) {
                        bonusExp = Math.round(totalExp * 0.50); // 50% of total exp
                    }
                    conn.p.updateExp(bonusExp, false);
                    conn.p.item.remove(4, id_potion, 1);
                } else {
                    conn.p.sendNoticeBox("Level tidak valid");
                }

            }
            case 84 -> {
                if (conn.p.map.zoneId != conn.p.map.maxzone) {
                    Service.send_notice_box(conn, "Hanya bisa digunakan di area perdagangan");
                    return;
                }
                if (conn.p.item.wear[11] == null || (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3599
                        && conn.p.item.wear[11].id != 3600 && conn.p.item.wear[11].id != 3601)) {
                    Service.send_notice_box(conn, "Hanya bisa digunakan saat menjadi pedagang ");
                    return;
                }
                if (conn.p.pet_di_buon == null) {
                    conn.p.pet_di_buon = new Pet_di_buon(84, Manager.gI().get_index_mob_new(), conn.p.x, conn.p.y,
                            conn.p.map.mapId, conn.p.name, conn.p);
                    Pet_di_buon_manager.add(conn.p.name, conn.p.pet_di_buon);
                    //
                    Message m22 = new Message(4);
                    m22.writer().writeByte(1);
                    m22.writer().writeShort(131);
                    m22.writer().writeShort(conn.p.pet_di_buon.objectId);
                    m22.writer().writeShort(conn.p.pet_di_buon.x);
                    m22.writer().writeShort(conn.p.pet_di_buon.y);
                    m22.writer().writeByte(-1);
                    conn.addmsg(m22);
                    m22.cleanup();
                    //
                    conn.p.item.remove(4, id_potion, 1);
                } else {
                    Service.send_notice_box(conn,
                            "Anda sudah membawa 1 ekor!\nPosisi:\n"
                                    + GameMap.getMapById(conn.p.pet_di_buon.id_map)[0].name
                                    + "\n"
                                    + conn.p.pet_di_buon.x + " " + conn.p.pet_di_buon.y);
                }
            }
            case 86 -> {
                if (conn.p.map.zoneId != conn.p.map.maxzone) {
                    Service.send_notice_box(conn, "Hanya bisa digunakan di area perdagangan");
                    return;
                }
                if (conn.p.item.wear[11] == null || (conn.p.item.wear[11] != null && conn.p.item.wear[11].id != 3593
                        && conn.p.item.wear[11].id != 3594 && conn.p.item.wear[11].id != 3595)) {
                    Service.send_notice_box(conn, "Hanya bisa digunakan saat menjadi perampok ");
                    return;
                }
                if (conn.p.pet_di_buon == null) {
                    conn.p.pet_di_buon = new Pet_di_buon(86, Manager.gI().get_index_mob_new(), conn.p.x, conn.p.y,
                            conn.p.map.mapId, conn.p.name, conn.p);
                    Pet_di_buon_manager.add(conn.p.name, conn.p.pet_di_buon);
                    //
                    Message m22 = new Message(4);
                    m22.writer().writeByte(1);
                    m22.writer().writeShort(132);
                    m22.writer().writeShort(conn.p.pet_di_buon.objectId);
                    m22.writer().writeShort(conn.p.pet_di_buon.x);
                    m22.writer().writeShort(conn.p.pet_di_buon.y);
                    m22.writer().writeByte(-1);
                    conn.addmsg(m22);
                    m22.cleanup();
                    //
                    conn.p.item.remove(4, id_potion, 1);
                } else {
                    Service.send_notice_box(conn,
                            "Anda sudah membawa 1 ekor!\nPosisi:\n"
                                    + GameMap.getMapById(conn.p.pet_di_buon.id_map)[0].name
                                    + "\n"
                                    + conn.p.pet_di_buon.x + " " + conn.p.pet_di_buon.y);
                }
            }
            case 0, 1, 8, 9, 25, 2 -> {
                if (conn.p.time_use_poition_hp < System.currentTimeMillis()) {
                    conn.p.time_use_poition_hp = System.currentTimeMillis() + 2000L;
                    conn.p.item.remove(4, id_potion, 1);
                    int param = ItemTemplate4.item.get(id_potion).getValue();
                    param += ((param * 5 * conn.p.body.getSkillPoint(9)) / 100);
                    Service.usepotion(conn.p, 0, param);
                }
            }
            case 3, 4, 5 -> {
                if (conn.p.time_use_poition_mp < System.currentTimeMillis()) {
                    conn.p.time_use_poition_mp = System.currentTimeMillis() + 2000L;
                    conn.p.item.remove(4, id_potion, 1);
                    int param = ItemTemplate4.item.get(id_potion).getValue();
                    param += ((param * 5 * conn.p.body.getSkillPoint(10)) / 100);
                    Service.usepotion(conn.p, 1, param);
                }
            }
            case 6 -> {
                Service.send_box_input_yesno(conn, 125, "Harap konfirmasi Anda ingin mereset potensi");
            }
            case 7 -> {
                Service.send_box_input_yesno(conn, 124, "Mohon konfirmasi Anda ingin mereset skill");
            }
            case 10 -> {
                conn.p.item.remove(4, id_potion, 1);
                EffTemplate ef = conn.p.getEffectDefault(-125);
                if (ef != null) {
                    long time_extra = (ef.time - System.currentTimeMillis()) + (1000 * 60 * 120 - 1);
                    if (time_extra > (1000 * 60 * 60 * 24 * 3 - 1)) {
                        time_extra = 1000 * 60 * 60 * 24 * 3 - 1;
                    }
                    conn.p.add_EffDefault(-125, Manager.exp_boost_potion, (int) time_extra);
                } else {
                    conn.p.add_EffDefault(-125, Manager.exp_boost_potion, (1000 * 60 * 120 - 1));
                }
                conn.p.set_x2_xp(1);
            }
            case 11, 12, 13, 14, 15, 16 -> {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Tas penuh!");
                    return;
                }
                Item47 it = new Item47();
                it.id = (short) (id_potion - 11);
                it.quantity = (short) ItemTemplate4.item.get(id_potion).getValue();
                conn.p.item.add_item_bag47(4, it);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 19 -> {
                conn.p.item.remove(4, id_potion, 1);
                EffTemplate ef = conn.p.getEffectDefault(-125);
                if (ef != null) {
                    long time_extra = (ef.time - System.currentTimeMillis()) + (1000 * 60 * 30 - 1);
                    if (time_extra > (1000 * 60 * 60 * 24 * 3 - 1)) {
                        time_extra = 1000 * 60 * 60 * 24 * 3 - 1;
                    }
                    conn.p.add_EffDefault(-125, Manager.exp_boost_potion, (int) time_extra);
                } else {
                    conn.p.add_EffDefault(-125, Manager.exp_boost_potion, (1000 * 60 * 30 - 1));
                }
                conn.p.set_x2_xp(1);
            }
            case 24 -> {
                if (conn.p.hieuchien == 0) {
                    Service.send_notice_box(conn, "Karaktermu masih baik, belum perlu mencuci dosa!");
                    return;
                }
                conn.p.item.remove(4, id_potion, 1);
                conn.p.hieuchien = 0;
            }
            case 26 -> {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Tas penuh!");
                    return;
                }
                Item47 it = new Item47();
                it.id = (short) (id_potion - 1);
                it.quantity = (short) ItemTemplate4.item.get(id_potion).getValue();
                conn.p.item.add_item_bag47(4, it);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 69 -> {
                if (conn.p.pet_follow == -1) {
                    Service.send_notice_nobox_white(conn, "Belum membawa pet!");
                    return;
                }
                Pet pet = null;
                for (Pet temp : conn.p.mypet) {
                    if (temp.is_follow) {
                        pet = temp;
                    }
                }
                if (pet != null) {
                    if (pet.can_revolution()) {
                        pet.UpgradeLevel();
                        pet.exp = 0;
                        pet.spriteImage++;
                        Service.sendPlayerWear(conn.p);
                        conn.p.item.remove(4, id_potion, 1);
                    } else if (pet.level >= 29) {
                        Service.send_notice_box(conn, "Pet sudah mencapai level maksimal!");
                    } else {
                        Service.send_notice_nobox_white(conn, "Pet masih terlalu kecil!");
                    }
                }
            }
            case 142 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 1;
                vgo.toX = 492;
                vgo.toY = 366;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 241 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 103;
                vgo.toX = 282;
                vgo.toY = 186;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 164 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 88;
                vgo.toX = 456;
                vgo.toY = 360;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 166 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 89;
                vgo.toX = 456;
                vgo.toY = 360;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 168 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 90;
                vgo.toX = 456;
                vgo.toY = 360;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 170 -> {
                Vgo vgo = null;
                vgo = new Vgo();
                vgo.toMap = 91;
                vgo.toX = 456;
                vgo.toY = 360;
                conn.p.changeMap(conn.p, vgo);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 162 -> {
                // FIX: Util.random(n) itu inklusif 0..n, jadi item ini bisa
                // ngasih pesan "Anda mendapatkan: 0 exp" walau item udah
                // kepake/kehapus dari inventory. Kasih minimal 1 exp.
                int exp_add = Util.random(1, conn.p.level * 100);
                conn.p.updateExp(exp_add, false);
                int quant_item = Util.random(2, 5);
                short[] item_add = new short[]{13, 5, 25, 8, 9, 10, 11, 18, 131, 132, 133, 33, 44, 48, 49, 50, 51,
                        205, 206, 207, 142};
                byte[] item_type = new byte[]{7, 4, 4, 7, 7, 7, 7, 4, 4, 4, 4, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4};
                //
                Message m = new Message(78);
                m.writer().writeUTF("Anda mendapatkan: " + exp_add + " exp");
                m.writer().writeByte(quant_item); // size
                for (int i = 0; i < quant_item; i++) {
                    int index = Util.random(item_add.length - 1);
                    // if (25 > Util.random(110)) {
                    // index = Medal_Material.m_yellow[Util.random(Medal_Material.m_yellow.length)];
                    // }
                    m.writer().writeUTF(""); // name
                    m.writer().writeShort((item_type[index] == 4) ? ItemTemplate4.item.get(item_add[index]).getIcon()
                            : ItemTemplate7.item.get(item_add[index]).getIcon()); // icon
                    int quant_ = Util.random(1, 3);
                    m.writer().writeInt(quant_); // quantity
                    m.writer().writeByte(item_type[index]); // type in bag
                    m.writer().writeByte(0); // tier
                    m.writer().writeByte(0); // color
                    //
                    // m.writer().writeUTF(""); // name
                    // m.writer().writeShort(item2.getIcon()); // icon
                    // m.writer().writeInt(quant2_); // quantity
                    // m.writer().writeByte(7); // type in bag
                    // m.writer().writeByte(0); // tier
                    // m.writer().writeByte(0); // color
                    //
                    // m.writer().writeUTF(""); // name
                    // m.writer().writeShort(0); // icon
                    // m.writer().writeInt(quant3_); // quantity
                    // m.writer().writeByte(4); // type in bag
                    // m.writer().writeByte(0); // tier
                    // m.writer().writeByte(0); // color
                    Item47 itbag = new Item47();
                    itbag.id = item_add[index];
                    itbag.quantity = (short) quant_;
                    itbag.category = item_type[index];
                    conn.p.item.add_item_bag47(item_type[index], itbag);
                }
                m.writer().writeUTF("");
                m.writer().writeByte(1);
                m.writer().writeByte(0);
                conn.addmsg(m);
                m.cleanup();
                //
                conn.p.item.remove(4, id_potion, 1);
            }
            case 207, 205 -> {
                // ruong do
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Tas penuh!");
                    return;
                }
                List<Short> it_ = new ArrayList<>();
                if (conn.p.level < 30) {
                    it_.addAll(LeaveItemMap.item2x);
                } else if (conn.p.level < 40) {
                    it_.addAll(LeaveItemMap.item3x);
                } else if (conn.p.level < 50) {
                    it_.addAll(LeaveItemMap.item4x);
                } else if (conn.p.level < 60) {
                    it_.addAll(LeaveItemMap.item5x);
                } else if (conn.p.level < 70) {
                    it_.addAll(LeaveItemMap.item6x);
                } else if (conn.p.level < 80) {
                    it_.addAll(LeaveItemMap.item7x);
                } else if (conn.p.level < 90) {
                    it_.addAll(LeaveItemMap.item8x);
                } else if (conn.p.level < 100) {
                    it_.addAll(LeaveItemMap.item9x);
                } else if (conn.p.level < 110) {
                    it_.addAll(LeaveItemMap.item10x);
                } else if (conn.p.level < 120) {
                    it_.addAll(LeaveItemMap.item11x);
                } else if (conn.p.level < 130) {
                    it_.addAll(LeaveItemMap.item12x);
                } else if (conn.p.level < 140) {
                    it_.addAll(LeaveItemMap.item13x);
                }
                if (it_.size() < 1) {
                    Service.send_notice_box(conn, "Peti kosong!");
                    conn.p.item.remove(4, id_potion, 1);
                    return;
                }
                int id_item_can_drop = it_.get(Util.random(it_.size()));
                int dem = 0;
                if (id_potion == 207) {
                    while (dem < 50 && it_.size() > 2 && conn.p.level > 0
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 3)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 4)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 5)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 6)) {
                        id_item_can_drop = it_.get(Util.random(it_.size()));
                        dem++;
                    }
                } else {
                    while (dem < 50 && it_.size() > 2 && conn.p.level > 0
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 2
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 0
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 1
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 8
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 9
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 10
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)
                            && !(ItemTemplate3.item.get(id_item_can_drop).getType() == 11
                            && ItemTemplate3.item.get(id_item_can_drop).getClazz() == conn.p.clazz)) {
                        id_item_can_drop = it_.get(Util.random(it_.size()));
                        dem++;
                    }
                }
                if (dem >= 50) {
                    Service.send_notice_box(conn, "Peti kosong, haha");
                    conn.p.item.remove(4, id_potion, 1);
                    return;
                }
                byte color_ = (byte) Util.random(5);
                byte tier_ = (byte) 0;
                //
                short index_real = 0;
                String name = ItemTemplate3.item.get(id_item_can_drop).getName();
                for (int i = id_item_can_drop - 5; i < id_item_can_drop + 5; i++) {
                    if (ItemTemplate3.item.get(i).getName().equals(name)
                            && ItemTemplate3.item.get(i).getColor() == color_) {
                        index_real = (short) i;
                        break;
                    }
                }
                ItemTemplate3 item = ItemTemplate3.item.get(index_real);
                //
                Message m = new Message(78);
                if (id_potion == 207) {
                    m.writer().writeUTF("Peti ungu");
                } else if (id_potion == 205) {
                    m.writer().writeUTF("Peti merah");
                }
                m.writer().writeByte(1); // size
                for (int i = 0; i < 1; i++) {
                    m.writer().writeUTF(item.getName()); // name
                    m.writer().writeShort(item.getIcon()); // icon
                    m.writer().writeInt(1); // quantity
                    m.writer().writeByte(3); // type in bag
                    m.writer().writeByte(tier_); // tier
                    m.writer().writeByte(color_); // color
                }
                m.writer().writeUTF("");
                m.writer().writeByte(1);
                m.writer().writeByte(0);
                conn.addmsg(m);
                m.cleanup();
                //
                Item3 itbag = new Item3();
                itbag.id = item.getId();
                itbag.name = item.getName();
                itbag.clazz = item.getClazz();
                itbag.type = item.getType();
                itbag.level = item.getLevel();
                itbag.icon = item.getIcon();
                itbag.op = new ArrayList<>();
                itbag.op.addAll(item.getOp());
                itbag.color = item.getColor();
                itbag.part = item.getPart();
                itbag.tier = tier_;
                itbag.islock = false;
                itbag.time_use = 0;
                conn.p.item.add_item_bag3(itbag);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 206 -> {
                ItemTemplate7 item1 = ItemTemplate7.item.get(Util.random(8, 12));
                ItemTemplate7 item2 = ItemTemplate7.item.get((50 > Util.random(0, 100)) ? 0 : 3);
                int quant1_ = Util.random(1, 6);
                int quant2_ = Util.random(1, 6);
                int quant3_ = Util.random(300, 500);
                //
                Message m = new Message(78);
                m.writer().writeUTF("Peti emas");
                m.writer().writeByte(3); // size
                // for (int i = 0; i < 3; i++) {
                m.writer().writeUTF(""); // name
                m.writer().writeShort(item1.getIcon()); // icon
                m.writer().writeInt(quant1_); // quantity
                m.writer().writeByte(7); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                //
                m.writer().writeUTF(""); // name
                m.writer().writeShort(item2.getIcon()); // icon
                m.writer().writeInt(quant2_); // quantity
                m.writer().writeByte(7); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                //
                m.writer().writeUTF(""); // name
                m.writer().writeShort(0); // icon
                m.writer().writeInt(quant3_); // quantity
                m.writer().writeByte(4); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                // }
                m.writer().writeUTF("");
                m.writer().writeByte(1);
                m.writer().writeByte(0);
                conn.addmsg(m);
                m.cleanup();
                //
                Item47 itbag = new Item47();
                itbag.id = item1.getId();
                itbag.quantity = (short) quant1_;
                itbag.category = 7;
                conn.p.item.add_item_bag47(7, itbag);
                //
                Item47 itbag2 = new Item47();
                itbag2.id = item2.getId();
                itbag2.quantity = (short) quant2_;
                itbag2.category = 7;
                conn.p.item.add_item_bag47(7, itbag2);
                //
                conn.p.updateGold(quant3_);
                //
                conn.p.item.charInventory(7);
                conn.p.item.remove(4, id_potion, 1);
            }
            // PETI BOSS
            case 273 -> {

                List<Short> items = new ArrayList<>();
                List<Integer> quantities = new ArrayList<>();
                List<Short> catagories = new ArrayList<>();
                short[] equips = {
                        4988, 4992, 4996, 5000, 5004, 5008, 5012, 5016,
                        4956, 4960, 4964, 4968, 4972, 4976, 4980, 4984,

                        4990, 4994, 4998, 5002, 5006, 5010, 5014, 5018,
                        4958, 4962, 4966, 4970, 4974, 4978, 4982, 4986,

                        4989, 4993, 4997, 5001, 5005, 5009, 5013, 5017,
                        4957, 4961, 4965, 4969, 4973, 4977, 4981, 4985,

                        4991, 4995, 4998, 5003, 5007, 5011, 5015, 5019,
                        4959, 4963, 4967, 4971, 4975, 4979, 4983, 4987
                };

                for (int i = 0; i < Util.random(1, 3); i++) {

                    short item7 = (short) Util.random(246, 345);
                    Item47 potion = new Item47();
                    potion.id = item7;
                    potion.quantity = (short) Util.random(1, 6);
                    potion.category = 7;
                    items.add(item7);
                    quantities.add((int) potion.quantity);
                    catagories.add((short) potion.category);
                    conn.p.item.add_item_bag47(7, potion);
                }

                int chance = Util.random(0, 99);

                if (chance < 10) {
                    short id = equips[(int) (Math.random() * equips.length)];
                    Item3 item3 = Item3.fromTemplate(id);
                    item3.islock = false;
                    items.add(id);
                    quantities.add(1);
                    catagories.add((short) 3);
                    conn.p.item.add_item_bag3(item3);
                }

                // Book
                if (chance < 2) {
                    short book = (short) Util.random(4577, 4584);
                    ItemTemplate3 temp = ItemTemplate3.item.get(book);
                    Item3 equip = new Item3();
                    equip.id = temp.getId();
                    equip.name = temp.getName();
                    equip.icon = temp.getIcon();
                    equip.color = temp.getColor();
                    equip.part = temp.getPart();
                    equip.type = temp.getType();
                    equip.level = temp.getLevel();
                    equip.clazz = temp.getClazz();
                    equip.tier = 0;
                    equip.tierStar = 0;
                    equip.islock = false;
                    equip.op = temp.getOp();

                    items.add(book);
                    quantities.add(1);
                    catagories.add((short) 3);
                    conn.p.item.add_item_bag3(equip);

                }

                short[] ar_id = new short[items.size()];
                int[] ar_quant = new int[quantities.size()];
                short[] ar_type = new short[catagories.size()];
                for (int i = 0; i < ar_id.length; i++) {
                    ar_id[i] = items.get(i);
                    ar_quant[i] = quantities.get(i);
                    ar_type[i] = catagories.get(i);
                }


                Service.Show_open_box_notice_item(conn.p, "Kamu mendapatkan", ar_id, ar_quant, ar_type);

                conn.p.item.remove(4, id_potion, 1);
            }
            case 274 -> {
                ItemTemplate7 item1 = ItemTemplate7.item.get(Util.random(417, 456));
                ItemTemplate7 item2 = ItemTemplate7.item.get(Util.random(457, 463));
                int quant1_ = Util.random(10, 11);
                int quant2_ = Util.random(2, 3);
                //
                Message m = new Message(78);
                m.writer().writeUTF("Peti poin prestasi");
                m.writer().writeByte(3); // size
                // for (int i = 0; i < 3; i++) {
                m.writer().writeUTF(""); // name
                m.writer().writeShort(item1.getIcon()); // icon
                m.writer().writeInt(quant1_); // quantity
                m.writer().writeByte(7); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                //
                m.writer().writeUTF(""); // name
                m.writer().writeShort(item2.getIcon()); // icon
                m.writer().writeInt(quant2_); // quantity
                m.writer().writeByte(7); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                //
                m.writer().writeUTF(""); // name
                m.writer().writeShort(0); // icon
                m.writer().writeByte(4); // type in bag
                m.writer().writeByte(0); // tier
                m.writer().writeByte(0); // color
                // }
                m.writer().writeUTF("");
                m.writer().writeByte(1);
                m.writer().writeByte(0);
                conn.addmsg(m);
                m.cleanup();
                //
                Item47 itbag = new Item47();
                itbag.id = item1.getId();
                itbag.quantity = (short) quant1_;
                itbag.category = 7;
                conn.p.item.add_item_bag47(7, itbag);
                //
                Item47 itbag2 = new Item47();
                itbag2.id = item2.getId();
                itbag2.quantity = (short) quant2_;
                itbag2.category = 7;
                conn.p.item.add_item_bag47(7, itbag2);
                //
                //
                conn.p.item.charInventory(7);
                conn.p.item.charInventory(4);
                conn.p.item.remove(4, id_potion, 1);
            }
            case 318 -> {
                InputDialog dialog = InputDialog
                        .builder()
                        .title("Ubah Nama")
                        .fields(List.of("Nickname", "Kata Sandi"))
                        .action((player, args) -> {
                            String name = args[0];
                            String pass = args[1];

                            // only letters, no space, no special char
                            if (!name.matches("^[A-Za-z]+$")) {
                                player.sendNoticeBox("Nickname hanya boleh huruf A-Z tanpa spasi atau simbol!");
                                return;
                            }

                            // min length check
                            if (name.length() < 3) {
                                player.sendNoticeBox("Nickname minimal 3 huruf!");
                                return;
                            }

                            // min length check
                            if (!pass.equalsIgnoreCase(player.conn.pass)) {
                                player.sendNoticeBox("Kata sandi salah!");
                                return;
                            }

                            if (SQLHelper
                                    .selectFrom("player")
                                    .where("name", name)
                                    .exists()) {

                                player.sendNoticeBox("Nama telah digunakan");
                                return;
                            }

                            int len = name.length();
                            long price;
                            if (len == 3) {
                                price = 1_000_000;
                            } else if (len == 4) {
                                price = 500_000;
                            } else {
                                price = 200_000;
                            }

                            Dialog confirm = Dialog
                                    .builder()
                                    .id((byte) -4)
                                    .text(String.format("Kamu akan dikenakan biaya sebesar %d lanjutkan?", price))
                                    .args(java.util.Map.of("name", name, "price", price))
                                    .onRespond((p, yOrN, argsx) -> {
                                        try {


                                            if (yOrN) {

                                                String n = (String) argsx.get("name");
                                                long cost = (Long) argsx.get("price");

                                                if (p.getGem() < cost) {
                                                    p.sendNoticeBox("Tidak cukup permata");
                                                    return;
                                                }

                                                p.name = n;
                                                p.updateGem(-cost);
                                                p.item.charInventory(4);
                                                Service.sendMainCharInfo(p);

                                                SQLHelper
                                                        .update("player")
                                                        .where("id", p.objectId)
                                                        .set("name", n)
                                                        .execute();
                                            }
                                        } catch (Exception e) {
                                            p.sendNoticeBox("Terjadi Kesalahan");
                                        }
                                    })
                                    .build();

                            player.openDialog(confirm);

                        })
                        .build();


                conn.p.openInput(dialog);

            }
            case 261 -> {
                if (conn.p.level < 10 || conn.p.level == 20 || conn.p.level == 30 || conn.p.level == 40) {
                    Service.send_notice_nobox_white(conn, "Level tidak sesuai");
                    return;
                }
                conn.p.item.remove(4, id_potion, 1);
                if (conn.p.getlevelpercent() >= 500) {
                    conn.p.updateExp(-(Level.entrys.get(conn.p.level - 1).exp / 2), false);
                } else {
                    int levelchange = conn.p.level - 1;
                    long exp_add = (Level.entrys.get(levelchange - 1).exp * (500 + conn.p.getlevelpercent())) / 1000;
                    conn.p.level = (short) (levelchange - 1);
                    conn.p.exp = Level.entrys.get(levelchange - 2).exp - 1;
                    conn.p.tiemnang = (short) (1 + Level.get_tiemnang_by_level(conn.p.level - 1));
                    conn.p.kynang = (short) (1 + Level.get_kynang_by_level(conn.p.level - 1));
                    conn.p.point1 = (short) (4 + conn.p.level);
                    conn.p.point2 = (short) (4 + conn.p.level);
                    conn.p.point3 = (short) (4 + conn.p.level);
                    conn.p.point4 = (short) (4 + conn.p.level);
                    conn.p.skillPoint = new byte[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                    conn.p.updateExp(1 + exp_add, false);
                    //
                    Service.sendMainCharInfo(conn.p);
                    for (int i = 0; i < conn.p.map.players.size(); i++) {
                        Player p0 = conn.p.map.players.get(i);
                        if (p0.objectId != conn.p.objectId && (Math.abs(p0.x - conn.p.x) < 200)
                                && (Math.abs(p0.y - conn.p.y) < 200)) {
                            MapService.send_in4_other_char(p0.map, p0, conn.p);
                        }
                    }
                }
            }
            case 228, 229, 230, 231, 232, 233, 234 -> {
                short id_wing_recei = (short) (id_potion + 4414);
                boolean check = true;
                for (int i = 0; i < conn.p.item.bag3.length; i++) {
                    if (conn.p.item.bag3[i] != null && conn.p.item.bag3[i].id == id_wing_recei) {
                        check = false;
                        break;
                    }
                }
                if (conn.p.item.wear[14] != null && conn.p.item.wear[14].id == id_wing_recei) {
                    check = false;
                }
                if (check) {
                    Item3 itbag = new Item3();
                    itbag.id = id_wing_recei;
                    itbag.name = ItemTemplate3.item.get(id_wing_recei).getName();
                    itbag.clazz = ItemTemplate3.item.get(id_wing_recei).getClazz();
                    itbag.type = ItemTemplate3.item.get(id_wing_recei).getType();
                    itbag.level = 60;
                    itbag.icon = ItemTemplate3.item.get(id_wing_recei).getIcon();
                    itbag.op = new ArrayList<>();
                    //
                    itbag.op.add(new Option(7, Util.random(100, 500), itbag.id));
                    itbag.op.add(new Option(8, Util.random(100, 500), itbag.id));
                    itbag.op.add(new Option(9, Util.random(100, 500), itbag.id));
                    itbag.op.add(new Option(10, Util.random(100, 500), itbag.id));
                    itbag.op.add(new Option(11, Util.random(100, 500), itbag.id));
                    //
                    itbag.color = ItemTemplate3.item.get(id_wing_recei).getColor();
                    itbag.part = ItemTemplate3.item.get(id_wing_recei).getPart();
                    itbag.tier = 0;
                    itbag.islock = true;
                    itbag.time_use = 0;
                    conn.p.item.add_item_bag3(itbag);
                } else {
                    Service.send_notice_nobox_white(conn, "Đã có trong hành trang!");
                }
            }
            case 245 -> {
                Message m = new Message(23);
                m.writer().writeUTF("Túi Hành Trang");
                m.writer().writeByte(3);
                m.writer().writeShort(0);
                conn.addmsg(m);
                m.cleanup();
            }
            case 253 -> {
                if (conn.p.item.total_item_by_id(4, id_potion) > 0) {
                    conn.p.item.remove(4, id_potion, 1);
                    conn.p.item.add_item_bag47((short) 252, (short) 10, (byte) 4);
                    conn.p.item.charInventory(4);
                }
            }
            case 303 -> {
                // đèn hoa đăng
                Service.sendBoxInputText(conn, 28, "Lepaskan lentera", new String[]{"Nama teman yang ikut melepas"});
            }
            case 305 -> {
                // bó sen hồng
                if (conn.p.item.get_bag_able() < 3) {
                    Service.send_notice_box(conn, "Setidaknya tersedia 3 slot kosong di tasmu");
                    return;
                }
                if (conn.p.item.total_item_by_id(4, id_potion) > 0) {

                    conn.p.item.remove(4, id_potion, 1);
                    List<box_item_template> ids = new ArrayList<>();
                    List<Integer> it7 = new ArrayList<>(java.util.Arrays.asList(12, 13, 11));
                    List<Integer> it7_vip = new ArrayList<>(java.util.Arrays.asList(14, 471, 346, 33));
                    List<Integer> it4 = new ArrayList<>(java.util.Arrays.asList(251, 294, 275, 52, 18));
                    List<Integer> it4_vip = new ArrayList<>(java.util.Arrays.asList(206, 147));
                    for (int i = 0; i < Util.random(1, 2); i++) {
                        int ran = Util.random(100);
                        if (ran < 2) {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(2, 5);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 12) {// nlmd vang tim
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

                    Service.Show_open_box_notice_item(conn.p, "Kamu mendapatkan", ids);

                }
            }
            case 307 -> {
                // bó sen trắng
                // try{
                if (conn.p.item.get_bag_able() < 3) {
                    Service.send_notice_box(conn, "Cần 3 ô trống trong hành trang!");
                    return;
                }
                if (conn.p.item.total_item_by_id(4, id_potion) > 0) {
                    conn.p.item.remove(4, id_potion, 1);
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
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 6) { // nltt
                            short id = (short) Util.random(417, 464);
                            short quant = (short) Util.random(2);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 16) {
                            short id = Util.random(it4_vip, new ArrayList<>()).shortValue();
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else if (ran < 30) {
                            short id = Util.random(it7_vip, new ArrayList<>()).shortValue();
                            short quant = (short) 1;
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else if (ran < 45) {
                            short id = Util.random(it4, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 3);
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        } else if (ran < 70) {
                            short id = Util.random(it7, new ArrayList<>()).shortValue();
                            short quant = (short) Util.random(1, 3);
                            ids.add(new box_item_template(id, quant, (byte) 7));
                            conn.p.item.add_item_bag47(id, quant, (byte) 7);
                        } else {
                            short id = (short) Util.random(new int[]{2, 5});
                            short quant = (short) Util.random(100, 300);
                            ids.add(new box_item_template(id, quant, (byte) 4));
                            conn.p.item.add_item_bag47(id, quant, (byte) 4);
                        }
                    }
                    // conn.p.item.char_inventory(3);
                    // conn.p.item.char_inventory(4);
                    //
                    // conn.p.item.char_inventory(7);
                    Service.Show_open_box_notice_item(conn.p, "Bạn nhận được", ids);
                }
                // }catch(Exception e){e.printStackTrace();}
            }

            case 331 -> {
                InputDialog dialog = InputDialog
                        .builder()
                        .title("Kartu Change Name")
                        .fields(List.of("Nama Baru"))
                        .action((player, args) -> {
                            try {
                                String newName = args[0].trim();

                                if (newName.contains(" ")) {
                                    player.sendNoticeBox("Nama tidak boleh mengandung spasi!");
                                    return;
                                }
                                if (!newName.matches("^[A-Za-z0-9]+$")) {
                                    player.sendNoticeBox("Nama hanya boleh huruf dan angka, tanpa simbol!");
                                    return;
                                }
                                if (newName.length() < 6) {
                                    player.sendNoticeBox("Nama minimal 6 karakter!");
                                    return;
                                }
                                if (newName.length() > 14) {
                                    player.sendNoticeBox("Nama maksimal 14 karakter!");
                                    return;
                                }
                                if (SQLHelper.selectFrom("player").where("name", newName).exists()) {
                                    player.sendNoticeBox("Nama sudah digunakan oleh pemain lain!");
                                    return;
                                }

                                String oldName = player.name;

                                Dialog confirm = Dialog
                                        .builder()
                                        .id((byte) -4)
                                        .text(String.format("Ganti nama dari \"%s\" menjadi \"%s\"?\nPerubahan berlaku di semua data.", oldName, newName))
                                        .args(java.util.Map.of("newName", newName, "oldName", oldName))
                                        .onRespond((p, yOrN, argsx) -> {
                                            try {
                                                if (!yOrN) return;
                                                String nName = (String) argsx.get("newName");
                                                String oName = (String) argsx.get("oldName");

                                                if (SQLHelper.selectFrom("player").where("name", nName).exists()) {
                                                    p.sendNoticeBox("Nama sudah digunakan, silakan pilih nama lain!");
                                                    return;
                                                }

                                                SQLHelper.update("player").where("id", p.objectId).set("name", nName).execute();

                                                // update account.char
                                                // FIX: SQL.java set autoCommit(false) untuk SELURUH
                                                // connection pool. Tanpa commit() eksplisit di sini,
                                                // UPDATE ini di-ROLLBACK otomatis oleh HikariCP begitu
                                                // koneksi ditutup -- link akun<->karakter di tabel
                                                // `account` diam-diam TIDAK PERNAH ke-update walau
                                                // ganti nama di tabel `player` berhasil.
                                                try (java.sql.Connection dbConn = core.SQL.gI().getConnection();
                                                     java.sql.PreparedStatement ps = dbConn.prepareStatement(
                                                        "UPDATE `account` SET `char` = REPLACE(`char`, ?, ?) WHERE `char` LIKE ?")) {
                                                    ps.setString(1, "\"" + oName + "\"");
                                                    ps.setString(2, "\"" + nName + "\"");
                                                    ps.setString(3, "%\"" + oName + "\"%");
                                                    ps.executeUpdate();
                                                    if (!dbConn.getAutoCommit()) {
                                                        dbConn.commit();
                                                    }
                                                } catch (Exception e) { log.warn("Gagal update account.char: {}", e.getMessage()); }

                                                // update clan.mems
                                                // FIX: sama seperti di atas -- commit() yang hilang
                                                // bikin daftar member guild di kolom `mems` tidak
                                                // pernah ikut ke-update namanya setelah ganti nama.
                                                try (java.sql.Connection dbConn = core.SQL.gI().getConnection();
                                                     java.sql.PreparedStatement ps = dbConn.prepareStatement(
                                                        "UPDATE `clan` SET `mems` = REPLACE(`mems`, ?, ?) WHERE `mems` LIKE ?")) {
                                                    ps.setString(1, "\"" + oName + "\"");
                                                    ps.setString(2, "\"" + nName + "\"");
                                                    ps.setString(3, "%\"" + oName + "\"%");
                                                    ps.executeUpdate();
                                                    if (!dbConn.getAutoCommit()) {
                                                        dbConn.commit();
                                                    }
                                                } catch (Exception e) { log.warn("Gagal update clan.mems: {}", e.getMessage()); }

                                                if (p.myclan != null) {
                                                    for (template.ClanMember mem : p.myclan.members) {
                                                        if (mem.name.equals(oName)) { mem.name = nName; break; }
                                                    }
                                                }

                                                // update wedding
                                                try (java.sql.Connection dbConn = core.SQL.gI().getConnection();
                                                    java.sql.PreparedStatement ps = dbConn.prepareStatement(
                                                        "UPDATE `wedding` SET `name` = REPLACE(`name`, ?, ?) WHERE `name` LIKE ?")) {
                                                    ps.setString(1, "\"" + oName + "\"");
                                                    ps.setString(2, "\"" + nName + "\"");
                                                    ps.setString(3, "%\"" + oName + "\"%");
                                                    int rows = ps.executeUpdate();
                                                    dbConn.commit(); // ← tambah ini
                                                    log.info("Wedding update rows affected: {}", rows); // ← cek berapa row kena
                                                } catch (Exception e) {
                                                    log.warn("Gagal update wedding.name: {}", e.getMessage());
                                                }

                                                event_daily.Wedding wed = event_daily.Wedding.get_obj(oName);
                                                if (wed != null) {
                                                    if (wed.name_1.equals(oName)) wed.name_1 = nName;
                                                    if (wed.name_2.equals(oName)) wed.name_2 = nName;
                                                }

                                                p.name = nName;
                                                p.item.remove(4, id_potion, 1);
                                                p.item.charInventory(4);
                                                Service.sendMainCharInfo(p);
                                                MapService.broadcastMainCharInfo(p.map, p);
                                                p.sendNoticeBox("Nama berhasil diganti menjadi: " + nName);

                                            } catch (Exception e) {
                                                log.error("Error Kartu Change Name: {}", e.getMessage(), e);
                                                p.sendNoticeBox("Terjadi kesalahan saat mengganti nama.");
                                            }
                                        })
                                        .build();
                                player.openDialog(confirm);

                            } catch (Exception e) {
                                log.error("Error Kartu Change Name input: {}", e.getMessage(), e);
                                player.sendNoticeBox("Terjadi kesalahan.");
                            }
                        })
                        .build();
                conn.p.openInput(dialog);
            }
            default -> {
                // Service.send_notice_nobox_white(conn, "4Chưa có chức năng này");

            }
        }
        // ruong tim
    }

    private static void use_item_mount(Session conn, short id) throws IOException {
        switch (id) {
            case 62:
            case 63:
            case 64:
            case 65:
            case 66: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) (id - 62));
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 70:
            case 71:
            case 72:
            case 73:
            case 74: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 8);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 124: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 5);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 125: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 146: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 6);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 159: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 7);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 160: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 8);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 161: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 9);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 163: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 10);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 222: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 11);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 223: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 246: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 12);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 247: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 248: {
                conn.p.mount = new Mount((short) -1, (byte) 12);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 250: {
                conn.p.mount = new Mount((short) -1, (byte) 13);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 251: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 69, (byte) 20);
                conn.p.map.sendUseMount(conn.p);

                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 268: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 20, (byte) 69);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 271: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 107, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 272: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }

            case 275: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 111, (byte) 17);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 276: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 294: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 116, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 295: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 281: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 115, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 282: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 279: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 114, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }

            case 317: {
                conn.p.mount = new Mount((short) 115, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 280: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 299: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 117, (byte) 22);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }

            case 300: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 323: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 145, (byte) 22);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }

            case 324: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 269: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 106, (byte) 15);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 296: {
                conn.p.mount = new Mount((short) 106, (byte) 15);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 270: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 301: {
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) 121, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                break;
            }
            case 302: {
                if (conn.p.item.get_bag_able() < 1) {
                    Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                    return;
                }
                conn.p.item.remove(4, id, 1);
                Item47 itbag = new Item47();
                itbag.id = (short) (id - 1);
                itbag.quantity = 99;
                conn.p.item.add_item_bag47(4, itbag);
                break;
            }
            case 313: {
                conn.p.mount = new Mount((short) 117, (byte) 22);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 314: {
                conn.p.mount = new Mount((short) 121, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 315: {
                conn.p.mount = new Mount((short) 116, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 316: {
                conn.p.mount = new Mount((short) 114, (byte) 20);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 325: { // Mount Guild baru - visual sama seperti item 161
                conn.p.item.remove(4, id, 1);
                conn.p.mount = new Mount((short) -1, (byte) 9);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            case 318: {
                conn.p.mount = new Mount((short) 145, (byte) 22);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 319: {
                conn.p.mount = new Mount((short) 207, (byte) 12);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);

                break;
            }
            case 320: {
                conn.p.mount = new Mount((short) 172, (byte) 22);
                conn.p.map.sendUseMount(conn.p);
                MapService.broadcastMainCharInfo(conn.p.map, conn.p);
                Service.sendMainCharInfo(conn.p);
                break;
            }
            
            default: {
                // System.out.println("mount id " + id);
                break;
            }
        }


    }


    public static void ProcessItem3(Session conn, Message m2) throws IOException {

        byte id = m2.reader().readByte();
        byte index = m2.reader().readByte();
        if (id < 0 || id > (conn.p.maxbag - 1)) {
            return;
        }
        //
        Item3 temp3 = conn.p.item.bag3[id];
        if (temp3 != null) {
            DisabledItem disabled = ItemManager.getInstance().getDisabled(id, 3);
            if (disabled != null) {
                conn.p.sendNoticeBox(disabled.getReason());
                return;
            }

            if (temp3.clazz != 4 && temp3.clazz != conn.p.clazz) {
                Service.send_notice_nobox_white(conn, "Class tidak valid");
                return;
            }
            if (temp3.level > conn.p.level) {
                Service.send_notice_nobox_white(conn, "Level belum cukup");
                return;
            }
            if (temp3.type == 14) {
                Service.send_notice_nobox_white(conn, "Bawa ke kebun binatang untuk ditetaskan, tidak bisa dipakai begitu!");

                return;
            }
            if (temp3.time_use > 0) {
                long time_ = temp3.time_use - System.currentTimeMillis();
                time_ /= 60_000;
                Service.send_notice_nobox_white(conn, "Gunakan setelah " + ((time_ > 0) ? time_ : 1) + " menit lagi");
                return;
            }
            if (temp3.islock) {
                switch (temp3.type) {
                    case 0: // coat
                    case 1: // pant
                    case 2: // crown
                    case 3: // grove
                    case 4: // ring
                    case 5: // chain
                    case 6: // shoes
                    case 7: // wing
                    case 12: // amulet (wear[22])
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
                    case 27:
                    case 11:
                    case 28: // weapon
                    case 29: {
                        conn.p.playerWear(temp3, id, index);
                        break;
                    }
                    default: {
                        Service.send_notice_nobox_white(conn, "Tekan 2 kali baru bisa digunakan");
                        break;
                    }
                }
            } else {
                conn.p.id_buffer_126 = id;
                conn.p.id_index_126 = index;
                Service.send_box_input_yesno(conn, 126, "Menggunakan item ini akan mengunci, harap konfirmasi!");
            }
        }
    }

    public static void ProcessItem7(Session conn, Message m2) throws IOException {
        short id = m2.reader().readShort();
        DisabledItem disabled = ItemManager.getInstance().getDisabled(id, 7);
        if (disabled != null) {
            conn.p.sendNoticeBox(disabled.getReason());
            return;
        }

        if (conn.p.item.total_item_by_id(7, id) > 0) {
            if (id >= 352 && id <= 381) {
                conn.p.id_ngoc_tinh_luyen = id;
                Service.sendBoxInputText(conn, 16, "Masukkan jumlah", new String[]{"Masukkan jumlah"});

            } else {
                switch (id) {
                    case 5:
                    case 6:
                    case 7: {
                        if (conn.p.item.get_bag_able() < 1) {
                            Service.send_notice_nobox_white(conn, "Hành trang đầy!");
                            return;
                        }
                        Item47 it = new Item47();
                        it.id = (short) (id - 4);
                        it.quantity = ItemTemplate7.item.get(id).getValue();
                        conn.p.item.add_item_bag47(7, it);
                        conn.p.item.remove(7, id, 1);
                        break;
                    }
                    default: {
                        Service.send_notice_nobox_white(conn, "7Chưa có chức năng này");
                        break;
                    }
                }
            }
            // conn.p.item.char_inventory(7);
            conn.p.item.charInventory(4);
            conn.p.item.charInventory(7);
            conn.p.item.charInventory(3);
        }
    }

    static void openBox(Session s, int boxId) throws IOException {

        Box box = ItemBoxManager.gI().getBoxById(boxId);
        if (box == null) {
            s.p.sendNoticeBox("Item ini tidak aktif");
            return;
        }

        // Cek apakah inventory penuh sebelum membuka box
        if (s.p.item.get_bag_able() < 1) {
            s.p.sendNoticeBox("Inventory penuh! Harap kosongkan slot terlebih dahulu sebelum membuka box.");
            return;
        }

        Reward reward = ItemBoxManager.gI().rollSingle(box);


        if (reward != null) {


            List<Short> items = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();
            List<Short> categories = new ArrayList<>();

            if (reward.hasEquip()) {
                reward.equipments.forEach(item -> {

                    if (item.expiry_date == 0) {
                        item.islock = false;
                    }
                    s.p.item.add_item_bag3(item);
                    items.add(item.id);
                    quantities.add(1);
                    categories.add((short) 3);

                });

            }

            if (reward.hasPotion()) {
                reward.potions.forEach(item -> {
                    if (item.id == -1) {
                        s.p.updateGold(item.quantity);
                    } else {
                        s.p.item.add_item_bag47(item.category, item);
                    }
                    items.add(item.id);
                    quantities.add((int) item.quantity);
                    categories.add((short) item.category);
                });
            }

            if (reward.hasMaterials()) {
                reward.materials.forEach(item -> {
                    s.p.item.add_item_bag47(item.category, item);
                    items.add(item.id);
                    quantities.add((int) item.quantity);
                    categories.add((short) item.category);
                });
            }

            short[] ar_id = new short[items.size()];
            int[] ar_quant = new int[quantities.size()];
            short[] ar_type = new short[categories.size()];
            for (int i = 0; i < ar_id.length; i++) {
                ar_id[i] = items.get(i);
                ar_quant[i] = quantities.get(i);
                ar_type[i] = categories.get(i);
            }

            Service.Show_open_box_notice_item(s.p, "Kamu mendapatkan", ar_id, ar_quant, ar_type);

        } else {
            s.p.sendNoticeBox("Box Kosong");
        }


    }
}