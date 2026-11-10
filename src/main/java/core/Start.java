package core;


import core.lua.LuaLoop;
import core.lua.NativeLua;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Start {

    public static void main(String[] args) {

        LuaLoop gameLoop = new LuaLoop(20);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (SQL.is_connected) {

                SQL.gI().close();
                gameLoop.stop();
                NativeLua.destroy();

            }
        }));

        ServerManager.gI().init();

        if (Manager.gI().useLua) {
            if (!NativeLua.initialize()) {
                log.info("Native Lua failed to load!");
                return;
            }

            NativeLua.load("data/scripts/Main.lua");

            gameLoop.start();
        }

    }
}








