package game.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.map.MapDropConfig;

import client.Player;
import core.Util;
import game.event.GameEvent;
import game.event.GameEventManager;
import game.event.natal.ChristmasEvent;
import client.io.Message;
import game.items.ExchangeService;
import game.items.models.ExchangeItem;
import game.items.models.RequiredItem;
import model.event.GlobalEvent;
import template.EffTemplate;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.MainObject;
import template.Medal_Material;
import template.Option;

public class LeaveItemMap {

    public static List<Short> item0x = new ArrayList<>();
    public static List<Short> item1x = new ArrayList<>();
    public static List<Short> item2x = new ArrayList<>();
    public static List<Short> item3x = new ArrayList<>();
    public static List<Short> item4x = new ArrayList<>();
    public static List<Short> item5x = new ArrayList<>();
    public static List<Short> item6x = new ArrayList<>();
    public static List<Short> item7x = new ArrayList<>();
    public static List<Short> item8x = new ArrayList<>();
    public static List<Short> item9x = new ArrayList<>();
    public static List<Short> item10x = new ArrayList<>();
    public static List<Short> item11x = new ArrayList<>();
    public static List<Short> item12x = new ArrayList<>();
    public static List<Short> item13x = new ArrayList<>();
    public static List<Short> item14x = new ArrayList<>();
    public static List<Short> item15x = new ArrayList<>();

    // Drop konfigurasi per-map dari database (map_drop_config)
    public static Map<Byte, List<MapDropConfig>> mapDropConfig = new HashMap<>();

    /**
     * Drop item berdasarkan konfigurasi tabel map_drop_config di database.
     * Digunakan khusus untuk map premium 111-114 (dan map lain yang dikonfigurasi).
     * Setiap baris di map_drop_config = satu item dengan peluang drop independen.
     */
    public static void leaveItemByMapConfig(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob == null || gameMap == null) return;
        List<MapDropConfig> configs = mapDropConfig.get(gameMap.mapId);
        if (configs == null || configs.isEmpty()) return;

        for (MapDropConfig cfg : configs) {
            if (Util.random(0, 300) < cfg.getDropChance()) {
                int qty = (cfg.getMinQuantity() >= cfg.getMaxQuantity())
                        ? cfg.getMinQuantity()
                        : Util.random(cfg.getMinQuantity(), cfg.getMaxQuantity() + 1);
                switch (cfg.getItemType()) {
                    case 3: // equipment — cari index berdasarkan db_id
                        int indexEquip = -1;
                        for (int i = 0; i < ItemTemplate3.item.size(); i++) {
                            if (ItemTemplate3.item.get(i).getId() == cfg.getItemId()) {
                                indexEquip = i;
                                break;
                            }
                        }
                        if (indexEquip == -1) {
                            break;
                        }
                        String equipName = ItemTemplate3.item.get(indexEquip).getName();
                        leave_item_by_type3(gameMap, indexEquip, cfg.getColor(),
                                p, equipName, mob.objectId);
                        break;
                    case 4: // potion / consumable — cari index berdasarkan db_id
                        short indexItem4 = -1;
                        for (short i = 0; i < ItemTemplate4.item.size(); i++) {
                            if (ItemTemplate4.item.get(i).getId() == cfg.getItemId()) {
                                indexItem4 = i;
                                break;
                            }
                        }
                        if (indexItem4 == -1) {
                            break;
                        }
                        for (int i = 0; i < qty; i++)
                            leave_item_by_type4(gameMap, indexItem4, p, mob.objectId);
                        break;
                    case 7: // material / craft — cari index berdasarkan db_id
                        short indexItem7 = -1;
                        for (short i = 0; i < ItemTemplate7.item.size(); i++) {
                            if (ItemTemplate7.item.get(i).getId() == cfg.getItemId()) {
                                indexItem7 = i;
                                break;
                            }
                        }
                        if (indexItem7 == -1) {
                            break;
                        }
                        for (int i = 0; i < qty; i++)
                            leave_item_by_type7(gameMap, indexItem7, p, mob.objectId);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static void leave_gold(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob != null) {
            int index_item_map = gameMap.get_item_map_index_able();
            if (index_item_map > -1) {
                //
                gameMap.itemDrop[index_item_map] = new ItemMap();
                gameMap.itemDrop[index_item_map].id_item = -1;
                gameMap.itemDrop[index_item_map].color = 0;
                int gold_drop = Util.random(mob.level * 100, mob.level * 300);
                EffTemplate ef = p.getEffectDefault(52);
                if (ef != null) {
                    gold_drop += (gold_drop * (ef.param / 100)) / 100;
                }
                gameMap.itemDrop[index_item_map].quantity = gold_drop;
                gameMap.itemDrop[index_item_map].category = 4;
                gameMap.itemDrop[index_item_map].idmaster = (short) p.objectId;
                gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
                gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
                String name = "gold *" + gameMap.itemDrop[index_item_map].quantity;
                // add in4 game scr
                Message mi = new Message(19);
                mi.writer().writeByte(4);
                mi.writer().writeShort(mob.objectId); // index mob die
                mi.writer().writeShort(0); // id icon (0 : vang)
                mi.writer().writeShort(index_item_map); //
                mi.writer().writeUTF(name);
                mi.writer().writeByte(0); // color
                mi.writer().writeShort(p.objectId); // id player
                MapService.sendMsgPlayerInside(gameMap, p, mi, true);
                mi.cleanup();
            }
        }
    }

    public static void leave_gold(GameMap gameMap, MainObject mob, Player p, int gold_drop) throws IOException {
        if (mob != null) {
            int index_item_map = gameMap.get_item_map_index_able();
            if (index_item_map > -1) {
                //
                gameMap.itemDrop[index_item_map] = new ItemMap();
                gameMap.itemDrop[index_item_map].id_item = -1;
                gameMap.itemDrop[index_item_map].color = 0;
                EffTemplate ef = p.getEffectDefault(52);
                if (ef != null) {
                    gold_drop += (gold_drop * (ef.param / 100)) / 100;
                }
                gameMap.itemDrop[index_item_map].quantity = gold_drop;
                gameMap.itemDrop[index_item_map].category = 4;
                gameMap.itemDrop[index_item_map].idmaster = (short) p.objectId;
                gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
                gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
                String name = "emas *" + gameMap.itemDrop[index_item_map].quantity;
                // add in4 game scr
                Message mi = new Message(19);
                mi.writer().writeByte(4);
                mi.writer().writeShort(mob.objectId); // index mob die
                mi.writer().writeShort(0); // id icon (0 : vang)
                mi.writer().writeShort(index_item_map); //
                mi.writer().writeUTF(name);
                mi.writer().writeByte(0); // color
                mi.writer().writeShort(p.objectId); // id player
                MapService.sendMsgPlayerInside(gameMap, p, mi, true);
                mi.cleanup();
            }
        }
    }

    public static void leave_item_3(GameMap gameMap, MobInMap mob, Player p) throws IOException {

        if (!p.isDropEquipment) {
            return;
        }

        if (mob != null) {
            short id_item_can_drop = 0;
            byte color_ = 0;
            if (60 > Util.random(0, 200)) {
                color_ = 1;
            } else if (25 > Util.random(0, 275)) {
                color_ = 2;
            } else if (15 > Util.random(0, 350)) {
                color_ = 3;
            } else if (5 > Util.random(0, 500)) {
                color_ = 4;
            }
            if (mob.color_name != 0) {
                color_ = 3;
            }
            if (mob.level >= 1 && mob.level < 10 && !item0x.isEmpty()) {
                id_item_can_drop = item0x.get(Util.random(0, item0x.size() - 1));
            } else if (mob.level >= 10 && mob.level < 20 && !item1x.isEmpty()) {
                id_item_can_drop = item1x.get(Util.random(0, item1x.size() - 1));
            } else if (mob.level >= 20 && mob.level < 30 && !item2x.isEmpty()) {
                id_item_can_drop = item2x.get(Util.random(0, item2x.size() - 1));
            } else if (mob.level >= 30 && mob.level < 40 && !item3x.isEmpty()) {
                id_item_can_drop = item3x.get(Util.random(0, item3x.size() - 1));
            } else if (mob.level >= 40 && mob.level < 50 && !item4x.isEmpty()) {
                id_item_can_drop = item4x.get(Util.random(0, item4x.size() - 1));
            } else if (mob.level >= 50 && mob.level < 60 && !item5x.isEmpty()) {
                id_item_can_drop = item5x.get(Util.random(0, item5x.size() - 1));
            } else if (mob.level >= 60 && mob.level < 70 && !item6x.isEmpty()) {
                id_item_can_drop = item6x.get(Util.random(0, item6x.size() - 1));
            } else if (mob.level >= 70 && mob.level < 80 && !item7x.isEmpty()) {
                id_item_can_drop = item7x.get(Util.random(0, item7x.size() - 1));
            } else if (mob.level >= 80 && mob.level < 90 && !item8x.isEmpty()) {
                id_item_can_drop = item8x.get(Util.random(0, item8x.size() - 1));
            } else if (mob.level >= 90 && mob.level < 100 && !item9x.isEmpty()) {
                id_item_can_drop = item9x.get(Util.random(0, item9x.size() - 1));
            } else if (mob.level >= 100 && mob.level < 110 && !item10x.isEmpty()) {
                id_item_can_drop = item10x.get(Util.random(0, item10x.size() - 1));
            } else if (mob.level >= 110 && mob.level < 120 && !item11x.isEmpty()) {
                id_item_can_drop = item11x.get(Util.random(0, item11x.size() - 1));
            } else if (mob.level >= 120 && mob.level < 130 && !item12x.isEmpty()) {
                id_item_can_drop = item12x.get(Util.random(0, item12x.size() - 1));
            } else if (mob.level >= 130 && !item13x.isEmpty()) {
                id_item_can_drop = item13x.get(Util.random(0, item13x.size() - 1));
            }
            // Fix: Check if valid item was found
            if (id_item_can_drop == 0) {
                return; // No valid item to drop
            }
            String name = ItemTemplate3.item.get(id_item_can_drop).getName();
            short index_real = 0;
            if (id_item_can_drop < 20) {
                for (int i = 0; i < 20; i++) {
                    if (ItemTemplate3.item.get(i).getName().equals(name)
                            && ItemTemplate3.item.get(i).getColor() == color_) {
                        index_real = (short) i;
                        break;
                    }
                }
            } else {
                for (int i = id_item_can_drop - 5; i < id_item_can_drop + 5; i++) {
                    if (ItemTemplate3.item.get(i).getName().equals(name)
                            && ItemTemplate3.item.get(i).getColor() == color_) {
                        index_real = (short) i;
                        break;
                    }
                }
            }
            //
            leave_item_by_type3(gameMap, index_real, color_, p, name, mob.objectId);
        }
    }

    public static void leave_item_by_type3(GameMap gameMap, int index_real, int color_, Player p_master, String name, int index)
            throws IOException {
        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            // Build fully-populated ItemMap locally before assigning to avoid race with expiry cleanup
            ItemMap drop = new ItemMap();
            drop.id_item = (short) index_real;
            drop.color = (byte) color_;
            drop.quantity = 1;
            drop.category = 3;
            drop.idmaster = (short) p_master.objectId;
            List<Option> opnew = new ArrayList<Option>();
            for (Option op_old : ItemTemplate3.item.get(index_real).getOp()) {
                Option temp = new Option(1, 1, (short) 0);
                temp.id = op_old.id;
                if (temp.id != 37 && temp.id != 38) {
                    if (op_old.getParam(0) < 10) {
                        temp.setParam(Util.random(0, 10));
                    } else {
                        temp.setParam(Util.random((9 * op_old.getParam(0)) / 10, op_old.getParam(0)));
                    }
                } else {
                    temp.setParam(1);
                }
                opnew.add(temp);
            }
            drop.op = new ArrayList<>();
            drop.op.addAll(opnew);
            drop.time_exist = System.currentTimeMillis() + 60_000L;
            drop.time_pick = System.currentTimeMillis() + 1_500L;
            gameMap.itemDrop[index_item_map] = drop;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(3);
            mi.writer().writeShort(index); // index mob die
            mi.writer().writeShort(ItemTemplate3.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(name);
            mi.writer().writeByte(color_); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leave_item_4(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob != null) {
            short index_real = (short) Util.random(0, 5);
            //
            leave_item_by_type4(gameMap, index_real, p, mob.objectId);
        }
    }

    public static void leave_item_by_type4(GameMap gameMap, short id_item, Player p_master, int index_mob) throws IOException {
        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = id_item;
            gameMap.itemDrop[index_item_map].color = 0;
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 4;
            gameMap.itemDrop[index_item_map].idmaster = (short) p_master.objectId;
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(4);
            mi.writer().writeShort(index_mob); // id mob die
            mi.writer().writeShort(ItemTemplate4.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(ItemTemplate4.item.get(gameMap.itemDrop[index_item_map].id_item).getName());
            mi.writer().writeByte(0); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leaveItemUpgrade(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob != null) {
            short index_real = (short) Util.random(0, 4);
            //
            leave_item_by_type7(gameMap, index_real, p, mob.objectId);
        }
    }

    private static void leave_item_by_type7(GameMap gameMap, short id_it, Player p_master, int indexmob) throws IOException {

        // Drop item7 id 46-145 dinonaktifkan permanen (tidak tergantung setting player)
        if (id_it >= 46 && id_it <= 145) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialMedal && id_it >= 46 && id_it <= 345) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialUpgrade && id_it >= 0 && id_it <= 3) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialWing && id_it >= 8 && id_it <= 11) {
            return;
        }


        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = id_it;
            if (ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getColor() == 21) {
                gameMap.itemDrop[index_item_map].color = 1;
            } else {
                gameMap.itemDrop[index_item_map].color = 0;
            }
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 7;
            gameMap.itemDrop[index_item_map].idmaster = (short) p_master.objectId;
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(7);
            mi.writer().writeShort(indexmob); // id mob die
            mi.writer().writeShort(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getName());
            mi.writer().writeByte(gameMap.itemDrop[index_item_map].color); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leave_item_boss(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        final int dropRate = 30; // 90%

        if (mob != null && mob.isBoss()) {
            // roi do boss co dinh
            short[] id_item_leave3 = new short[]{};
            short[] id_item_leave4 = new short[]{};
            short[] id_item_leave7 = new short[]{};
            // short id_medal_material = -1;
            short sizeRandomMedal = 0;
            switch (mob.template.mob_id) {
                case 101: { // xa nu
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 53};
                    id_item_leave7 = new short[]{0, 4, 14, 0, 4, 12, 2, 2, 1, 1, 10, 10};
                    sizeRandomMedal = (short) (30);

                    break;
                }
                case 84: { // de vang
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54};
                    id_item_leave7 = new short[]{8, 9, 10, 13, 14, 0, 4, 0, 4};
                    sizeRandomMedal = (short) (35);

                    break;
                }
                case 83: { // de bac
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 53};
                    id_item_leave7 = new short[]{0, 4, 0, 4, 11, 12, 14, 2, 3, 2, 3};
                    sizeRandomMedal = (short) (45);
                    break;
                }
                case 103: { // bo cap chua
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 48, 50, 18, 9, 48, 50, 18, 9, 2, 5, 2, 5, 2,
                            5, 2, 5};
                    id_item_leave7 = new short[]{0, 0, 0};
                    sizeRandomMedal = (short) (20);
                    break;
                }
                case 104: { // quy 1 mat
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 10, 10};
                    id_item_leave7 = new short[]{2, 3, 2, 3, 12, 12, 8, 9, 10, 8, 9, 10};
                    sizeRandomMedal = (short) (25);
                    break;
                }
                case 105: { // quy dau bo
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 49, 49};
                    id_item_leave7 = new short[]{11, 0, 4, 0, 4, 13, 14, 2, 3, 2, 3};
                    sizeRandomMedal = (short) (40);
                    break;
                }
                case 106: { // ky sy dia nguc
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};

                    sizeRandomMedal = (short) (50);
                    break;
                }
                case 149: { // nhen chua
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};

                    sizeRandomMedal = (short) (55);
                    break;
                }
                case 155: { // giant skeleton
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 173: { // tho tuyet
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 195: { // Godzila
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 196: { // King kong
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 197: { // ga trong
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 186: { // Người tuyết nhỏ
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 187: { // Lính rìu nhỏ
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }

                case 188, 219, 220, 221, 222: { // Lão trọc
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1};
                    id_item_leave7 = new short[]{14};

                    sizeRandomMedal = (short) (60);
                    break;
                }

            }
            for (short id : id_item_leave3) {
                ItemTemplate3 temp = ItemTemplate3.item.get(id);
                leave_item_by_type3(gameMap, id, temp.getColor(), p, temp.getName(), mob.objectId);
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave4) {
                    if (id == -1) {
                        leave_gold(gameMap, mob, p);
                    } else {
                        leave_item_by_type4(gameMap, id, p, mob.objectId, p.objectId);
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave7) {
                    leave_item_by_type7(gameMap, id, p, mob.objectId, p.objectId);
                }
            }
            for (int l = 0; l < sizeRandomMedal; l++) {
                leave_item_by_type7(gameMap, (short) Util.random(136, 146), p, mob.objectId, p.objectId);
            }
        }
    }

    public static void leaveCraftMaterial(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob != null) {
            short index_real = -1;
            switch (mob.level) {
                case 1:
                case 2:
                case 3:
                case 4: {
                    index_real = Medal_Material.m_white[0][Util.random(0, 3)];
                    break;
                }
                case 5: {
                    index_real = Medal_Material.m_white[0][Util.random(3, 6)];
                    break;
                }
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17: {
                    index_real = Medal_Material.m_white[0][Util.random(6, 9)];
                    break;
                }
                case 19: {
                    index_real = Medal_Material.m_white[1][Util.random(0, 3)];
                    break;
                }
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26: {
                    index_real = Medal_Material.m_white[1][Util.random(3, 6)];
                    break;
                }
                case 28:
                case 29: {
                    index_real = Medal_Material.m_white[1][Util.random(6, 9)];
                    break;
                }
                case 31:
                case 32:
                case 33:
                case 34:
                case 35: {
                    index_real = Medal_Material.m_white[1][9];
                    break;
                }
                case 37: {
                    index_real = Medal_Material.m_white[2][Util.random(0, 3)];
                    break;
                }
                case 39:
                case 40:
                case 41:
                case 42:
                case 43: {
                    index_real = Medal_Material.m_white[2][Util.random(3, 6)];
                    break;
                }
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54: {
                    index_real = Medal_Material.m_white[2][Util.random(6, 9)];
                    break;
                }
                case 55: {
                    index_real = Medal_Material.m_white[3][Util.random(0, 3)];
                    break;
                }
                case 57:
                case 58:
                case 59: {
                    index_real = Medal_Material.m_white[3][Util.random(3, 6)];
                    break;
                }
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                case 68:
                case 69: {
                    index_real = Medal_Material.m_white[3][Util.random(6, 9)];
                    break;
                }
                case 71:
                case 72:
                case 73:
                case 74: {
                    index_real = Medal_Material.m_white[4][Util.random(0, 3)];
                    break;
                }
                case 76:
                case 77: {
                    index_real = Medal_Material.m_white[4][Util.random(3, 6)];
                    break;
                }
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 83:
                case 84:
                case 85:
                case 86:
                case 87:
                case 88:
                case 89: {
                    index_real = Medal_Material.m_white[4][Util.random(6, 9)];
                    break;
                }
                case 90:
                case 91:
                case 92: {
                    index_real = Medal_Material.m_white[5][Util.random(0, 3)];
                    break;
                }
                case 93:
                case 94:
                case 95: {
                    index_real = Medal_Material.m_white[5][Util.random(3, 6)];
                    break;
                }
                case 96:
                case 97:
                case 98:
                case 99:
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                case 106:
                case 107: {
                    index_real = Medal_Material.m_white[5][Util.random(6, 9)];
                    break;
                }
                case 109:
                case 110: {
                    index_real = Medal_Material.m_white[6][Util.random(0, 3)];
                    break;
                }
                case 112:
                case 113: {
                    index_real = Medal_Material.m_white[6][Util.random(3, 6)];
                    break;
                }
                case 114:
                case 115:
                case 116:
                case 117:
                case 118:
                case 119:
                case 120:
                case 121:
                case 122:
                case 123: {
                    index_real = Medal_Material.m_white[6][Util.random(6, 9)];
                    break;
                }
                case 125:
                case 126:
                case 127: {
                    index_real = Medal_Material.m_white[0][Util.random(6, 9)];
                    break;
                }
            }
            //
            if (index_real > -1) {
                //
                leave_item_by_type7(gameMap, index_real, p, mob.objectId);
            }
            // if (25 > Util.random(0, 100)) {
            // index_real = Medal_Material.m_yellow[Util.random(0, 10)];
            // //
            // leave_item_by_type7(map, index_real, p, mob.index);
            // }
            if (25 > Util.random(0, 100)) {
                index_real = (short) ((15 > Util.random(0, 120)) ? 11
                        : ((35 > Util.random(0, 120)) ? 10 : ((50 > Util.random(0, 120)) ? 9 : 8)));
                //
                leave_item_by_type7(gameMap, index_real, p, mob.objectId);
            }
        }
    }

    public static void leave_item_event(GameMap gameMap, MobInMap mob, Player p) throws IOException {

        List<GlobalEvent> events = GameEventManager.gI().getAllEvents();

        List<Integer> dropItems = new ArrayList<>();
        for (GlobalEvent event : events) {
            for (RequiredItem requiredItem : event.getRequiredItems()) {
                dropItems.add(requiredItem.getItemId());
            }
        }
        
        if (!dropItems.isEmpty()) {
            int randomIndex = (int) (Math.random() * dropItems.size());
            int randomId = dropItems.get(randomIndex);

            leave_item_by_type4(gameMap, (short) randomId, p, mob.objectId);
        }
    }

    public static void leave_material_ngockham(GameMap gameMap, MobInMap mob, Player p) throws IOException {
        if (mob != null) {
            short index_real = -1;
            //
            if (25 > Util.random(120)) {
                switch (mob.template.mob_id) {
                    case 167: {
                        index_real = 362;
                        break;
                    }
                    case 168: {
                        index_real = 372;
                        break;
                    }
                    case 169: {
                        index_real = 367;
                        break;
                    }
                    case 170: {
                        index_real = 357;
                        break;
                    }
                    case 171: {
                        index_real = 377;
                        break;
                    }
                    case 172: {
                        index_real = 352;
                        break;
                    }
                }
            }
            if (index_real != -1) {
                leave_item_by_type7(gameMap, index_real, p, mob.objectId);
            }
        }
    }

    public static void leave_vang(GameMap gameMap, MobInMap mob, int id_mater) throws IOException {
        if (mob != null) {
            int index_item_map = gameMap.get_item_map_index_able();
            if (index_item_map > -1) {
                //
                gameMap.itemDrop[index_item_map] = new ItemMap();
                gameMap.itemDrop[index_item_map].id_item = -1;
                gameMap.itemDrop[index_item_map].color = 0;
                int vang_drop = Util.random(mob.level * 25, mob.level * 100);
                gameMap.itemDrop[index_item_map].quantity = vang_drop;
                gameMap.itemDrop[index_item_map].category = 4;
                gameMap.itemDrop[index_item_map].idmaster = (short) id_mater;
                gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
                gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
                String name = "emas *" + gameMap.itemDrop[index_item_map].quantity;
                // add in4 game scr
                Message mi = new Message(19);
                mi.writer().writeByte(4);
                mi.writer().writeShort(mob.objectId); // index mob die
                mi.writer().writeShort(0); // id icon (0 : vang)
                mi.writer().writeShort(index_item_map); //
                mi.writer().writeUTF(name);
                mi.writer().writeByte(0); // color
                mi.writer().writeShort(-1); // id player
                MapService.sendMsgPlayerInside(gameMap, mob, mi, true);
                mi.cleanup();
            }
        }
    }

    public static void leave_item_by_type3(GameMap gameMap, int index_real, int color_, Player p_master, String name, int index,
                                           int idP)
            throws IOException {
        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = (short) index_real;
            gameMap.itemDrop[index_item_map].color = (byte) color_;
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 3;
            gameMap.itemDrop[index_item_map].idmaster = (short) idP;
            List<Option> opnew = new ArrayList<>();
            for (Option op_old : ItemTemplate3.item.get(index_real).getOp()) {
                Option temp = new Option(1, 1, (short) 0);
                temp.id = op_old.id;
                if (temp.id != 37 && temp.id != 38) {
                    if (op_old.getParam(0) < 10) {
                        temp.setParam(Util.random(0, 10));
                    } else {
                        temp.setParam(Util.random((9 * op_old.getParam(0)) / 10, op_old.getParam(0)));
                    }
                } else {
                    temp.setParam(1);
                }
                opnew.add(temp);
            }
            gameMap.itemDrop[index_item_map].op = new ArrayList<>();
            gameMap.itemDrop[index_item_map].op.addAll(opnew);
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(3);
            mi.writer().writeShort(index); // index mob die
            mi.writer().writeShort(ItemTemplate3.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(name);
            mi.writer().writeByte(color_); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leave_item_by_type4(GameMap gameMap, short index_real, Player p_master, int index, int idp)
            throws IOException {
        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = index_real;
            gameMap.itemDrop[index_item_map].color = 0;
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 4;
            gameMap.itemDrop[index_item_map].idmaster = (short) idp;
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(4);
            mi.writer().writeShort(index); // id mob die
            mi.writer().writeShort(ItemTemplate4.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(ItemTemplate4.item.get(gameMap.itemDrop[index_item_map].id_item).getName());
            mi.writer().writeByte(0); // color
            mi.writer().writeShort(-1); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }

    public static void leave_item_by_type7(GameMap gameMap, short id_it, Player p_master, int index, int idp)
            throws IOException {
        // Drop item7 id 46-145 dinonaktifkan permanen (tidak tergantung setting player)
        if (id_it >= 46 && id_it <= 145) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialMedal && id_it >= 46 && id_it <= 345) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialUpgrade && id_it >= 0 && id_it <= 3) {
            return;
        }

        if (p_master != null && !p_master.isDropMaterialWing && id_it >= 8 && id_it <= 11) {
            return;
        }

        int index_item_map = gameMap.get_item_map_index_able();
        if (index_item_map > -1) {
            //
            gameMap.itemDrop[index_item_map] = new ItemMap();
            gameMap.itemDrop[index_item_map].id_item = id_it;
            if (ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getColor() == 21) {
                gameMap.itemDrop[index_item_map].color = 1;
            } else {
                gameMap.itemDrop[index_item_map].color = 0;
            }
            gameMap.itemDrop[index_item_map].quantity = 1;
            gameMap.itemDrop[index_item_map].category = 7;
            gameMap.itemDrop[index_item_map].idmaster = (short) idp;
            gameMap.itemDrop[index_item_map].time_exist = System.currentTimeMillis() + 60_000L;
            gameMap.itemDrop[index_item_map].time_pick = System.currentTimeMillis() + 1_500L;
            // add in4 game scr
            Message mi = new Message(19);
            mi.writer().writeByte(7);
            mi.writer().writeShort(index); // id mob die
            mi.writer().writeShort(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getIcon());
            mi.writer().writeShort(index_item_map); //
            mi.writer().writeUTF(ItemTemplate7.item.get(gameMap.itemDrop[index_item_map].id_item).getName());
            mi.writer().writeByte(gameMap.itemDrop[index_item_map].color); // color
            mi.writer().writeShort(p_master.objectId); // id player
            MapService.sendMsgPlayerInside(gameMap, p_master, mi, true);
            mi.cleanup();
        }
    }
}
