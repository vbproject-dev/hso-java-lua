package game.items;

import game.event.GameEventManager;
import game.guild.Guild;
import client.Player;
import core.Service;
import client.io.Session;
import game.items.box.ItemBoxManager;
import game.items.compensation.CompensationManager;
import game.items.models.DisabledItem;
import game.items.models.GuildItem;
import game.items.models.ItemReward;
import game.mount.Mount;
import game.shop.ShopManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.fashion.FashionData;
import model.mount.MountData;
import template.Item3;
import template.Item47;
import utils.SQLHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
public class ItemManager {
    private final CopyOnWriteArrayList<DisabledItem> disabledItems = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuildItem> guildItems = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<FashionData> fashionData = new CopyOnWriteArrayList<>();
    private final Map<Integer, MountData> mountData = new ConcurrentHashMap<>();

    private ItemManager() {
    }

    private static class Holder {
        private static final ItemManager INSTANCE = new ItemManager();
    }

    public static ItemManager getInstance() {
        return Holder.INSTANCE;
    }

    public void disableItem(int itemId, int category, String reason) {
        DisabledItem item = new DisabledItem(itemId, category, reason == null ? "Item ini sedang di nonaktifkan" : reason);
        // Check if already exists
        if (!disabledItems.contains(item)) {
            disabledItems.add(item);
            SQLHelper.insert("item_disable")
                    .value("item_id", item.getId())
                    .value("item_category", item.getCategory())
                    .value("reason", item.getReason())
                    .execute();
        }
    }


    public boolean enableItem(int itemId, int category) {
        return disabledItems.removeIf(item -> {
            boolean match = item.match(itemId, category);
            if (match) {
                SQLHelper.delete("item_disabled")
                        .where("item_id", itemId)
                        .where("item_category", category)
                        .execute();
            }
            return match;
        });
    }


    /* ===================== VALIDATION ===================== */

    /**
     * Validates if item can be used
     */
    public boolean isDisabled(int itemId, int category) {
        return getDisabled(itemId, category) != null;
    }

    public DisabledItem getDisabled(int itemId, int category) {
        for (DisabledItem item : disabledItems) {
            if (item.match(itemId, category)) {
                return item;
            }
        }
        return null;
    }

    public void loadAll() {
        ItemBoxManager.gI().loadDatabase();
        ExchangeService.gI().loadDatabase();
        CompensationManager.gI().loadDatabase();
        loadDisableItems();
        loadClanItem();
        loadFashionData();
        loadMountData();
        ShopManager.getInstance().load();
    }

    public Mount getMountData(int itemId) {
        MountData data;
        if ((data = mountData.get(itemId)) != null) {
            return new Mount((short) data.getPart(), (byte) data.getType());
        }
        return new Mount((short) -1, (byte) 4);
    }

    private void loadMountData() {
        mountData.clear();
        var listItems = SQLHelper.selectFrom("mount_data").getAsModel(MountData.class);
        listItems.forEach(mount -> mountData.put(mount.getId(), mount));
    }

    private void loadFashionData() {
        fashionData.clear();
        var listFashion = SQLHelper.selectFrom("fashion_data").getAsModel(FashionData.class);
        if (!listFashion.isEmpty()) {
            fashionData.addAll(listFashion);
        }
    }

    public void sendReward(Session s, ItemReward reward) {
        List<Short> items = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Short> categories = new ArrayList<>();

        for (Item3 item : reward.equipments()) {
            items.add(item.id);
            quantities.add(1);
            categories.add((short) 3);
            s.p.item.add_item_bag3(item);
        }
        for (Item47 item : reward.potions()) {
            items.add(item.id);
            quantities.add(1);
            categories.add((short) 4);
            s.p.item.add_item_bag47(4, item);
        }
        for (Item47 item : reward.materials()) {
            items.add(item.id);
            quantities.add(1);
            categories.add((short) 7);
            s.p.item.add_item_bag47(7, item);
        }

        if (reward.gold() > 0) {
            items.add((short) -1);
            quantities.add((int) reward.gold());
            categories.add((short) 4);
            s.p.updateGold(reward.gold());

        }

        if (reward.gem() > 0) {
            items.add((short) -2);
            quantities.add((int) reward.gem());
            categories.add((short) 4);
            s.p.updateGem(reward.gem());

        }

        short[] ar_id = new short[items.size()];
        int[] ar_quant = new int[quantities.size()];
        short[] ar_type = new short[categories.size()];
        for (int i = 0; i < ar_id.length; i++) {
            ar_id[i] = items.get(i);
            ar_quant[i] = quantities.get(i);
            ar_type[i] = categories.get(i);
        }

        try {
            s.p.item.charInventory(7);
            s.p.item.charInventory(4);
            s.p.item.charInventory(3);
            Service.Show_open_box_notice_item(s.p, "Kamu Mendapatkan", ar_id, ar_quant, ar_type);
        } catch (IOException ignore) {
        }
    }

    private void loadDisableItems() {
        List<DisabledItem> keys = SQLHelper.selectFrom("item_disable")
                .get(
                        rs -> new DisabledItem(
                                rs.getInt("item_id"),
                                rs.getInt("item_category"),
                                rs.getString("reason")
                        )
                );
        disabledItems.addAll(keys);

    }

    public boolean canUse(Player player, int itemId) {
        if (isGuildItem(itemId)) {
            if (player.myclan == null) return false;
            return guildItems.stream().anyMatch(guildItem -> guildItem.getItemId() == itemId && guildItem.getGuildName().equalsIgnoreCase(player.myclan.shortName));
        }

        return true;
    }

    public Item3 getGuildItem(int type, Guild guild) {
        for (GuildItem g : guildItems) {
            if (g.getGuildName().equalsIgnoreCase(guild.shortName)) {
                Item3 temp = Item3.fromTemplate((short) g.getItemId());
                if (temp != null && temp.type == type) {
                    return temp;
                }
            }
        }
        return null;
    }

    /**
     * Cari baris konfigurasi guild_item (dari DB) untuk guild + type tertentu.
     * Dipakai untuk mengambil stat (option_warrior/option_assassin/dst) yang
     * sudah diatur admin lewat database, bukan hasil random dari kode.
     */
    public GuildItem findGuildItemConfig(int type, Guild guild) {
        for (GuildItem g : guildItems) {
            if (g.getGuildName().equalsIgnoreCase(guild.shortName)) {
                Item3 temp = Item3.fromTemplate((short) g.getItemId());
                if (temp != null && temp.type == type) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * Ambil stat (List<Option>) untuk guild + type + class tertentu, langsung dari DB.
     * Return null kalau guild tersebut belum mengatur stat custom untuk class itu,
     * sehingga pemanggil tahu harus fallback ke stat random bawaan (StatGenerator).
     */
    public List<template.Option> getGuildItemOptions(int type, Guild guild, int clazz) {
        GuildItem config = findGuildItemConfig(type, guild);
        if (config == null) return null;
        // param di-random antara value..maxValue kalau maxValue diisi di DB,
        // kalau tidak (atau kolom-nya null) balik null supaya fallback ke StatGenerator.
        return config.rollOptionsForClass(clazz);
    }

    /**
     * Reload konfigurasi guild_item dari database (dipanggil setelah admin
     * mengubah stat lewat query/tool tanpa perlu restart server).
     */
    public void reloadClanItem() {
        guildItems.clear();
        loadClanItem();
    }


    private boolean isGuildItem(int itemId) {
        return guildItems.stream().anyMatch(guildItem -> guildItem.getItemId() == itemId);
    }

    public short getFashionPart(int itemId) {
        for (FashionData data : fashionData) {
            if (data.getItemId() == itemId) {
                return data.getPart();
            }
        }

        return -1;
    }

    private void loadClanItem() {
        List<GuildItem> items = SQLHelper.selectFrom("guild_item")
                .getAsModel(GuildItem.class);
        guildItems.addAll(items);

    }


}