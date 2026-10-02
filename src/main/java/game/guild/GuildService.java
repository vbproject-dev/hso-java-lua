package game.guild;

import client.Player;
import client.io.Message;
import client.io.Session;

import core.BXH;
import core.Service;
import game.map.GameMap;
import lombok.extern.slf4j.Slf4j;
import template.ClanMember;
import template.Item3;
import template.PlayerPart;

import java.io.IOException;

@Slf4j
public class GuildService {
    private GuildService() {
    }

    private static class Holder {
        private static final GuildService INSTANCE = new GuildService();
    }

    public static GuildService getInstance() {
        return Holder.INSTANCE;
    }

    public static Guild getGuildByIndex(int id) {
        for (Guild guild : Guild.entrys) {
            int index = Guild.entrys.indexOf(guild);
            if (index == id) return guild;
        }

        return null;
    }

    public void onGuildMessage(Session s, byte type, Message m) throws IOException {

        switch (type) {
            case 13 -> {
                int guildId = m.reader().readInt();
                Guild guild = GuildService.getGuildByIndex(guildId);
                if (guild != null) {
                    sendMemberList(s, guild);
                }
            }
            case 15 -> {
                int guildId = m.reader().readInt();
                Guild guild = GuildService.getGuildByIndex(guildId);
                if (guild != null) {
                    sendGuildInfo(s, guild);
                }
            }
        }
    }

    private void sendMemberList(Session s, Guild guild) throws IOException {
        Message m = new Message(56);
        m.writer().writeByte(4);
        m.writer().writeUTF(guild.name);
        m.writer().writeByte(99);
        m.writer().writeInt(0);
        m.writer().writeByte(guild.members.size());
        for (int i = 0; i < guild.members.size(); i++) {
            ClanMember mem = guild.members.get(i);
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

            m.writer().writeShort(guild.icon);
            m.writer().writeUTF(guild.shortName);
            m.writer().writeByte(mem.memberType);
        }
        s.addmsg(m);
        m.cleanup();
    }

    public void sendGuildInfo(Session s, Guild guild) throws IOException {
        Message m = new Message(69);
        m.writer().writeByte(15);

        if (guild.isLeader(s.p.name)) {
            m.writer().writeByte(0);
        } else {
            m.writer().writeByte(1);
        }
        m.writer().writeByte(0);
        m.writer().writeInt(Guild.entrys.indexOf(guild));
        m.writer().writeShort(guild.icon);
        m.writer().writeUTF(guild.shortName);
        m.writer().writeUTF(guild.name);
        m.writer().writeShort(guild.level);
        m.writer().writeShort(guild.get_percent_level());
        if (BXH.BXH___GUILD.contains(guild)) {
            m.writer().writeShort((BXH.BXH___GUILD.indexOf(guild) + 1)); // index bxh
        } else {
            m.writer().writeShort(9999); // index bxh
        }
        m.writer().writeShort(guild.members.size()); // mem
        m.writer().writeShort(guild.maxMember); // max mem
        m.writer().writeUTF(guild.members.get(0).name);
        m.writer().writeUTF(guild.slogan); // slogan
        m.writer().writeUTF(guild.getRule()); // noi quy
        m.writer().writeLong(guild.gold);
        m.writer().writeInt(guild.gems);
        m.writer().writeByte(0); // thanh tich
        s.addmsg(m);
        m.cleanup();
    }
}
