package core.lua;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class LuaTest {

    public static void main(String[] args) throws IOException {
        if (!NativeLua.initialize()) {
            log.info("Native Lua failed to load!");
            return;
        }

        NativeLua.load("data/scripts/Main.lua");

//        var msg = new Message(20);
//        msg.writer().writeUTF("HI FROM JAVA");
//        Message response = JavaToLua.call("Some.test", new Object[]{new Message((byte) 10, msg.getData())});
//        if (response != null) {
//            System.out.println(response.reader().readUTF());
//        }
    }
}
