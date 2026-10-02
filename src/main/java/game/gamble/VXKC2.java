
package game.gamble;

import history.His_VXMM;
import client.Player;
import core.Log;
import core.Manager;
import core.Service;
import core.Util;
import client.io.Message;
import client.io.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VXKC2 implements Runnable {

    public static long LastGameMoney = 0;
    public Thread mainloopThread;
    private boolean running;
    private boolean started;
    private short time;
    private String last_winner = "Belum Ada Pemenang";
    private long vang_win = 0;
    private int vang_join = 0;
    private HashMap<Integer, Integer> list_playerHashMap = new HashMap<>();

    public VXKC2() {
        time = 120;
        started = false;
        mainloopThread = new Thread(this);
        mainloopThread.start();
    }

    public void send_in4(Player p) throws IOException {
        Message m = new Message(-32);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(87);
        String text = "Roda Putar Permata\r\n" + "Waktu\r\n" + getTime() + "\r\n" + " Total permata: \r\n"
                + Util.number_format(get_total_vang()) + "\r\n" + " Permata ikut serta: \r\n"
                + Util.number_format((list_playerHashMap.containsKey(p.objectId)) ? list_playerHashMap.get(p.objectId) : 0)
                + "\r\n"
                + "Tingkat kemenangan: " + getPercent(p) + "%\r\n" + "Jumlah orang saat ini: " + get_join() + "\r\n"
                + "Pemenang sebelumnya: " + getLastWinner() + "\r\n" + "Jumlah permata yang dimenangkan: " + getVangWin() + "\r\n"
                + "Jumlah permata ikut serta: " + getVangJoin();
        m.writer().writeUTF(text);
        p.conn.addmsg(m);
        m.cleanup();
    }

    private String getPercent(Player p) {
        if (list_playerHashMap.containsKey(p.objectId)) {
            float percent = ((float) list_playerHashMap.get(p.objectId) * 100) / get_total_vang();
            return String.format("%.3f", percent);
        }
        return "0.0";
    }

    private int get_join() {
        return list_playerHashMap.size();
    }

    private long get_total_vang() {
        long total = LastGameMoney / 2;
        for (Map.Entry<Integer, Integer> player : list_playerHashMap.entrySet()) {
            total += player.getValue();
        }
        return total;
    }

    private String getVangJoin() {
        if (vang_join > 0) {
            return Util.number_format(vang_join);
        }
        return "chưa có";
    }

    private String getVangWin() {
        if (vang_win > 0) {
            return Util.number_format(vang_win);
        }
        return "chưa có";
    }

    private String getLastWinner() {
        return last_winner;
    }

    private String getTime() {
        if (started) {
            if (time <= 120 && time > 60) {
                if (time > 60 && time <= 69) {
                    return "01:0" + (time - 60);
                } else {
                    return "01:" + (time - 60);
                }
            } else if (time >= 0 && time <= 60) {
                if (time > 0 && time <= 9) {
                    return "00:0" + time;
                } else {
                    return "00:" + time;
                }
            }
        } else {
            return "02:00";
        }
        return "0";
    }

    @Override
    public void run() {
        running = true;
        long time1 = 0;
        long time2 = 0;
        while (running) {
            try {
                time1 = System.currentTimeMillis();
                update();
                time2 = System.currentTimeMillis();
                long time3 = (1000L - (time2 - time1));
                if (time3 > 0) {
                    Thread.sleep(time3);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private synchronized void update() {
        if (started) {
            time--;
            if (time <= 0) {
                try {
                    notice_winner();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void notice_winner() throws IOException {
        His_VXMM hist = new His_VXMM((byte) 1);
        int index = -1;
        int dem = 0;
        for (int i = 0; i < 20 && index == -1; i++) {
            dem = 0;
            for (Map.Entry<Integer, Integer> player : list_playerHashMap.entrySet()) {
                long percent = (((long) list_playerHashMap.get(player.getKey())) * 100L) / get_total_vang();
                if (percent > Util.random(100)) {
                    index = dem;
                }
                if (index != -1) {
                    break;
                }
                dem++;
            }
        }

        if (index == -1) {
            index = Util.random(0, list_playerHashMap.size()); // random win :v
        }
        dem = 0;
        for (Map.Entry<Integer, Integer> player : list_playerHashMap.entrySet()) {
            if (dem == index) {
                Player p0 = null;
                for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
                    Session s = Session.SESSION_LIST.get(i);
                    if (s == null || s.p == null) {
                        continue;
                    }
                    if (s.p.objectId == player.getKey()) {
                        p0 = s.p;
                        break;
                    }
                }

                if (p0 != null && p0.map != null) {
                    hist.namePWin = p0.name;
                    hist.lastMoney = p0.getGem();
                    hist.moneyround = get_total_vang();

                    last_winner = p0.name;
                    vang_join = player.getValue();
                    vang_win = (get_total_vang() * 7L) / 10L;
                    Manager.gI().chatKTGprocess(last_winner + " telah memenangkan " + Util.number_format(vang_win)
                            + " permata saat berpartisipasi dalam roda putar keberuntungan");
                    p0.updateGem(vang_win);
                    Log.gI().add_log(p0.name, "VXKC menang " + Util.number_format(vang_win) + " permata");
                    p0.item.charInventory(5);

                    hist.affterMoney = p0.getGem();
                    hist.Logger = "hadir";
                    hist.moneyJoin = vang_join;
                    hist.Flus();
                    LastGameMoney = 0;
                } else {
                    hist.moneyJoin = player.getValue();
                    hist.moneyround = get_total_vang();
                    hist.Logger = "tidak hadir";
                    hist.Flus();

                    Manager.gI().chatKTGprocess(
                            "Pemenang sudah offline jadi setengah hadiah akan dipindah ke putaran berikutnya");
                    LastGameMoney = get_total_vang();
                }
                break;
            }
            dem++;
        }
        refresh();
    }

    public synchronized void refresh() {
        started = false;
        this.list_playerHashMap.clear();
        time = 120;
    }

    public void close() {
        running = false;
        mainloopThread.interrupt();
        mainloopThread = null;
    }

    public synchronized void join_vxmm(Player p, int vang_join_vxmm) throws IOException {
        if (time > 10) {
            if (Manager.isLockVX) {
                Service.send_notice_box(p.conn, "Tôi cần nghỉ ngơi, hãy quay lại sau!");
                return;
            }
            if (list_playerHashMap.containsKey(p.objectId)) {
                Service.send_notice_box(p.conn, "Anda hanya bisa berpartisipasi 1 kali");
                return;
            }
            if (p.conn.status != 0) {
                Service.send_notice_box(p.conn, "Bạn không thể tham gia!");
                return;
            }
            if (list_playerHashMap.containsKey(p.objectId)
                    && (list_playerHashMap.get(p.objectId) + vang_join_vxmm) > 50_000) {
                Service.send_notice_box(p.conn, "Hanya bisa berpartisipasi maksimal 50k permata");
                return;
            }
            if ((get_total_vang() + vang_join_vxmm) > 50_000_000) {
                Service.send_notice_box(p.conn, "Total permata dalam roda putar maksimal hanya 50 juta");
                return;
            }
            p.updateGem(-vang_join_vxmm);
            Log.gI().add_log(p.name, "VXKC bermain " + Util.number_format(vang_join_vxmm) + " permata");
            p.item.charInventory(5);
            Service.send_notice_box(p.conn,
                    "berpartisipasi " + Util.number_format(vang_join_vxmm) + " permata berhasil");
            if (!list_playerHashMap.containsKey(p.objectId)) {
                list_playerHashMap.put(p.objectId, vang_join_vxmm);
            } else {
                int add = list_playerHashMap.get(p.objectId) + vang_join_vxmm;
                list_playerHashMap.replace(p.objectId, list_playerHashMap.get(p), add);
            }
            if (list_playerHashMap.size() > 1 && !started) {
                this.start_rotate();
            }
        } else {
            Service.send_notice_box(p.conn, "Tidak bisa menambah ketika hanya tersisa 10 detik lagi");
        }
    }

    private synchronized void start_rotate() throws IOException {
        Manager.gI().chatKTGprocess("Roda putar permata mulai berputar!!");
        started = true;
    }
}
