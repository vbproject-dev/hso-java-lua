package client.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import devtools.AccountHelper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import game.guild.Guild;
import client.MessageHandler;
import client.Player;
import core.CheckDDOS;
import core.Log;
import core.Manager;
import core.SQL;
import core.SaveData;
import core.ServerManager;
import core.SessionManager;
import core.Util;

import game.map.GameMap;
import game.map.MapService;
import template.*;
import utils.IconHelper;
import utils.SQLHelper;

@Slf4j
public class Session implements Runnable {

    public static final List<Session> SESSION_LIST = new LinkedList<>();

    /**
     * Salinan aman (thread-safe) dari SESSION_LIST untuk diiterasi. SESSION_LIST ditulis (add/remove)
     * di dalam synchronized oleh thread lain; iterasi langsung tanpa lock bisa melempar
     * ConcurrentModificationException / IndexOutOfBoundsException.
     */
    public static Session[] snapshotList() {
        synchronized (SESSION_LIST) {
            return SESSION_LIST.toArray(new Session[0]);
        }
    }
    // public static final List<Session> client_entrys = new ArrayList<>();
    public final Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Thread sendd;
    private Thread receiv;
    public boolean connected;
    // FIX (root cause of recurring "HSO_pool - Interrupted during connection
    // acquisition" SQLExceptions): disconnect() had no guard against being
    // invoked twice concurrently for the same session. It can legitimately
    // be triggered from more than one thread — e.g. the player's own receiv
    // thread (natural EOF/socket close, or an anti-spam kick from
    // MapService.use_skill) AND, separately, a background game-tick/map
    // cleanup thread that notices the same socket already looks closed
    // (see GameMap's "!p.conn.connected" cleanup) and also calls
    // p.conn.close(). Every call unconditionally did
    // this.receiv.interrupt(), so a second, concurrent call would interrupt
    // the receiv thread from the FIRST call while it was still blocked
    // inside p.flush() waiting on a HikariCP connection — producing exactly
    // this InterruptedException mid-save. Guarding with an atomic
    // compare-and-set makes disconnect() run its body at most once.
    private final java.util.concurrent.atomic.AtomicBoolean disconnecting = new java.util.concurrent.atomic.AtomicBoolean(
            false);
    private final BlockingQueue<Message> list_msg;
    private boolean sendKeyComplete;
    private byte curR;
    private byte curW;

    private final byte[] keys = "@HSO".getBytes();
    public int id;
    public String user;
    public String pass;
    public byte ac_admin = 0;
    public String ip;
    public boolean lock;
    /**
     * Index item yang dipilih admin di menu OTHER_MENU - per-session agar tidak
     * race condition
     */
    public int adminSelectedIndex = -1;
    private final MessageHandler controller;
    public Player p;
    public byte zoomlv;
    public byte status;
    public boolean get_in4;
    public long timeConnect;
    public long tongnap;
    public int topnap;
    public long coin;
    public int version;

    // --- MEMBERSHIP ---
    public byte membership_type = 0; // 0=None, 1=7Day, 2=30Day
    public long membership_expire = 0L; // Unix timestamp milliseconds

    public HashMap<String, Object> state = new HashMap<>();


    public Session(Socket socket) {
        timeConnect = System.currentTimeMillis();
        Random random = new Random();
        random.nextBytes(keys);
        this.socket = socket;
        this.list_msg = new LinkedBlockingQueue<Message>();
        this.sendKeyComplete = false;
        this.connected = false;
        this.controller = new MessageHandler(this);
        get_in4 = false;
    }

    public void init() {
        try {
            this.ip = this.socket.getInetAddress().getHostAddress();

            if (this.ip != null && !this.ip.equals("127.0.0.1")
                    && (CheckDDOS.isIPExist(this.ip) || !CheckDDOS.canAccess(this.ip) || !CheckDDOS.checkCountIP(ip))) {
                this.socket.close();
                this.connected = false;
                synchronized (Session.SESSION_LIST) {
                    Session.SESSION_LIST.remove(this);
                }
            }

            this.dis = new DataInputStream(socket.getInputStream());
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.connected = true;
            this.get_in4 = false;
            this.sendd = new Thread(() -> {
                try {
                    while (connected) {
                        Message m = list_msg.poll(5, TimeUnit.SECONDS);
                        if (m != null) {
                            send_msg(m);
                            m.cleanup();
                        }
                    }
                } catch (InterruptedException e) {
                } catch (IOException e) {
                } finally {
                    // disconnect();
                }
            });
            this.receiv = new Thread(this);
            this.receiv.start();
            this.sendd.start();
            // CheckDDOS.addIp(ip);
            synchronized (Session.SESSION_LIST) {
                Session.SESSION_LIST.add(this);
            }
            System.out.println("accecpt ip " + ip + " - online : " + Session.SESSION_LIST.size());
        } catch (IOException e) {
            e.printStackTrace();
            this.connected = false;
        }
    }

    public void SaveIP() {
        String sql = "UPDATE `account` SET `last_ip` = '" + this.ip + "' WHERE id = " + this.id + ";";
        try (Connection connection = SQL.gI().getConnection(); Statement ps = connection.createStatement()) {
            if (ps.executeUpdate(sql) > 0) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        // FIX: make disconnect() idempotent/thread-safe. See the comment on
        // the `disconnecting` field above for why this is needed.
        if (!disconnecting.compareAndSet(false, true)) {
            return;
        }

        // Throttle login hanya untuk logout normal, bukan AFK
        if (this.p == null || !this.p.modeBot) {
            Manager.gI().time_login_client.put(this.user, (System.currentTimeMillis() + Manager.gI().time_login));
        }

        //
        // CheckDDOS.removeIp(ip);
        this.connected = false;
        this.sendd.interrupt();
        this.receiv.interrupt();
        if (this.p != null) {

            if (p.modeBot) {
                // ── Mode AFK ────────────────────────────────────────────────
                // Simpan data, tapi JANGAN keluarkan player dari map.
                // Karakter tetap di dunia game dan dikendalikan bot.
                boolean wasInterrupted2 = Thread.interrupted();
                try {
                    p.flush();
                } catch (Exception _e) {
                    core.Log.gI().add_log("system", "[AFK] flush gagal " + p.name);
                }
                try {
                    p.quest.save(p.objectId);
                } catch (Exception _e) {
                    core.Log.gI().add_log("system", "[AFK] quest.save gagal " + p.name);
                }
                try {
                    p.setOffline();
                } catch (Exception _e) {
                } // isOnline = modeBot = true → tetap "online"
                if (wasInterrupted2)
                    Thread.currentThread().interrupt();
                Log.gI().add_log(p.name, "[AFK] Masuk mode AFK");
                // ────────────────────────────────────────────────────────────
            } else {
                // ── Logout normal ────────────────────────────────────────────
                try {
                    if (p.party != null && p.party.get_mems().size() > 1) {
                        p.party.remove_mems(p);
                        p.party.sendin4();
                        p.party.send_txt_notice(p.name + " Meninggalkan party");
                        p.party = null;
                    }
                    if (p.name_trade != null && !p.name_trade.equals("")) {
                        Player p0;
                        p0 = GameMap.get_player_by_name(p.name_trade);
                        if (p0 != null) {
                            Message m = new Message(36);
                            m.writer().writeByte(6);
                            p0.conn.addmsg(m);
                            m.cleanup();
                            p0.name_trade = "";
                            p0.lock_trade = false;
                            p0.money_trade = 0;
                            p0.accept_trade = false;
                            p0.list_item_trade = null;
                        }
                    }
                    // Reset wedding chest flag saat disconnect
                    p.wedding_chest_open = false;
                    // Reset state MoLy agar tiket tidak hilang jika disconnect di tengah proses
                    p.id_select_mo_ly = -1;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Clear interrupted flag before DB saves so HikariCP can acquire connections
                boolean wasInterrupted = Thread.interrupted();
                try {
                    p.flush();
                } catch (Exception _e) {
                    core.Log.gI().add_log("system", "[Session] flush gagal " + p.name + ": " + _e.getMessage());
                }
                // Pause premium teleport timer saat logout
                feature.teleport.PremiumTeleportManager.gI().pauseSession(p);
                MapService.leave(p.map, p);
                // if (p.map.ld == null || (p.map.ld != null && p.map.ld.p1.id != p.id &&
                // p.map.ld.p2.id != p.id)) {
                // MapService.leave(p.map, p);
                // }
                Log.gI().add_log(p.name, "Logout : [Vàng] : " + Util.number_format(p.getGold()) + " : [Ngọc] : "
                        + Util.number_format(p.getGem()));

                try {
                    p.setOffline();
                } catch (Exception _e) {
                    core.Log.gI().add_log("system", "[Session] setOffline gagal " + p.name + ": " + _e.getMessage());
                }
                try {
                    p.quest.save(p.objectId);
                } catch (Exception _e) {
                    core.Log.gI().add_log("system", "[Session] quest.save gagal " + p.name + ": " + _e.getMessage());
                }
                if (wasInterrupted)
                    Thread.currentThread().interrupt(); // restore
            } // end else (logout normal)
        } // end if (this.p != null)
          //

        try {
            if (this.socket != null && this.socket.isConnected()) {
                this.socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (Session.SESSION_LIST) {
            Session.SESSION_LIST.remove(this);
        }
        System.out.println("disconnect session " + user + " - online : " + (Session.SESSION_LIST.size() - 1));
    }

    public void addmsg(Message m) {
        if (connected) {
            this.list_msg.add(m);
        }
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Message m = read_msg();
                if (m != null) {
                    if (this.ip.equals("127.0.0.1") && m.cmd == -1) {
                        ServerManager.gI().close();
                        System.out.println("Close server is processing....");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SaveData.process();
                                for (int k = Session.SESSION_LIST.size() - 1; k >= 0; k--) {
                                    Session.SESSION_LIST.get(k).p = null;
                                    try {
                                        Session s = SESSION_LIST.get(k);
                                        s.p = null;
                                        s.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Manager.gI().close();
                            }
                        }).start();
                    } else if (m.cmd == -40) {
                        sendkeys();
                    } else if (sendKeyComplete
                            && (this.p != null || m.cmd == 61 || m.cmd == 1 || m.cmd == 14 || m.cmd == 13)) {
                        try {
                            controller.process_msg(m);
                        } catch (IOException e) {
                            if (!(e instanceof java.io.EOFException)) {
                                e.printStackTrace();
                            }
                        }
                    }
                    m.cleanup();
                }
            }
        } catch (Exception e) {
            // EOFException and SocketException are expected on client disconnect - don't
            // log as errors
            if (!(e instanceof java.io.EOFException) && !(e instanceof java.net.SocketException)) {
                CheckDDOS.NextError(this.ip);
                e.printStackTrace();
            }
        } finally {
            if (!this.socket.isClosed()) {
                disconnect();
            }
        }
    }

    private void send_msg(Message msg) throws IOException {
        byte[] data = msg.getData();
        if (msg.cmd == 25) {
            msg.cmd = 126;
        }
        if (sendKeyComplete) {
            byte b = writeKey(msg.cmd);
            dos.writeByte(b);
        } else {
            dos.writeByte(msg.cmd);
        }

        if (data != null) {
            int size = data.length;
            SessionManager.addBandWidth(ip, size);
            if (msg.cmd == -51 || msg.cmd == -52 || msg.cmd == -54 || msg.cmd == 126) {
                if (msg.cmd == 126) {
                    byte bspec = writeKey((byte) 25);
                    dos.writeByte(bspec);
                }
                byte b4 = (byte) (size);
                byte b3 = (byte) ((byte) (size >> 8));
                byte b2 = (byte) ((byte) (size >> 16));
                byte b1 = (byte) ((byte) (size >> 24));
                final int byte4 = this.writeKey(b4);
                final int byte3 = this.writeKey(b3);
                final int byte2 = this.writeKey(b2);
                final int byte1 = this.writeKey(b1);
                this.dos.writeByte(byte1);
                this.dos.writeByte(byte2);
                this.dos.writeByte(byte3);
                this.dos.writeByte(byte4);
            } else if (sendKeyComplete) {
                int byte1 = writeKey((byte) (size >> 8));
                dos.writeByte(byte1);
                int byte2 = writeKey((byte) (size));
                dos.writeByte(byte2);
            } else {
                final int byte1 = (byte) (size & 0xFF00);
                this.dos.writeByte(byte1);
                final int byte2 = (byte) (size & 0xFF);
                this.dos.writeByte(byte2);
            }
            if (sendKeyComplete) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = writeKey(data[i]);
                }
            }
            dos.write(data);
        } else {
            final int byte1 = (byte) (0);
            this.dos.writeByte(byte1);
            final int byte2 = (byte) (0);
            this.dos.writeByte(byte2);
        }
        dos.flush();
        msg.cleanup();
        // Util.logconsole("___send msg : " + msg.cmd + " - size : " + data.length + " :
        // " + user, 1, msg.cmd);
    }

    private Message read_msg() throws IOException {
        byte cmd = dis.readByte();
        if (sendKeyComplete) {
            cmd = readKey(cmd);
        }
        int size;
        if (sendKeyComplete) {
            byte b1 = dis.readByte();
            byte b2 = dis.readByte();
            size = (readKey(b1) & 255) << 8 | readKey(b2) & 255;
        } else {
            size = dis.readShort();
        }
        byte data[] = new byte[size];
        int len = 0;
        int byteRead = 0;
        while (len != -1 && byteRead < size) {
            len = dis.read(data, byteRead, size - byteRead);
            if (len > 0) {
                byteRead += len;
            }
        }
        if (sendKeyComplete) {
            for (int i = 0; i < data.length; i++) {
                data[i] = readKey(data[i]);
            }
        }
        SessionManager.addBandWidth(ip, size);
        return new Message(cmd, data);
    }

    private byte readKey(final byte b) {
        final byte curR = this.curR;
        this.curR = (byte) (curR + 1);
        final byte i = (byte) ((keys[curR] & 0xFF) ^ (b & 0xFF));
        if (this.curR >= keys.length) {
            this.curR %= (byte) keys.length;
        }
        return i;
    }

    private byte writeKey(final byte b) {
        final byte curW = this.curW;
        this.curW = (byte) (curW + 1);
        final byte i = (byte) ((keys[curW] & 0xFF) ^ (b & 0xFF));
        if (this.curW >= keys.length) {
            this.curW %= (byte) keys.length;
        }
        return i;
    }

    private void sendkeys() throws IOException {
        Message m = new Message(-40);
        m.writer().writeByte(keys.length);
        m.writer().writeByte(keys[0]);
        for (int i = 1; i < keys.length; i++) {
            m.writer().writeByte(keys[i] ^ keys[i - 1]);
        }
        send_msg(m);
        m.cleanup();
        sendKeyComplete = true;
    }

    public void getClientInfo(Message m) throws IOException {
        if (!CheckDDOS.checkCountIP(ip)) {
            loginFail("This IP has reached the limit!");
            return;
        }
        this.user = m.reader().readUTF().trim();
        this.pass = m.reader().readUTF().trim();

        if (user.equals("1") && pass.equals("1")) {
            loginFail("Kunjungi http://ksatria.vbpixel.com untuk mendaftar akun");

            return;
        }

        String ver = m.reader().readUTF();
        m.reader().readUTF(); // clinePro
        m.reader().readUTF(); // pro
        m.reader().readUTF(); // agent
        this.zoomlv = m.reader().readByte();
        m.reader().readByte(); // device
        m.reader().readInt(); // id
        m.reader().readByte(); // area
        m.reader().readByte(); // !Main.isPC ? 0 : 1
        m.reader().readByte(); // IndexRes
        m.reader().readByte(); // indexInfoLogin
        m.reader().readByte(); // fake byte
        short indexCharPar = m.reader().readShort();
        m.reader().readUTF(); // stringPackageName

        if (ver.isEmpty()) {
            version = 309;
        } else {
            version = Integer.parseInt(ver.replace(".", ""));
        }
        int MIN_VERSION = 309;
        if (version < MIN_VERSION) {
            loginFail("Silakan update ke versi terbaru.\nSilakan kunjungi http://ksatria.vbpixel.com untuk mendownload patch terbaru.");
            return;
        }

        long time_can_login = 0;
        if (Manager.gI().time_login_client.containsKey(this.user)) {
            time_can_login = Manager.gI().time_login_client.get(this.user) - System.currentTimeMillis();
        }
        if (this.ac_admin <= 0 && time_can_login > 0 && !ip.equals("127.0.0.1")) {
            float t_ = ((float) time_can_login) / 1000f;
            loginFail("Kamu baru boleh login setelah " + String.format("%.1f", t_) + "detik!");
            return;
        }

        int dem = 0;
        for (Session other : Session.snapshotList()) {
            if (other != null && other.ip != null && other.ip.equals(this.ip)) {
                dem++;
            }
        }
        if (dem > Manager.gI().allow_ip_client) {
            loginFail("Jumlah IP yang dapat mengakses saat ini sudah terlampaui!");
            return;
        }

        if (!loadAccount()) {
            log.info("Terjadi kesalahan");
            return;
        }

        if (Manager.gI().isServerAdmin && this.ac_admin <= 0) {
            loginFail("This server can only be accessed by admin!");
            return;
        }
        int indexPart = PartDataLoader.getPartIndex(zoomlv);
        if (indexCharPar != indexPart) {
            Message m13 = new Message(63);
            m13.writer().writeByte(60);
            addmsg(m13);
            m13.cleanup();

            sendPartChar();
        } else {
            sendListCharacter();
        }
        //
        Message md = new Message(31);
        md.writer().writeUTF(user);
        md.writer().writeUTF(pass);
        addmsg(md);
        md.cleanup();

        //
        for (int id = 10200; id < 10242; id++) {
            byte[] data = IconHelper.getIcon(zoomlv, id);
            if (data == null)
                continue;

            Message m22 = new Message(-51);
            m22.writer().writeShort(id);
            m22.writer().write(data);
            addmsg(m22);
            m22.cleanup();
        }

        this.get_in4 = true;
    }

    public boolean loadAccount() {

        Boolean login;
        try {
            login = SQLHelper.selectFrom("account")
                    .where("user", user)
                    .and("pass", pass)
                    .firstAs(rs -> {
                        this.id = rs.getInt("id");
                        this.ac_admin = rs.getByte("ac_admin");
                        this.status = rs.getByte("status");
                        this.coin = rs.getLong("coin");
                        this.tongnap = rs.getLong("tongnap");
                        this.lock = rs.getByte("lock") == 1;
                        return true;
                    });
        } catch (core.DbUnavailableException e) {
            // DB lagi sibuk/pool habis - ini BUKAN akun salah, jangan disamakan.
            // Sebelumnya kondisi ini jatuh ke "Name pengguna atau password salah"
            // karena query yang gagal balikin null, sama seperti akun tidak ketemu.
            log.error("Login gagal karena database tidak tersedia untuk user: {}", user, e);
            loginFail("Server sedang sibuk, silakan coba login kembali beberapa saat lagi.");
            return false;
        }

        if (login == null || !login) {
            loginFail("Name pengguna atau password salah");
            log.info("Name pengguna atau password salah");
            return false;
        }

        // Load membership
        feature.membership.MembershipService.onLogin(this);

        long time_can_login = 0;
        if (Manager.gI().time_login_client.containsKey(this.user)) {
            time_can_login = Manager.gI().time_login_client.get(this.user) - System.currentTimeMillis();
        }

        if (this.ac_admin <= 0 && time_can_login > 0 && !ip.equals("127.0.0.1")) {
            float t_ = ((float) time_can_login) / 1000f;
            loginFail("Kamu baru boleh login setelah " + String.format("%.1f", t_) + "detik!");
            return false;
        }
        if (isLocked()) {
            loginFail("Akun ini terkunci untuk sementara waktu silahkan hubungi admin.");
            return false;
        }

        return true;
    }

    public void sendPartChar() {

        try {
            List<PartData> parts = PartDataLoader.getAllByZoom(zoomlv);
            int partIndex = PartDataLoader.getPartIndex(zoomlv);

            List<PartData> filtered = parts.stream()
                    .filter(Objects::nonNull)
                    .filter(partData -> partData.type != 113)
                    .toList();

            int size = filtered.size();

            Message m = new Message(-57);
            m.writer().writeShort(partIndex);
            m.writer().writeShort(size);
            addmsg(m);
            m.cleanup();

            for (PartData part : filtered) {
                m = new Message(-52);
                m.writer().writeByte(part.type);
                m.writer().writeShort(part.id);
                m.writer().writeInt(part.image.length);
                m.writer().write(part.image);
                m.writer().write(part.imageData);
                addmsg(m);
                m.cleanup();

            }

            sendListCharacter();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loginFail(String s) {
        try {
            Message m = new Message(2);
            m.writer().writeUTF(s);
            m.writer().writeByte(0);
            addmsg(m);
            m.cleanup();
        } catch (IOException ignore) {
        }
    }

    public void sendListCharacter() throws IOException {

        List<java.util.Map<String, Object>> players = SQLHelper.selectFrom("player")
                .where("uid", id)
                .get(rs -> {
                    java.util.Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("name", rs.getString("name"));
                    map.put("body", rs.getString("body"));
                    map.put("level", rs.getShort("level"));
                    map.put("clazz", rs.getByte("clazz"));
                    map.put("itemwear", rs.getString("itemwear"));
                    return map;
                });

        Message m = new Message(13);
        m.writer().writeByte(players.size());
        for (java.util.Map<String, Object> player : players) {
            String name = (String) player.get("name");
            m.writer().writeInt((int) player.get("id"));
            m.writer().writeUTF(name);
            JSONArray jsar = (JSONArray) JSONValue.parse((String) player.get("body"));
            if (jsar == null) {
                return;
            }
            m.writer().writeByte(Byte.parseByte(jsar.get(0).toString())); // head
            m.writer().writeByte(Byte.parseByte(jsar.get(2).toString())); // hair
            m.writer().writeByte(Byte.parseByte(jsar.get(1).toString())); // eye
            //
            jsar.clear();
            List<PlayerPart> itemwear = new ArrayList<>();
            jsar = (JSONArray) JSONValue.parse((String) player.get("itemwear"));
            if (jsar == null) {
                return;
            }
            for (Object o : jsar) {
                JSONArray jsar2 = (JSONArray) JSONValue.parse(o.toString());
                if (jsar2 == null) {
                    return;
                }
                byte index_wear = Byte.parseByte(jsar2.get(9).toString());
                if (index_wear != 0 && index_wear != 1 && index_wear != 6 && index_wear != 7 && index_wear != 10) {
                    continue;
                }
                PlayerPart temp = new PlayerPart();
                temp.type = Byte.parseByte(jsar2.get(2).toString());
                temp.part = Short.parseShort(jsar2.get(6).toString());
                itemwear.add(temp);
            }
            jsar.clear();
            m.writer().writeByte(itemwear.size()); // size part
            for (PlayerPart playerPart : itemwear) {
                m.writer().writeByte(playerPart.type);
                m.writer().writeByte(playerPart.part);
            }
            short level_ = (short) player.get("level");
            if (level_ > Manager.gI().lvmax) {
                level_ = (short) Manager.gI().lvmax;
            }
            m.writer().writeShort(level_);
            m.writer().writeByte((byte) player.get("clazz"));
            m.writer().writeByte(1); // fake
            m.writer().writeByte(0); // fake
            Guild guild = Guild.getPlayerGuild(name);
            if (guild != null) {
                m.writer().writeShort(guild.icon);
                m.writer().writeUTF(guild.shortName);
                m.writer().writeByte(guild.get_mem_type(name));
            } else {
                m.writer().writeShort(-1);
            }
        }
        addmsg(m);
        m.cleanup();

    }

    public void charCreate(Message m) throws IOException {
        if (!Manager.gI().ip_create_char.containsKey(this.ip)) {
            Manager.gI().ip_create_char.put(this.ip, 0);
        }
        int time_ = Manager.gI().ip_create_char.get(this.ip);
        if (time_ > 2) {
            notice_create_char("K");
            // return;
        }

        byte clazz = m.reader().readByte();
        String name = m.reader().readUTF().toLowerCase();
        byte hair = m.reader().readByte();
        byte eye = m.reader().readByte();
        byte head = m.reader().readByte();

        log.info("charCreate recv: ip={}, clazz={}, name={}, hair={}, eye={}, head={}",
                this.ip, clazz, name, hair, eye, head);

        // Valid ranges across ALL classes (union), so a stray index from the
        // client never gets hard-rejected.
        var HEADS = List.of(0, 1);
        var EYES_ALL = List.of(8, 9, 10, 11);
        var HAIRS_ALL = List.of(0, 1, 2, 3);

        if (!HEADS.contains((int) head)) {
            head = 0;
        }
        if (!EYES_ALL.contains((int) eye)) {
            eye = 8;
        }
        if (!HAIRS_ALL.contains((int) hair)) {
            hair = 0;
        }

        // Keep the intended art direction: eye/hair "style" (the offset within
        // its pair) is preserved, but re-based onto the pair that matches the
        // chosen class, instead of rejecting the request outright.
        int eyeBase = clazz < 2 ? 8 : 10;
        int eyeOffset = eye < 10 ? eye - 8 : eye - 10;
        eye = (byte) (eyeBase + eyeOffset);

        int hairBase = clazz < 2 ? 0 : 2;
        int hairOffset = hair < 2 ? hair : hair - 2;
        hair = (byte) (hairBase + hairOffset);

        Pattern p = Pattern.compile("^[a-zA-Z0-9]{6,10}$");
        if (!p.matcher(name).matches()) {
            notice_create_char("Nama nggak valid, coba lagi!");
            return;
        }
        if (name.contains("ad") || name.contains("server") || name.contains("sever") || name.contains("thongbao")) {
            notice_create_char("Nama tidak valid coba lagi!");
            return;
        }

        if (AccountHelper.isPlayerExists(name)) {
            loginFail(String.format("Nama %s sudah terpakai", name));
            return;
        }

        if (SQLHelper.selectFrom("player")
                .where("uid", id)
                .count() >= 3) {

            loginFail("Tidak ada ruang untuk membuat karakter baru");
            return;
        }

        JSONArray body = new JSONArray();
        body.add(head);
        body.add(eye);
        body.add(hair);

        JSONArray skills = new JSONArray();
        skills.add(1);
        for (int i = 0; i < 20; i++) {
            skills.add(0);
        }

        java.util.Map<String, Object> values = new HashMap<>();
        values.put("uid", id);
        values.put("name", name);
        values.put("body", body.toJSONString());
        values.put("clazz", clazz);
        values.put("level", 1);
        values.put("exp", 0L);
        values.put("site", "[0,132,354]");
        values.put("item4", "[[11,1],[14,1]]");
        values.put("item7", "[]");
        values.put("vang", 100_000L);
        values.put("kimcuong", 10_000);
        values.put("tiemnang", (short) 5);
        values.put("kynang", (short) 1);
        values.put("point1", (short) 5);
        values.put("point2", (short) 5);
        values.put("point3", (short) 5);
        values.put("point4", (short) 5);
        values.put("skill", skills.toJSONString());
        values.put("item3", "[]");
        values.put("item5", "[]");
        values.put("itemwear", AccountHelper.getItemwear(clazz));
        values.put("giftcode", "[]");
        values.put("pet", "[]");
        values.put("maxbag", (byte) 126);
        values.put("itembox3", "[]");
        values.put("itembox4", "[]");
        values.put("itembox7", "[]");
        values.put("rms_save", "[[],[]]");
        values.put("date", Date.from(Instant.now()).toString());
        values.put("diemdanh", (byte) 1);
        values.put("eff", "[]");
        values.put("itembox5", "[]");
        values.put("friend", "[]");
        values.put("enemies", "[]");
        values.put("typeexp", (byte) 1);
        values.put("medal_create_material",
                "[295,261,318,328,341,249,285,321,329,344,284,280,316,327,344,288,280,317,327,342]");
        values.put("medal_create_material_v2", "[]"); // Medal V2 — akan di-generate otomatis saat pertama dipakai
        values.put("point_active", "[3,0]");

        boolean inserted = SQLHelper.insert("player")
                .values(values)
                .executeAndCheck();

        if (!inserted) {
            loginFail("Terjadi Kesalahan");
            return;
        }

        Manager.gI().ip_create_char.replace(this.ip, time_, (time_ + 1));
        sendListCharacter();

    }

    private void notice_create_char(String s) throws IOException {
        Message m = new Message(37);
        m.writer().writeUTF(s);
        m.writer().writeUTF("");
        m.writer().writeByte(0);
        addmsg(m);
        m.cleanup();
    }

    boolean isAdmin() {
        return this.ac_admin > 0;
    }

    boolean isLocalhost() {
        return "127.0.0.1".equals(ip);
    }

    private boolean isLocked() {
        return this.lock;
    }

    public void close() throws IOException {
        this.disconnect();
    }

}