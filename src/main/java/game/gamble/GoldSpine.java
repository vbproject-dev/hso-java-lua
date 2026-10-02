
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

public class GoldSpine implements Runnable {

    public static long LastGameMoney = 0;
    public Thread mainloopThread;
    private boolean running;
    private boolean started;
    private short time;
    private String last_winner = "Belum ada pemenang";
    private long vang_win = 0;
    private int vang_join = 0;
    private HashMap<Integer, Integer> listPlayerHashMap = new HashMap<>();

    public GoldSpine() {
        time = 120;
        started = false;
        mainloopThread = new Thread(this);
        mainloopThread.start();
    }

    public void send_in4(Player p) throws IOException {
        Message m = new Message(-32);
        m.writer().writeShort(p.objectId);
        m.writer().writeByte(86);
        String text = "VIP Spin\r\n" +
                "Time\r\n" + getTime() + "\r\n" +
                " Total Gold: \r\n" + Util.number_format(getTotalVang()) + "\r\n" +
                " Joined Gold: \r\n" + Util.number_format(listPlayerHashMap.getOrDefault(p.objectId, 0)) + "\r\n" +
                "Win Rate: " + getPercent(p) + "%\r\n" +
                "Current Players: " + getJoin() + "\r\n" +
                "Last Winner: " + getLastWinner() + "\r\n" +
                "Gold Won: " + getVangWin() + "\r\n" +
                "Gold Joined: " + getVangJoin();

        m.writer().writeUTF(text);
        p.conn.addmsg(m);
        m.cleanup();
    }

    private String getPercent(Player p) {
        if (listPlayerHashMap.containsKey(p.objectId)) {
            float percent = ((float) listPlayerHashMap.get(p.objectId) * 100) / getTotalVang();
            return String.format("%.3f", percent);
        }
        return "0.0";
    }

    private int getJoin() {
        return listPlayerHashMap.size();
    }

    private long getTotalVang() {
        long total = LastGameMoney / 2;
        for (Map.Entry<Integer, Integer> player : listPlayerHashMap.entrySet()) {
            total += player.getValue();
        }
        return total;
    }

    private String getVangJoin() {
        if (vang_join > 0) {
            return Util.number_format(vang_join);
        }
        return "Belum ada peserta";
    }

    private String getVangWin() {
        if (vang_win > 0) {
            return Util.number_format(vang_win);
        }
        return "Belum ada peserta";
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
        His_VXMM hist = new His_VXMM((byte) 0);
        int index = -1;
        int dem = 0;
        for (int i = 0; i < 20 && index == -1; i++) {
            dem = 0;
            for (Map.Entry<Integer, Integer> player : listPlayerHashMap.entrySet()) {
                long percent = (((long) listPlayerHashMap.get(player.getKey())) * 100L) / getTotalVang();
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
            index = Util.random(0, listPlayerHashMap.size()); // random win :v
        }
        dem = 0;
        for (Map.Entry<Integer, Integer> player : listPlayerHashMap.entrySet()) {
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
                    hist.lastMoney = p0.getGold();
                    hist.moneyround = getTotalVang();

                    last_winner = p0.name;
                    vang_join = player.getValue();
                    vang_win = getTotalVang();
                    long thue = (getTotalVang() / 100) * Manager.thue;
                    vang_win -= thue;
                    if (Manager.guildThue != null)
                        Manager.guildThue.updateGold(thue);
                    Manager.gI().chatKTGprocess(last_winner + " telah memenangkan " + Util.number_format(vang_win)
                            + " emas saat berpartisipasi dalam roda putar keberuntungan");
                    p0.updateGold(vang_win);

                    Log.gI().add_log(p0.name, "VXMM menang " + Util.number_format(vang_win) + " emas");
                    p0.item.charInventory(5);

                    hist.affterMoney = p0.getGold();
                    hist.Logger = "hadir";
                    hist.moneyJoin = vang_join;
                    hist.Flus();
                    LastGameMoney = 0;
                } else {
                    hist.moneyJoin = player.getValue();
                    hist.moneyround = getTotalVang();
                    hist.Logger = "tidak hadir";
                    hist.Flus();

                    Manager.gI().chatKTGprocess(
                            "Pemenang sudah offline jadi setengah hadiah akan dipindah ke ronde berikutnya");
                    LastGameMoney = getTotalVang();
                }
                break;
            }
            dem++;
        }
        refresh();
    }

    public synchronized void refresh() {
        started = false;
        this.listPlayerHashMap.clear();
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
                Service.send_notice_box(p.conn, "Saya perlu istirahat, silakan kembali lagi nanti!");
                return;
            }
            if (listPlayerHashMap.containsKey(p.objectId)) {
                Service.send_notice_box(p.conn, "Anda hanya bisa berpartisipasi 1 kali!");
                return;
            }
            if (p.conn.status != 1) {
                Service.send_notice_box(p.conn, "Anda tidak bisa berpartisipasi!");
                return;
            }
            if (listPlayerHashMap.containsKey(p.objectId)
                    && (listPlayerHashMap.get(p.objectId) + vang_join_vxmm) > 200_000_000) {
                Service.send_notice_box(p.conn, "Hanya bisa berpartisipasi maksimal 200.000.000 emas");
                return;
            }
            if ((getTotalVang() + vang_join_vxmm) > 2_000_000_000) {
                Service.send_notice_box(p.conn, "Total emas dalam roda putar maksimal hanya 2 miliar");
                return;
            }
            p.updateGold(-vang_join_vxmm);
            Log.gI().add_log(p.name, "VXMM bermain " + Util.number_format(vang_join_vxmm) + " emas");
            p.item.charInventory(5);
            Service.send_notice_box(p.conn, "berpartisipasi " + Util.number_format(vang_join_vxmm) + " emas berhasil");
            if (!listPlayerHashMap.containsKey(p.objectId)) {
                listPlayerHashMap.put(p.objectId, vang_join_vxmm);
            } else {
                int add = listPlayerHashMap.get(p.objectId) + vang_join_vxmm;
                listPlayerHashMap.replace(p.objectId, listPlayerHashMap.get(p.objectId), add);
            }
            if (listPlayerHashMap.size() > 1 && !started) {
                this.start_rotate();
            }
        } else {
            Service.send_notice_box(p.conn, "Tidak bisa menambah ketika hanya tersisa 10 detik lagi");
        }
    }

    private synchronized void start_rotate() throws IOException {
        Manager.gI().chatKTGprocess("Roda putar VIP mulai berputar!!");
        started = true;
    }
}
