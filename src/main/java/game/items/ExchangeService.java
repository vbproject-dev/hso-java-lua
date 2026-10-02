package game.items;

import client.Player;
import core.SQL;
import core.Service;
import game.items.models.ExchangeItem;
import game.items.models.ItemReward;
import game.items.models.RequiredItem;
import lombok.extern.slf4j.Slf4j;
import model.event.GlobalEvent;
import model.item.Item;
import model.item.Reward;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.Item3;
import template.Item47;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class ExchangeService {
    private static ExchangeService instance;
    private final List<ExchangeItem> exchangeItemList = new ArrayList<>();

    public static ExchangeService gI() {
        if (instance == null) {
            instance = new ExchangeService();
        }
        return instance;
    }

    /**
     * FALLBACK GUESS ONLY — dipakai kalau RequiredItem TIDAK punya kategori
     * eksplisit (data lama, format DB 2-elemen [itemId, qty]).
     *
     * PERINGATAN: ID item TIDAK unik lintas tabel (item3/item4/item7 masing2
     * punya auto-increment sendiri), jadi ID yang sama bisa mewakili item
     * yang BEDA di tabel berbeda. Fungsi ini cek item7 dulu baru item4 — kalau
     * sebuah ID kebetulan juga terdaftar di item7 padahal item aslinya di
     * item4, hasil tebakannya SALAH (nyasar ke item7). Ini bukan bug baru,
     * ini keterbatasan bawaan dari menebak kategori lewat ID saja.
     *
     * FIX SEBENARNYA: simpan kategori eksplisit di DB, format required_items
     * jadi [[itemId, qty, category], ...] (persis seperti reward_item3/4/7
     * yang sudah dipisah per kolom). Lihat RequiredItem.hasExplicitCategory().
     * Fungsi ini hanya dipanggil kalau kategori eksplisit tidak ada.
     */
    public static byte resolveCategory(int itemId) {
        for (template.ItemTemplate7 t : template.ItemTemplate7.item) {
            if (t.getId() == itemId) return 7;
        }
        for (template.ItemTemplate4 t : template.ItemTemplate4.item) {
            if (t.getId() == itemId) return 4;
        }
        return 4; // fallback: perilaku lama
    }

    /**
     * Kategori final untuk sebuah RequiredItem: PAKAI nilai eksplisit dari DB
     * kalau ada (format 3-elemen), baru fallback ke tebakan ID kalau tidak ada.
     */
    public static byte resolveCategory(RequiredItem req) {
        if (req.hasExplicitCategory()) {
            return (byte) req.getCategory();
        }
        return resolveCategory(req.getItemId());
    }

    public boolean canExchange(Player player, List<RequiredItem> items) {
        for (RequiredItem req : items) {
            byte category = resolveCategory(req);
            int have = player.item.countItem((short) req.getItemId(), category);
            if (have < req.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public int getMaxExchangeCount(Player player, List<RequiredItem> items) {
        int max = Integer.MAX_VALUE;

        // Check material requirements
        for (RequiredItem req : items) {
            byte category = resolveCategory(req);
            int have = player.item.countItem((short) req.getItemId(), category);
            int possible = have / req.getQuantity();

            if (possible < max) {
                max = possible;
            }
        }

        // Check diamond limit
        int diamondPossible = player.getGem() / 100;

        if (diamondPossible < max) {
            max = diamondPossible;
        }

        return max;
    }

    /**
     * Sama seperti getMaxExchangeCount(), TAPI tanpa syarat permata.
     * CATATAN: saat ini tidak dipakai — semua jalur exchange (termasuk
     * global_event/npc -123 Nenek Merdeka) tetap kena biaya 100 permata/tukar
     * lewat getMaxExchangeCount(). Method ini dibiarkan tersedia sebagai utilitas
     * kalau suatu saat ada NPC/event lain yang memang butuh varian tanpa syarat
     * permata (mis. reward gratis murni bahan-only).
     */
    public int getMaxExchangeCountMaterialOnly(Player player, List<RequiredItem> items) {
        int max = Integer.MAX_VALUE;

        for (RequiredItem req : items) {
            byte category = resolveCategory(req);
            int have = player.item.countItem((short) req.getItemId(), category);
            int possible = have / req.getQuantity();

            if (possible < max) {
                max = possible;
            }
        }

        if (max == Integer.MAX_VALUE) {
            max = 0; // tidak ada requirement sama sekali -> jangan biarkan "unlimited"
        }

        return max;
    }

    /**
     * @deprecated pakai exchange(player, event, count) supaya player bisa
     * milih jumlah tukar sendiri. Overload ini dibiarkan (nukar sebanyak
     * maksimum) buat jaga-jaga kalau ada pemanggil lama.
     */
    @Deprecated
    public boolean exchange(Player player, GlobalEvent event) throws IOException {
        int max = getMaxExchangeCount(player, event.getRequiredItems());
        return exchange(player, event, max);
    }

    /**
     * Tukar sebanyak `count` kali (diinput player sendiri lewat InputDialog di
     * MenuManager.openGlobalMenu), bukan otomatis sebanyak maksimum.
     * Semua NPC/global_event (termasuk npc -123 Nenek Merdeka) kena biaya
     * 100 permata per tukar, sama seperti NPC exchange lainnya.
     */
    public boolean exchange(Player player, GlobalEvent event, int count) throws IOException {

        int max = getMaxExchangeCount(player, event.getRequiredItems());

        if (count <= 0) {
            player.sendNoticeBox("Jumlah tukar tidak valid");
            return false;
        }
        if (max <= 0) {
            player.sendNoticeBox("Bahan atau permata tidak mencukupi");
            return false;
        }
        if (count > max) {
            player.sendNoticeBox("Jumlah melebihi maksimum yang bisa ditukar (" + max + ")");
            return false;
        }

        int maxExchange = count;

        // Remove required materials
        for (RequiredItem req : event.getRequiredItems()) {
            int totalRemove = req.getQuantity() * maxExchange;
            byte category = resolveCategory(req);
            player.item.remove(category, req.getItemId(), totalRemove);
        }

        // Semua NPC/global_event kena biaya 100 permata/tukar.
        player.updateGem(-100L * maxExchange);

        var rewards = new ArrayList<Item>();
        for (Item item : event.getReward().getItems()) {

                rewards.add(new Item(
                        item.getItemId(),
                        item.getCategory(),
                        item.getQuantity(),
                        item.getDuration()
                ));

        }
        Service.sendReward(player, "Event Rewards", event.getReward(), maxExchange);
        return true;
    }


    public boolean exchange(Player player, ExchangeItem quest) throws IOException {

        int maxExchange = getMaxExchangeCount(player, quest.getRequirements());

        if (maxExchange <= 0) {
            player.sendNoticeBox("Bahan atau permata tidak mencukupi");
            return false;
        }

        // Remove required materials
        for (RequiredItem req : quest.getRequirements()) {
            int totalRemove = req.getQuantity() * maxExchange;
            byte category = resolveCategory(req);
            player.item.remove(category, req.getItemId(), totalRemove);
        }

        // Remove diamonds
        player.updateGem(-100L * maxExchange);

        // [ Give reward items ]
        List<Short> items = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Short> categories = new ArrayList<>();

        ItemReward reward = quest.getItemReward();
        if (!reward.equipments().isEmpty()) {
            for (Item3 equip : reward.equipments()) {
                player.item.add_item_bag3(equip);
                items.add(equip.id);
                quantities.add(maxExchange);
                categories.add((short) 3);
            }
        }

        if (!reward.potions().isEmpty()) {
            for (Item47 potion : reward.potions()) {
                player.item.add_item_bag47(potion.id, (short) (potion.quantity * maxExchange), (byte) 4);
                items.add(potion.id);
                quantities.add((int) potion.quantity * maxExchange);
                categories.add((short) 4);
            }
        }

        if (!reward.materials().isEmpty()) {
            for (Item47 material : reward.materials()) {
                player.item.add_item_bag47(material.id, (short) (material.quantity * maxExchange), (byte) 7);
                items.add(material.id);
                quantities.add((int) material.quantity * maxExchange);
                categories.add((short) 7);
            }
        }
        short[] ar_id = new short[items.size()];
        int[] ar_quant = new int[quantities.size()];
        short[] ar_type = new short[categories.size()];
        for (int i = 0; i < ar_id.length; i++) {
            ar_id[i] = items.get(i);
            ar_quant[i] = quantities.get(i);
            ar_type[i] = categories.get(i);
        }


        player.item.charInventory(7);
        player.item.charInventory(4);
        player.item.charInventory(3);
        Service.Show_open_box_notice_item(player, "Kamu Mendapatkan", ar_id, ar_quant, ar_type);

        return true;
    }


    /**
     * Cari nama item resmi buat ditampilkan di info NPC (mis. "Bahan yang
     * dibutuhkan"), tanpa pakai List.get(id) sebagai index (bisa salah
     * ambil/IndexOutOfBounds kalau id item lebih besar dari jumlah baris di
     * tabel item4/item7). Dicari lewat ID asli di template.
     *
     * INI CUMA FALLBACK TEBAKAN (item7 dulu, baru item4) — dipakai kalau
     * kategori item TIDAK diketahui. Kalau kamu punya RequiredItem (yang bisa
     * bawa kategori eksplisit dari DB), PAKAI resolveItemName(RequiredItem)
     * di bawah, JANGAN panggil overload ini langsung dengan getItemId(),
     * karena itu buang info kategorinya dan bisa nyasar lagi ke item7.
     */
    public static String resolveItemName(int itemId) {
        for (template.ItemTemplate7 t : template.ItemTemplate7.item) {
            if (t.getId() == itemId) return t.getName();
        }
        for (template.ItemTemplate4 t : template.ItemTemplate4.item) {
            if (t.getId() == itemId) return t.getName();
        }
        return "Item #" + itemId; // fallback aman, tidak crash NPC kalau id belum ada di template
    }

    /**
     * Versi yang BENAR untuk dipakai di menu/guide: pakai kategori eksplisit
     * dari RequiredItem kalau ada, jadi nama yang ditampilkan pasti nyambung
     * sama kategori yang dipakai saat cek & potong item (resolveCategory).
     * Tanpa ini, nama bisa "kebetulan ketemu" di item7 walau item aslinya
     * item4 (dan sebaliknya) — sama seperti bug resolveCategory yang lama.
     */
    public static String resolveItemName(RequiredItem req) {
        byte category = resolveCategory(req);
        int itemId = req.getItemId();
        if (category == 7) {
            for (template.ItemTemplate7 t : template.ItemTemplate7.item) {
                if (t.getId() == itemId) return t.getName();
            }
        } else if (category == 4) {
            for (template.ItemTemplate4 t : template.ItemTemplate4.item) {
                if (t.getId() == itemId) return t.getName();
            }
        }
        return resolveItemName(itemId); // fallback tebakan lama kalau tidak ketemu di kategori yang diharapkan
    }

    public ExchangeItem getItem(int npcId, String name) {
        for (ExchangeItem item : exchangeItemList) {
            if (item.getNpcId() == npcId && item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public void loadDatabase() {
        exchangeItemList.clear();

        // FIX RESOURCE LEAK: Connection dari pool HikariCP tidak pernah ditutup
        // sama sekali (cuma rs/ps yang di-close manual di akhir), dan karena
        // rs.close()/ps.close() ditulis manual (bukan try-with-resources), begitu
        // ada exception di tengah parsing JSON pada baris manapun sebelum kedua
        // close() itu, rs DAN ps ikut bocor juga selain conn. try-with-resources
        // menjamin ketiganya selalu ditutup, sukses maupun gagal.
        try (Connection conn = SQL.gI().getConnection();
             Statement ps = conn.createStatement();
             ResultSet rs = ps.executeQuery("SELECT * FROM item_exchange")) {

            while (rs.next()) {

                int id = rs.getInt("id");
                String name = rs.getString("name");
                int npcId = rs.getInt("npc_id");

                // Parse Required Items [[itemId, quantity], ...]
                JSONArray jsonArray = (JSONArray) JSONValue.parse(rs.getString("required_items"));
                if (jsonArray == null) {
                    continue;
                }

                List<RequiredItem> requiredItems = new ArrayList<>();
                for (Object o : jsonArray) {
                    JSONArray innerArray = (JSONArray) o; // each inner array
                    if (innerArray.size() >= 2) {
                        int itemId = Integer.parseInt(innerArray.get(0).toString());
                        int quantity = Integer.parseInt(innerArray.get(1).toString());
                        // Elemen ke-3 opsional = kategori eksplisit (3/4/7).
                        // Format lama [itemId, qty] tetap jalan (fallback tebak
                        // via resolveCategory), format baru [itemId, qty, category]
                        // dipakai langsung tanpa nebak -> tidak nyasar lagi.
                        if (innerArray.size() >= 3) {
                            int category = Integer.parseInt(innerArray.get(2).toString());
                            requiredItems.add(new RequiredItem(itemId, quantity, category));
                        } else {
                            requiredItems.add(new RequiredItem(itemId, quantity));
                        }
                    }
                }


                String item3Data = rs.getString("reward_item3");
                String item4Data = rs.getString("reward_item4");
                String item7Data = rs.getString("reward_item7");
                long exp = rs.getLong("reward_exp");
                long gold = rs.getLong("reward_gold");
                long gem = rs.getLong("reward_gem");
                ItemReward reward = ItemReward.fromJsonArray(item3Data, item4Data, item7Data, exp, gold, gem);

                exchangeItemList.add(new ExchangeItem(npcId, name, requiredItems, reward));

            }

        } catch (Exception e) {
            log.error("Error loading item_exchange data:");
            e.printStackTrace();
        }
    }

    public void clear(String name) {
        exchangeItemList.removeIf(exchangeItem -> exchangeItem.getName().equals(name));
    }
}