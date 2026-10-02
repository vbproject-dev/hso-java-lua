package topup;

import client.Player;

import core.Service;
import core.Util;
import lombok.extern.slf4j.Slf4j;
import utils.SQLHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
public class TopupController {

    private volatile List<Topup> topups = new CopyOnWriteArrayList<>();

    private TopupController() {
    }

    private static class Holder {
        private static final TopupController INSTANCE = new TopupController();
    }

    public static TopupController getInstance() {
        return Holder.INSTANCE;
    }

    public void find(Player player) {
        List<Short> items = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Short> categories = new ArrayList<>();

        Iterator<Topup> it = topups.iterator();
        while (it.hasNext()) {
            Topup t = it.next();

            try {
                if (!t.getName().equalsIgnoreCase(player.name)) continue;

                if (t.getStatus() != Status.PENDING) {
                    topups.remove(t);
                    continue;
                }

                boolean updated = SQLHelper
                        .update("user_topup")
                        .where("id", t.getId())
                        .where("status", Status.PENDING.name())
                        .set("status", Status.SUCCESS.name())
                        .execute();

                if (!updated) {
                    log.warn("Topup id={} sudah diproses sebelumnya, skip grant (anti double).", t.getId());
                    topups.remove(t);
                    continue;
                }

                t.setStatus(Status.SUCCESS);

                if (t.getType() == TopupType.GOLD) {
                    items.add((short) -1);
                    categories.add((short) 4);
                    quantities.add((int) t.getAmount());
                    player.updateGold(t.getAmount());
                    addTopupPoint(player, TopupType.GOLD, t.getAmount());

                } else if (t.getType() == TopupType.GEM) {
                    items.add((short) -2);
                    categories.add((short) 4);
                    quantities.add((int) t.getAmount());
                    player.updateGem(t.getAmount());
                    addTopupPoint(player, TopupType.GEM, t.getAmount());
                }

                topups.remove(t);

            } catch (Exception e) {
                log.error("Gagal memproses topup id={} untuk player={}", t.getId(), player.name, e);
            }
        }

        if (!items.isEmpty()) {
            try {
                short[] ar_id = new short[items.size()];
                int[] ar_quant = new int[quantities.size()];
                short[] ar_type = new short[categories.size()];
                for (int i = 0; i < ar_id.length; i++) {
                    ar_id[i] = items.get(i);
                    ar_quant[i] = quantities.get(i);
                    ar_type[i] = categories.get(i);
                }
                player.item.charInventory(4);
                Service.Show_open_box_notice_item(player, "Berhasil Top Up", ar_id, ar_quant, ar_type);
            } catch (Exception e) {
                log.error("Gagal menampilkan notice topup untuk player={}", player.name, e);
            }
        }
    }

    /**
     * Akumulasi poin top up player, dipisah antara emas dan permata.
     * - GOLD -> kolom `poin_isi_ulang_emas`
     * - GEM  -> kolom `poin_isi_ulang_permata` (dipakai untuk stat StatType.GEM_RECHARGE_POINTS / "Poin isi ulang permata")
     * Ditulis langsung ke DB (bukan menunggu save berkala) supaya poin tidak hilang jika server crash.
     */
    private void addTopupPoint(Player player, TopupType type, long amount) {
        if (amount <= 0) return;

        String column = (type == TopupType.GOLD) ? "poin_isi_ulang_emas" : "poin_isi_ulang_permata";

        try {
            boolean ok = SQLHelper
                    .update("player")
                    .where("id", player.objectId)
                    .increment(column, amount)
                    .execute();

            if (!ok) {
                log.warn("Gagal update kolom {} untuk player={} (amount={})", column, player.name, amount);
                return;
            }

            if (type == TopupType.GOLD) {
                player.poinIsiUlangEmas += amount;
                notifyTopupPoint(player, "emas", player.poinIsiUlangEmas);
            } else {
                player.poinIsiUlangPermata += amount;
                notifyTopupPoint(player, "permata", player.poinIsiUlangPermata);
            }
        } catch (Exception e) {
            log.error("Gagal menambah poin topup ({}) untuk player={}", type, player.name, e);
        }
    }

    private void notifyTopupPoint(Player player, String label, long total) {
        try {
            if (player.conn != null) {
                Service.send_notice_nobox_white(player.conn,
                        "Total poin isi ulang " + label + " kamu sekarang: " + Util.shortFormat(total));
            }
        } catch (Exception e) {
            log.error("Gagal kirim notice poin topup untuk player={}", player.name, e);
        }
    }

    public void loadData() {
        try {
            List<Topup> loaded = SQLHelper
                    .selectFrom("user_topup")
                    .where("status", Status.PENDING.name())
                    .get(rs -> {

                        Topup t = new Topup();
                        t.setId(rs.getInt("id"));
                        t.setName(rs.getString("name"));
                        t.setAmount(rs.getLong("jumlah"));

                        // safer enum mapping
                        t.setType(TopupType.valueOf(rs.getString("type").toUpperCase()));
                        t.setStatus(Status.valueOf(rs.getString("status").toUpperCase()));

                        // DATETIME -> LocalDate
                        LocalDateTime ldt = rs.getObject("created_at", LocalDateTime.class);
                        t.setDate(ldt != null ? ldt.toLocalDate() : null);

                        return t;
                    });

            if (loaded != null) {
                topups = new CopyOnWriteArrayList<>(loaded);
                log.info("Loaded {} topup data ", topups.size());
            } else {
                log.warn("loadData() mengembalikan null, list topup tidak diubah.");
            }
        } catch (Exception e) {
            log.error("Gagal load data topup", e);
        }
    }
}