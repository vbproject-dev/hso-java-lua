package game.guild;

import client.Player;
import core.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import client.io.Message;
import client.io.Session;

import java.sql.PreparedStatement;

import game.map.GameMap;
import game.map.MapService;
import template.ClanMember;
import template.Item3;
import template.Item47;
import template.Level;
import template.Crystal;
import template.PlayerPart;

@Slf4j
@Data
public class Guild {

    public static final short[] item_shop
            = new short[]{19, 20, 146, 159, 160, 161, 163, 323,325};
    public static final List<Guild> entrys = new ArrayList<>();
    public static int[] vang_upgrade = new int[]{0, 1_000_000};
    public static int[] ngoc_upgrade = new int[]{0, 1_000};
    public List<ClanMember> members;
    public String name;
    public String shortName;
    public short icon;
    public short level;
    public long exp;
    public String slogan;
    public String rule;
    public int gems;
    public long gold;
    public String notice;
    public int maxMember;
    public List<Item47> clanItems;
    public List<Crystal> crystals;


    public void addCrystal(Crystal temp_mob) {
        this.crystals.add(temp_mob);
    }

    public void removeCrystal(Crystal temp_mob) {
        this.crystals.remove(temp_mob);
    }

    public void sendMemberRewards() {

        if (crystals == null || crystals.isEmpty()) {
            return;
        }

        int gem = (200 * crystals.size());
        int gold = (2_000_000 * crystals.size());

        // Salinan daftar member: accept_mem()/remove_mem() juga synchronized pada guild ini,
        // jadi tidak akan ada ConcurrentModificationException saat loop berjalan.
        List<ClanMember> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(members);
        }

        for (ClanMember member : snapshot) {
            if (member == null || member.name == null) {
                continue;
            }
            // try/catch PER MEMBER: error pada satu member tidak boleh menghentikan
            // pemberian reward ke member-member berikutnya.
            try {
                Player p = Manager.getPlayerByName(member.name);
                if (p == null) {
                    continue;
                }

                p.updateGem(gem);
                p.updateGold(gold);

                try {
                    Service.Show_open_box_notice_item(p, "Kamu mendapatkan",
                            new short[]{-2, -1},
                            new int[]{gem, gold},
                            new short[]{4, 4});
                } catch (Exception e) {
                    // Gagal kirim notif (mis. charInventory error karena isi tas member)
                    // tidak boleh membatalkan reward member lain.
                    System.out.println("[Tambang] Notif reward gagal untuk " + member.name + ": " + e);
                    e.printStackTrace();
                }

                System.out.println("Sending Guild rewards to " + p.name);
            } catch (Exception e) {
                System.out.println("[Tambang] Reward gagal untuk " + member.name + " (guild " + name + "): " + e);
                e.printStackTrace();
            }
        }

    }

    public void sendNotice(Player mem) throws IOException {
        for (ClanMember member : members) {
            Player p = Manager.getPlayerByName(member.name.toLowerCase());
            if (p == null) {
                continue;
            }

            String message = mem.name + " telah login kedalam game";

            Message m = new Message(34);
            m.writer().writeUTF("Guild");
            if (p.name.equalsIgnoreCase(mem.name)) {
                m.writer().writeUTF("Pengumuman: " + notice);

            } else {
                m.writer().writeUTF("System: " + message);
            }
            p.conn.addmsg(m);
            m.cleanup();
        }

    }

    public void clan_process(Session conn, Message m2, int type) throws IOException {
        log.debug("ClanRequest: {}", type);
        switch (type) {
            case 21: {
                this.open_box_clan(conn);
                break;
            }
            case 4: {
                if (!conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                    Service.send_notice_box(conn, "You are not the leader!");
                    return;
                }
                byte mem_type = m2.reader().readByte();
                String name_mem = m2.reader().readUTF();
                for (int i = 0; i < conn.p.myclan.members.size(); i++) {
                    if (conn.p.myclan.members.get(i).name.equals(name_mem)) {
                        conn.p.myclan.members.get(i).memberType = mem_type;
                        break;
                    }
                }
                String name_mem_type = "";
                switch (mem_type) {
                    case 126: {
                        name_mem_type += "Wakil Pemimpin";
                        break;
                    }
                    case 125: {
                        name_mem_type += "Ksatria Agung";
                        break;
                    }
                    case 124: {
                        name_mem_type += "Ksatria Mulia";
                        break;
                    }
                    case 123: {
                        name_mem_type += "Ksatria Kehormatan";
                        break;
                    }
                    case 122: {
                        name_mem_type += "Anggota Baru";
                        break;
                    }
                }

                Service.send_notice_box(conn, "Penunjukan berhasil " + name_mem + " menjadi " + name_mem_type);
                Player p0 = GameMap.get_player_by_name(name_mem);
                if (p0 != null) {
                    Service.send_notice_box(p0.conn, "Kamu telah ditunjuk sebagai " + name_mem_type);
                    MapService.broadcastMainCharInfo(conn.p.map, p0);
                    MapService.send_in4_other_char(p0.map, p0, p0);
                    Service.sendMainCharInfo(p0);
                }
                this.updateListMem(conn, name_mem, mem_type);
                break;
            }
            case 18: {
                if (!conn.p.myclan.members.get(0).name.equals(conn.p.name)) {
                    Service.send_notice_box(conn, "Kamu bukan pemimpin!");
                    return;
                }
                String name = m2.reader().readUTF();
                this.remove_mem(name);
                Service.send_notice_box(conn, "Berhasil mengeluarkan " + name + " dari klan!");
                Player p0 = GameMap.get_player_by_name(name);
                if (p0 != null) {
                    Service.send_notice_box(p0.conn, "Kamu dikeluarkan dari klan karena terlalu lemah!");
                    p0.myclan = null;
                    MapService.broadcastMainCharInfo(conn.p.map, p0);
                    MapService.send_in4_other_char(p0.map, p0, p0);
                    Service.sendMainCharInfo(p0);
                }
                this.updateListMem(conn, name, 121);
                break;
            }
            case 13: {
                send_list_mem(conn);
                break;
            }
            case 10: {
                if (this.members.size() >= this.maxMember) {
                    Service.send_notice_box(conn, "Jumlah anggota sudah penuh!");
                } else {
                    Player p0 = GameMap.get_player_by_name(m2.reader().readUTF());
                    if (p0 != null) {

                        if (p0.myclan != null) {
                            if (p0.myclan.name.equals(conn.p.myclan.name)) {
                                Service.send_notice_box(conn, "Pemain tersebut sudah menjadi anggota guild!");
                            } else {
                                Service.send_notice_box(conn, "Pemain tersebut adalah anggota guild lain!");
                            }
                            return;
                        }
                        Message m = new Message(69);
                        m.writer().writeByte(10);
                        m.writer().writeUTF(conn.p.name);
                        p0.conn.addmsg(m);
                        m.cleanup();
                    }
                }
                break;
            }

            case 6: {
                long value = m2.reader().readInt();
                if (value < 0 || value > 2_000_000_000 || ((value + this.gold) > 2_000_000_000L)
                        || value > conn.p.getGold()) {
                    Service.send_notice_box(conn, "Jumlah yang dimasukkan tidak valid");
                    return;
                }
                this.contributeGold(conn, value);
                break;
            }
            case 7: {
                long value = m2.reader().readInt();
                if (value < 0 || value > 2_000_000_000L || ((value + this.gems) > 2_000_000_000L)
                        || value > conn.p.getGem()) {
                    Service.send_notice_box(conn, "Jumlah yang dimasukkan tidak valid");
                    return;
                }
                this.contributeGem(conn, value);
                break;
            }
            case 14: {
                String name = m2.reader().readUTF();
                ClanMember p0 = null;
                for (int i = 0; i < members.size(); i++) {
                    ClanMember mem = members.get(i);
                    if (mem.name.equals(name)) {
                        p0 = mem;
                        break;
                    }
                }
                if (p0 != null) {
                    Message m = new Message(69);
                    m.writer().writeByte(14);
                    m.writer().writeUTF(p0.name);
                    m.writer().writeShort(p0.level);
                    m.writer().writeByte(p0.memberType);
                    m.writer().writeLong(this.get_mem_contribution_vang(p0.name));
                    m.writer().writeInt(this.get_mem_contribution_ngoc(p0.name));
                    conn.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_notice_box(conn, "Terjadi kesalahan!");
                }
                break;
            }
            case 2: {
                this.notice = m2.reader().readUTF();
                this.update_in4_clan_box_notice(conn, 2);
                Service.send_notice_box(conn, "Berhasil mengubah pengumuman");
                break;
            }
            case 16: {
                this.slogan = m2.reader().readUTF();
                this.update_in4_clan_box_notice(conn, 16);
                Service.send_notice_box(conn, "Berhasil mengubah slogan");
                break;
            }
            case 17: {
                this.rule = m2.reader().readUTF();
                this.update_in4_clan_box_notice(conn, 17);
                Service.send_notice_box(conn, "Berhasil mengubah peraturan");
                break;
            }

            case 15: {

                Message m = new Message(69);
                m.writer().writeByte(15);
                if (isLeader(conn.p.name)) {
                    m.writer().writeByte(0);
                } else {
                    m.writer().writeByte(1);
                }
                m.writer().writeByte(0);
                m.writer().writeInt(Guild.entrys.indexOf(this));
                m.writer().writeShort(this.icon);
                m.writer().writeUTF(this.shortName);
                m.writer().writeUTF(this.name);
                m.writer().writeShort(this.level);
                m.writer().writeShort(this.get_percent_level());
                if (BXH.BXH___GUILD.contains(this)) {
                    m.writer().writeShort((BXH.BXH___GUILD.indexOf(this) + 1)); // index bxh
                } else {
                    m.writer().writeShort(9999); // index bxh
                }
                m.writer().writeShort(this.members.size()); // mem
                m.writer().writeShort(this.maxMember); // max mem
                m.writer().writeUTF(this.members.get(0).name);
                m.writer().writeUTF(this.getSlogan()); // slogan
                m.writer().writeUTF(this.getRule()); // noi quy
                m.writer().writeLong(this.gold);
                m.writer().writeInt(this.gems);
                m.writer().writeByte(0); // thanh tich
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            default: { // type 8
                Service.send_notice_box(conn, "Terjadi kesalahan!");

                break;
            }
        }
    }

    public synchronized void remove_mem(String name) {
        ClanMember mem = null;
        for (int i = 1; i < this.members.size(); i++) {
            if (this.members.get(i).name.equals(name)) {
                mem = this.members.get(i);
            }
        }
        if (mem != null) {
            this.members.remove(mem);
        }
    }

    public void contributeGem(Session conn, long value) throws IOException {
        conn.p.updateGem(-value);
        Log.gI().add_log(conn.p.name, "Menyumbang " + Util.number_format(value) + " permata ke klan " + this.name);
        conn.p.item.charInventory(5);
        this.gems += value;
        this.update_in4_clan_box_notice(conn, 7);
        this.update_contribution_ngoc(conn.p.name, (int) value);
        Service.send_notice_box(conn, "Berhasil menyumbang " + Util.number_format(value) + " permata");
    }

    public void contributeGold(Session conn, long value) throws IOException {
        conn.p.updateGold(-value);
        Log.gI().add_log(conn.p.name, "Menyumbang " + Util.number_format(value) + " emas ke klan " + this.name);
        conn.p.item.charInventory(5);
        this.gold += value;
        this.update_in4_clan_box_notice(conn, 6);
        this.update_contribution_vang(conn.p.name, (int) value);
        Service.send_notice_box(conn, "Berhasil menyumbang " + Util.number_format(value) + " emas");
    }

    private void updateListMem(Session conn, String name_mem, int mem_type) throws IOException {
        Message m = new Message(69);
        m.writer().writeByte(19);
        m.writer().writeShort(32000);
        m.writer().writeUTF(name_mem);
        m.writer().writeInt(Guild.entrys.indexOf(this));
        m.writer().writeUTF(this.name);
        m.writer().writeUTF(this.shortName);
        m.writer().writeShort(this.icon);
        m.writer().writeByte(mem_type);
        conn.addmsg(m);
        m.cleanup();
    }

    private synchronized void send_list_mem(Session conn) throws IOException {
        Message m = new Message(56);
        m.writer().writeByte(4);
        m.writer().writeUTF(this.name);
        m.writer().writeByte(99);
        m.writer().writeInt(0);
        m.writer().writeByte(this.members.size());
        for (int i = 0; i < this.members.size(); i++) {
            ClanMember mem = this.members.get(i);
            Player p0 = GameMap.get_player_by_name(mem.name);
            if (p0 != null) {
                mem.head = p0.head;
                mem.eye = p0.eye;
                mem.hair = p0.hair;
                mem.level = p0.level;
                mem.head = p0.head;
                mem.wearing.clear();
                for (int i1 = 0; i1 < p0.item.wear.length; i1++) {
                    Item3 it = p0.item.wear[i1];
                    if (it != null && (i1 == 0 || i1 == 1 || i1 == 6 || i1 == 7 || i1 == 10)) {
                        PlayerPart part = new PlayerPart();
                        part.type = it.type;
                        part.part = it.part;
                        mem.wearing.add(part);
                    }
                }
            }
            m.writer().writeUTF(mem.name);
            m.writer().writeByte(mem.head);
            m.writer().writeByte(mem.eye);
            m.writer().writeByte(mem.hair);
            m.writer().writeShort(mem.level);

            m.writer().writeByte(mem.wearing.size());
            for (PlayerPart it : mem.wearing) {
                m.writer().writeByte(it.part);
                m.writer().writeByte(it.type);
            }

            if (p0 != null) {
                m.writer().writeByte(1);
            } else {
                m.writer().writeByte(0);
            }
            switch (mem.memberType) {
                case 127: {
                    m.writer().writeUTF("Pemimpin");
                    break;
                }
                default: { // type 122
                    m.writer().writeUTF("Anggota Baru");
                    break;
                }
            }

            m.writer().writeShort(this.icon);
            m.writer().writeUTF(this.shortName);
            m.writer().writeByte(mem.memberType);
        }
        conn.addmsg(m);
        m.cleanup();
    }

    private synchronized void update_in4_clan_box_notice(Session conn, int type) throws IOException {
        switch (type) {
            case 6:
            case 7: {
                Message m = new Message(69);
                m.writer().writeByte(15);
                m.writer().writeByte(0);
                m.writer().writeByte(1);
                m.writer().writeLong(this.gold);
                m.writer().writeInt(this.gems);
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 2:
            case 17: {
                Message m = new Message(69);
                m.writer().writeByte(15);
                m.writer().writeByte(0);
                m.writer().writeByte(2);
                m.writer().writeUTF(this.getRule());
                conn.addmsg(m);
                m.cleanup();
                break;
            }
            case 16: {
                Message m = new Message(69);
                m.writer().writeByte(15);
                m.writer().writeByte(0);
                m.writer().writeByte(3);
                m.writer().writeUTF(this.getSlogan());
                conn.addmsg(m);
                m.cleanup();
                break;
            }
        }
    }

    private synchronized void update_contribution_ngoc(String name, int quant) {
        for (int i = 0; i < members.size(); i++) {
            ClanMember temp = members.get(i);
            if (temp.name.equals(name)) {
                temp.gem += quant;
            }
        }
    }

    private synchronized void update_contribution_vang(String name, int quant) {
        for (int i = 0; i < members.size(); i++) {
            ClanMember temp = members.get(i);
            if (temp.name.equals(name)) {
                temp.gold += quant;
            }
        }
    }

    private synchronized int get_mem_contribution_ngoc(String name) {
        for (int i = 0; i < members.size(); i++) {
            ClanMember temp = members.get(i);
            if (temp.name.equals(name)) {
                return temp.gem;
            }
        }
        return 0;
    }

    private synchronized long get_mem_contribution_vang(String name) {
        for (int i = 0; i < members.size(); i++) {
            ClanMember temp = members.get(i);
            if (temp.name.equals(name)) {
                return temp.gold;
            }
        }
        return 0;
    }

    public String getRule() {
        String text = "";
        if (this.notice.equals("")) {
            if (this.rule.equals("")) {
                return "";
            }
            return ("@Peraturan: " + this.rule);
        } else {
            if (this.rule.equals("")) {
                text += "\n";
            } else {
                text += "@Peraturan: " + this.rule;
                text += "\n";
            }
            text += "@Pengumuman: " + this.notice;
        }
        return text;
    }


    private synchronized String getSlogan() {
        if (this.slogan.equals("")) {
            return "";
        }
        return ("@Slogan: " + this.slogan);
    }

    public int get_percent_level() {
        return (int) ((exp * 1000) / Level.entrys.get(level - 1).exp);
    }

    public synchronized static boolean create_clan(Session conn, String name, String name_shorted) throws IOException {
        for (Guild guild : entrys) {
            if (guild.name.equals(name)) {
                Service.send_notice_box(conn, "Nama ini sudah ada, silakan pilih nama lain!");
                return false;
            }
            if (guild.shortName.equals(name_shorted)) {
                Service.send_notice_box(conn, "Singkatan nama ini sudah ada, silakan pilih yang lain!");
                return false;
            }
        }

        Guild temp = new Guild();
        temp.members = new ArrayList<>();
        //
        ClanMember temp_mem = new ClanMember();
        temp_mem.name = conn.p.name;
        temp_mem.memberType = 127; // thu linh
        temp_mem.gem = 0;
        temp_mem.gold = 0;
        temp_mem.head = conn.p.head;
        temp_mem.eye = conn.p.eye;
        temp_mem.hair = conn.p.hair;
        temp_mem.level = conn.p.level;
        temp_mem.wearing = new ArrayList<>();
        temp.crystals = new ArrayList<>();
        for (int i = 0; i < conn.p.item.wear.length; i++) {
            // FIX: sertakan slot 22 (amulet/cincin) supaya ikut tampil.
            if (conn.p.item.wear[i] == null || (i != 0 && i != 1 && i != 6 && i != 7 && i != 10 && i != 22)) {
                continue;
            }
            PlayerPart temp2 = new PlayerPart();
            temp2.type = conn.p.item.wear[i].type;
            temp2.part = conn.p.item.wear[i].part;
            temp_mem.wearing.add(temp2);
        }
        //
        temp.members.add(temp_mem);
        temp.name = name;
        temp.shortName = name_shorted;
        temp.icon = 0;
        temp.level = 1;
        temp.exp = 0;
        temp.slogan = "";
        temp.rule = "";
        temp.notice = "";
        temp.setGold(0);
        temp.setGems(0);
        temp.maxMember = 5;
        temp.clanItems = new ArrayList<>();
        //
        // short[] list_it = new short[] {275, 279, 281, 294, 296, 299, 301};
        // //
        // for (int i = 0; i < list_it.length; i++) {
        // Item47 it = new Item47();
        // it.id = list_it[i];
        // it.quantity = 1;
        // temp.item_clan.add(it);
        // }
        //
        entrys.add(temp);
        conn.p.myclan = temp;
        String query
                = "INSERT INTO `clan` (`name`, `name_short`, `mems`, `item`, `level`, `exp`, `slogan`, `rule`, `notice`, `vang`, `kimcuong`, `max_mem`, `icon`) VALUES ('"
                + name + "', '" + name_shorted + "', '" + Guild.flush_mem_json(temp.members) + "', '"
                + Guild.flush_item_json(temp.clanItems) + "', " + temp.level + ", " + temp.exp + ", '" + temp.slogan
                + "', '" + temp.rule + "', '" + temp.notice + "', " + temp.gold + ", " + temp.gems + ", "
                + temp.maxMember + ", " + temp.icon + ")";
        try (Connection connection = SQL.gI().getConnection(); Statement statement = connection.createStatement();) {
            if (statement.executeUpdate(query) > 0) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        MapService.broadcastMainCharInfo(conn.p.map, conn.p);
        Service.sendMainCharInfo(conn.p);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static String flush_item_json(List<Item47> item) {
        JSONArray js = new JSONArray();
        for (Item47 temp : item) {
            JSONArray js2 = new JSONArray();
            js2.add(temp.id);
            js2.add(temp.quantity);
            js.add(js2);
        }
        return js.toJSONString();
    }

    @SuppressWarnings("unchecked")
    public synchronized static String flush_mem_json(List<ClanMember> mems2) {
        JSONArray js = new JSONArray();
        for (ClanMember temp : mems2) {
            JSONArray js2 = new JSONArray();
            js2.add(temp.name);
            js2.add(temp.memberType);
            js2.add(temp.gem);
            js2.add(temp.gold);
            js2.add(temp.head);
            js2.add(temp.eye);
            js2.add(temp.hair);
            js2.add(temp.level);
            JSONArray js3 = new JSONArray();
            for (PlayerPart part : temp.wearing) {
                JSONArray js4 = new JSONArray();
                js4.add(part.part);
                js4.add(part.type);
                js3.add(js4);
            }
            js2.add(js3);
            js.add(js2);
        }
        return js.toJSONString();
    }

    public synchronized static Guild getPlayerGuild(String name) {
        for (Guild temp : entrys) {
            for (int j = 0; j < temp.members.size(); j++) {
                ClanMember temp2 = temp.members.get(j);
                if (temp2.name.equals(name)) {
                    return temp;
                }
            }
        }
        return null;
    }

    public synchronized byte get_mem_type(String name) {
        for (int i = 0; i < members.size(); i++) {
            ClanMember temp = members.get(i);
            if (temp.name.equals(name)) {
                return temp.memberType;
            }
        }
        return 121;
    }

    public synchronized static int get_id_clan(Guild myclan) {
        return entrys.indexOf(myclan);
    }

    public synchronized static void set_clan(List<Guild> guild_list) {
        Guild.entrys.addAll(guild_list);
    }
    // public long getVang() {
    // return vang;
    // }

    public synchronized void setGold(long gold) {
        this.gold = gold;
    }
    // public int getKimcuong() {
    // return kimcuong;
    // }

    public synchronized void setGems(int gems) {
        this.gems = gems;
    }

    public synchronized static void flush() {
        List<Guild> list_to_remove = new ArrayList<>();
        String query
                = "UPDATE `clan` SET `level` = ?, `exp` = ?, `slogan` = ?, `rule` = ?, `mems` = ?, `item` = ?, `notice` = ?, `vang` = ?, `kimcuong` = ?, `icon` = ?, `max_mem` = ? WHERE `name` = ?;";
        try {
            Connection conn = SQL.gI().getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            for (int i = 0; i < Guild.entrys.size(); i++) {
                Guild guild = Guild.entrys.get(i);
                if (guild.members.size() < 1) {
                    list_to_remove.add(guild);
                    Guild.entrys.remove(guild);
                    i--;
                } else {
                    ps.clearParameters();
                    ps.setInt(1, guild.level);
                    ps.setLong(2, guild.exp);
                    ps.setNString(3, guild.slogan);
                    ps.setNString(4, guild.rule);
                    ps.setNString(5, Guild.flush_mem_json(guild.members));
                    ps.setNString(6, Guild.flush_item_json(guild.clanItems));
                    ps.setNString(7, guild.notice);
                    ps.setLong(8, guild.gold);
                    ps.setInt(9, guild.gems);
                    ps.setInt(10, guild.icon);
                    ps.setInt(11, guild.maxMember);
                    ps.setNString(12, guild.name);
                    ps.addBatch();
                    if (i % 50 == 0) {
                        ps.executeBatch();
                    }
                }
            }
            ps.executeBatch();
            conn.commit();
            //
            ps.close();
            ps = conn.prepareStatement("DELETE FROM `clan` WHERE `name` = ?;");
            for (int i = 0; i < list_to_remove.size(); i++) {
                Guild guild = list_to_remove.get(i);
                ps.clearParameters();
                ps.setNString(1, guild.name);
                ps.addBatch();
                if (i % 50 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
            //
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void accept_mem(Session conn, Player p0) throws IOException {
        for (int i = 0; i < p0.myclan.members.size(); i++) {
            if (p0.myclan.members.get(i).name.equals(conn.p.name)) {
                Service.send_notice_box(conn, "Kamu sudah bergabung dalam klan!");
                return;
            }
        }
        if (p0.myclan.members.size() >= p0.myclan.maxMember) {
            Service.send_notice_box(conn, "Jumlah anggota sudah penuh!");


        } else {
            Guild temp = new Guild();
            temp.members = new ArrayList<>();
            //
            ClanMember temp_mem = new ClanMember();
            temp_mem.name = conn.p.name;
            temp_mem.memberType = 122;
            temp_mem.gem = 0;
            temp_mem.gold = 0;
            temp_mem.head = conn.p.head;
            temp_mem.eye = conn.p.eye;
            temp_mem.hair = conn.p.hair;
            temp_mem.level = conn.p.level;
            temp_mem.wearing = new ArrayList<>();
            for (int i = 0; i < conn.p.item.wear.length; i++) {
                // FIX: sertakan slot 22 (amulet/cincin) supaya ikut tampil.
                if (conn.p.item.wear[i] == null || (i != 0 && i != 1 && i != 6 && i != 7 && i != 10 && i != 22)) {
                    continue;
                }
                PlayerPart temp2 = new PlayerPart();
                temp2.type = conn.p.item.wear[i].type;
                temp2.part = conn.p.item.wear[i].part;
                temp_mem.wearing.add(temp2);
            }
            p0.myclan.members.add(temp_mem);
            //
            conn.p.myclan = p0.myclan;
            MapService.broadcastMainCharInfo(conn.p.map, conn.p);
            Service.sendMainCharInfo(conn.p);
            Service.send_notice_box(conn, ("Berhasil bergabung dengan guild " + p0.myclan.name));
            Service.send_notice_box(p0.conn, (conn.p.name + " telah bergabung dengan guildmu"));

        }
    }

    public void open_box_clan(Session conn) throws IOException {
        Message m = new Message(69);
        m.writer().writeByte(21);
        m.writer().writeByte(3);
        m.writer().writeShort(this.clanItems.size());
        for (Item47 it : this.clanItems) {
            m.writer().writeShort(it.id);
            m.writer().writeShort(it.quantity);
        }
        conn.addmsg(m);
        m.cleanup();
    }

    public synchronized void updateExp(int exp) {
        this.exp += exp;
        if (this.exp > Level.entrys.get(this.level).exp) {
            this.exp = Level.entrys.get(this.level).exp;
        }
    }

    public synchronized long get_vang() {
        return this.gold;
    }

    public synchronized int get_ngoc() {
        return this.gems;
    }

    public synchronized void updateGold(long quant) {
        this.gold += quant;
    }

    public synchronized void updateGem(int quant) {
        this.gems += quant;
    }

    public boolean check_id(short id) {
        for (Item47 it : this.clanItems) {
            if (it.id == id && it.quantity > 0) {
                return true;
            }
        }
        return false;
    }

    public synchronized void remove_all_mem() throws IOException {
        while (this.members.size() > 1) {
            ClanMember mem = this.members.get(1);
            this.members.remove(mem);
            Player p0 = GameMap.get_player_by_name(mem.name);
            if (p0 != null) {
                p0.myclan = null;
                MapService.broadcastMainCharInfo(p0.map, p0);
                MapService.send_in4_other_char(p0.map, p0, p0);
                Service.sendMainCharInfo(p0);
                Service.send_notice_box(p0.conn, "Guild telah dibubarkan!");

            }
        }
        this.members.clear();
    }

    public static List<Guild> get_all_clan() {
        return Guild.entrys;
    }

    // ---------------------------------------------------------------
    // Cari guild dengan gold terbanyak saat ini (buat notice login)
    // ---------------------------------------------------------------
    public static Guild getTopGuildByGold() {
        Guild top = null;
        long max = 0;
        for (Guild g : Guild.entrys) {
            if (g.gold > max) {
                max = g.gold;
                top = g;
            }
        }
        return top;
    }

    public static int get_mem_by_level(short level) {
        int quant = (level / 5) * 5;
        quant += 5;
        return (quant < 45) ? quant : 45;
    }

    public synchronized Crystal get_mo_tai_nguyen(int n2) {
        for (int j = 0; j < this.crystals.size(); j++) {
            if (this.crystals.get(j).objectId == n2) {
                return this.crystals.get(j);
            }
        }
        return null;
    }

    public boolean isLeader(String name) {
        return members.get(0).name.equalsIgnoreCase(name);
    }
}