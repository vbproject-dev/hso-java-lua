package client;

import java.io.IOException;
import java.util.regex.Pattern;
import core.Log;
import core.Service;
import client.io.Message;
import client.io.Session;
import game.guild.Guild;

public class TextFromClient_2 {

    public static void process(Session conn, Message m2) throws IOException {
        short type = m2.reader().readShort();
        byte size = m2.reader().readByte();
        String[] value = new String[size];
        for (int i = 0; i < size; i++) {
            value[i] = m2.reader().readUTF();
        }
        switch (type) {
            case 0: {
                if (!value[0].equals("") && !value[1].equals("")) {
                    // Service.send_notice_box(conn, "Anda telah menamai klan Anda \"" + value[0] + "\" dan nama singkatnya \""
                    // + value[1] + "\" kan?\n ini cuma untuk tes jadi belum ada klan kkk :v");
                    if (value[0].contains("_") || value[0].contains("-") || value[0].contains("@") || value[0].contains("#")
                            || value[0].contains("^") || value[0].contains("$") || value[0].length() > 20
                            || value[0].length() < 4) {
                        Service.send_notice_box(conn, "Nama yang dimasukkan tidak valid");
                        return;
                    }
                    Pattern p = Pattern.compile("^[a-zA-Z0-9]{3,3}$");
                    if (!p.matcher(value[1]).matches()) {
                        Service.send_notice_box(conn, "Nama singkatan yang dimasukkan tidak valid");
                        return;
                    }
                    if (conn.p.getGem() < 20000) {
                        Service.send_notice_box(conn, "Permata tidak cukup, butuh 20k!");
                        return;
                    }
                    if (Guild.create_clan(conn, value[0], value[1])) {
                        conn.p.updateGem(-20000);
                        Log.gI().add_log(conn.p.name, "Membuat guild menghabiskan 20000 permata");
                        conn.p.item.charInventory(5);
                        Service.send_box_UI(conn, 20);
                        Service.send_notice_box(conn, "Silakan pilih ikon apa pun untuk dijadikan simbol");
                    }
                } else {
                    Service.send_notice_box(conn, "Tidak bisa membuat guild jika ada kolom kosong, kan?");
                }
                break;
            }
        }
    }
}
