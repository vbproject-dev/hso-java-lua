package game.map;

import java.io.IOException;
import client.Player;
import client.io.Message;

public class NpcChat {

    public static String CHAT_MR_BALLARD = "Chiến trường bắt đầu vào 21h30 phút hàng ngày";
    public static String CHAT_TOP = "Cám ơn các bạn đã like cho mình,hihi!";
    public static String CHAT_PHO_CHI_HUY = "Và cứ thế mỗi ngày thêm một tý\n"
            + "Ta yêu nhau cho mãi tới muôn đời.";
    public static String CHAT_PHAP_SU = "Ở đâu trăng có nhớ người\n"
            + "Ở đây đang có một người nhớ trăng..";
    public static String CHAT_ZORO = "Nắng mưa là chuyện của trời\n"
            + "Tương tư là chuyện của tôi yêu nàng.";
    public static String CHAT_AMAN = "Ôi hạnh phúc anh thấy mình nhỏ bé\n"
            + "Chép tình yêu trong trang giấy thơ ngây.";
    public static String CHAT_ODA = "Nửa sự thật không còn là sự thật\n"
            + "Và tình yêu không một nửa bao giờ...";
    public static String CHAT_LISA = "Với em dù lắm chua cay\n"
            + "Tình yêu trước gió càng lay càng bền.";
    public static String CHAT_SOPHIA = "Em là sóng nhưng xin đừng như sóng\n"
            + "Dội vào bờ xin chớ ngược ra khơi.";
    public static String CHAT_HAMMER = "Từ nay tôi đã có người\n"
            + "Có em đi đứng bên đời líu lo.";
    public static String CHAT_ZULU = "Chỉ mình em thôi và chỉ một\n"
            + "Mình em ngự trị trái tim anh.";
    public static String CHAT_DOUBA = "Nếu đã hẹn biển cứ nằm im nhé!\n"
            + "Sóng ra khơi rồi sóng lại quay về.";
    public static String CHAT_ANNA = "Khi say một chén cũng say\n"
            + "Khi nên tình nghĩa một ngày cũng nên.";

    /**
     * Cho NPC (id) noi chat voi tat ca nguoi choi trong map.
     */
    public static void chat(GameMap map, String txt, int id) throws IOException {
        if (map == null || txt == null) {
            return;
        }
        Message m = new Message(23);
        try {
            m.writer().writeUTF(txt);
            m.writer().writeByte(id);
            for (int j = 0; j < map.players.size(); j++) {
                Player p0 = map.players.get(j);
                if (p0 != null && p0.conn != null && p0.map != null && p0.map.equals(map)) {
                    p0.conn.addmsg(m);
                }
            }
        } finally {
            m.cleanup();
        }
    }
}