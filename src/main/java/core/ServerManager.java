package core;

import core.lua.NativeLua;
import game.event.DailyReset;
import game.event.GameEventManager;
import event_daily.CastleSiegeManager;
import game.pvp.PvpManager;
import client.io.Session;
import lombok.extern.slf4j.Slf4j;
import game.map.GameMap;
import model.player.PlayerData;
import topup.TopupController;
import utils.SQLHelper;
import utils._Time;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerManager implements Runnable {

    private static ServerManager instance;

    private Thread socketThread;
    private volatile boolean running;

    private ServerSocket server;
    private ScheduledExecutorService scheduler;

    private final long startTime;

    private ServerManager() {
        this.startTime = System.currentTimeMillis();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    // ================= INIT =================

    public void init() {
        Manager.gI().init();

        feature.teleport.PremiumTeleportManager.gI().init();

        List<PlayerData> playerDataList = SQLHelper.selectFrom("player").getAsModel(PlayerData.class);
        for (PlayerData data : playerDataList) {
            SQLHelper.update("player").where("name", data.getName()).set("active", false).execute();
        }
        
        running = true;

        // Scheduler (TIME LOGIC)
        scheduler = Executors.newScheduledThreadPool(2);

        // Main game tick (1 second)
        scheduler.scheduleAtFixedRate(this::serverTick, 0, 1, TimeUnit.SECONDS);

        // Cleanup & monitor (30 seconds)
        scheduler.scheduleAtFixedRate(this::cleanupTick, 30, 30, TimeUnit.SECONDS);

        // Socket accept thread
        socketThread = new Thread(this, "Socket-Acceptor");
        socketThread.start();

        log.info("Server started in {} ms", System.currentTimeMillis() - startTime);
    }

    // ================= SOCKET =================

    @Override
    public void run() {
        try {
            server = new ServerSocket(Manager.gI().server_port);
            log.info("LISTEN PORT {}...", Manager.gI().server_port);

            while (running) {
                Socket client = server.accept();
                if (!running) break;

                Session session = new Session(client);
                session.init();
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
        System.out.println("-----------GAME EXIT SOCKET----------");
    }

    // ================= GAME TICK =================

    private void serverTick() {
        try {
            Calendar now = Calendar.getInstance();

            int hour = now.get(Calendar.HOUR_OF_DAY);
            int min = now.get(Calendar.MINUTE);
            int sec = now.get(Calendar.SECOND);
            int day = now.get(Calendar.DAY_OF_WEEK);

            // ===== Save data =====
            if (sec == 10) {
                TopupController.getInstance().loadData();
                SaveData.process();
            }

            if (NativeLua.isLoaded()) {
                NativeLua.update(sec);
            }

            // ===== Global chat =====

            for (Notification notif : Manager.notifications) {
                if (min % notif.getDelay() == 0 && sec == 0) {
                    switch (notif.getId()) {
                        case 3 -> Manager.gI().chatKTGprocess(String.format(notif.getContent(), Manager.thue, Manager.nameClanThue));
                        // REVERT: Manager.gI().exp balik ke skala persen (100 = x1),
                        // jadi dibagi 100 lagi di sini biar notif yang ditampilkan
                        // ke player benar (mis. exp=200 -> tampil "x2", bukan "x200").
                        case 4 -> Manager.gI().chatKTGprocess(String.format(notif.getContent(), Manager.gI().exp / 100));
                        default -> Manager.gI().chatKTGprocess(notif.getContent());
                    }

                }
            }


            if (min % 11 == 0 && sec == 0) {
                CheckDDOS.ClearRam();
            }

            // ===== Sessions =====
            if (sec % 30 == 0) {
                SessionManager.removeClient();
            }

            // ===== Daily reset =====
            // FIX: sebelumnya trigger-nya "hour==0 && min==0 && sec==1" -- cuma
            // cocok PERSIS di satu detik itu sekali sehari, tanpa catch-up kalau
            // tick telat lewat jendela itu (reset jadi tidak jalan sama sekali
            // hari itu). Diganti ke DailyReset.checkMidnightReset(): dicek tiap
            // tick (murah, cuma bandingkan tanggal), otomatis kepicu begitu tick
            // manapun jalan setelah lewat tengah malam, tidak butuh detik presisi.
            if (DailyReset.checkMidnightReset()) {
                Manager.gI().ip_create_char.clear();

                for (GameMap[] gameMaps : GameMap.entrys) {
                    if (gameMaps == null) continue;
                    for (GameMap gameMap : gameMaps) {
                        if (gameMap == null || gameMap.players == null) continue;
                        gameMap.players.forEach(p -> {
                            try {
                                p.change_new_date();
                            } catch (Exception ignored) {
                            }
                        });
                    }
                }
            }

            // ===== PVP & EVENTS (termasuk PremiumBossManager via GameEventManager) =====
            PvpManager.gI().update();
            GameEventManager.gI().update();
            BossHDL.BossManager.Update();

            // ===== Castle Siege =====
            if (day % 2 == 0 && hour >= 20 && hour <= 23) {
                if (hour == 20 && min == 45 && sec == 0) {
                    CastleSiegeManager.StartRegister();
                } else if (hour == 21 && min == 30 && sec == 0) {
                    CastleSiegeManager.EndRegister();
                }
                CastleSiegeManager.update();
            }

            if (sec == 30 ) {
                for (Session session : Session.snapshotList()) {
                    if (session == null || session.p == null) {
                        continue;
                    }

                    if (session.p.isOnline) {
                        TopupController.getInstance().find(session.p);
                    }
                }
            }



            _Time.timeDay = _Time.GetTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CLEANUP =================

    private void cleanupTick() {
        try {
            SessionManager.checkBandWidth();

            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            System.out.println("[MEMORY] Used Heap: " + usedMb + " MB");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SHUTDOWN =================

    public void close() throws IOException {
        System.out.println("----------SERVER CLOSE----------");
        running = false;

        feature.teleport.PremiumTeleportManager.gI().shutdown();

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        if (server != null && !server.isClosed()) {
            server.close();
        }
    }
}