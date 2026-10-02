package game.map;

import java.io.IOException;
import client.io.Message;
import template.MainObject;

/**
 * Efek "kena hit" buat item.wear[22] (samping/cincin) — nempel & ngikutin
 * TARGET yang ditentukan (bisa player atau mob), muncul sebentar tiap kali
 * player yang pakai item ini terlibat combat (nyerang musuh / kena serang musuh).
 *
 * Protokol dikonfirmasi dari decompile client (ksatria_3_0_9_java):
 * - opcode -49, subOpcode 2 (handler eq.af() di client -> case 2)
 * - graphicId (unsigned byte 0-255) di-request client sebagai part_char
 *   TYPE 112, id = graphicId (lihat class `at` di client: this.g = 112).
 * - Efek ini ditempel ke entity (objectId) lewat list internal client
 *   (ez.this.k), jadi otomatis ngikutin posisi si TARGET selama durasi aktif.
 * - Field entity-type (byte setelah objectId) dipakai client buat cari
 *   entity yang tepat: sama seperti convention yang sudah dipakai server di
 *   tempat lain (mis. MapService.add_eff_stun) yaitu target.getTypeObject()
 *   -> 0 = player, 1 = mob.
 */
public class Eff_hit_wear22 {

    /**
     * @param gameMap    map tempat target berada (buat broadcast ke semua yang lihat)
     * @param target     entity yang mau ditempeli efek (Player ATAU MobInMap)
     * @param graphicId  id part_char type 112 (0-255), contoh: 51 -> file
     *                   data/part_char/{variant}/img/112_51.png
     * @param durationMs berapa lama efek nempel (ms), misal 1500 buat ~1.5 detik
     */
    public static void send(GameMap gameMap, MainObject target, int graphicId, int durationMs) throws IOException {
        if (gameMap == null || target == null) {
            return;
        }
        Message m = new Message(-49);
        m.writer().writeByte(2);                 // subOpcode 2: attach effect to entity
        m.writer().writeShort(0);                // payloadLength = 0 (tidak ada embedded asset)
        m.writer().writeByte(0);                 // n7 (mode, tidak dipakai buat kasus ini)
        m.writer().writeByte(0);                 // by6 (tidak dipakai buat kasus ini)
        m.writer().writeByte((byte) graphicId);  // eff id -> part_char type 112, id = graphicId
        m.writer().writeShort(target.objectId);  // target entity
        m.writer().writeByte(target.getTypeObject()); // 0 = player, 1 = mob
        m.writer().writeByte(0);                 // jalur normal (bukan graphicId==17 / cn.f)
        m.writer().writeInt(durationMs);         // durasi tampil (ms), harus > 0 supaya timed
        MapService.sendMsgPlayerInside(gameMap, target, m, true);
        m.cleanup();
    }
}