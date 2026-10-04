package  core.lua;


public final class NativeLua {

    static {
        System.loadLibrary("HSOPlugin");
    }
    public static native boolean initialize();

    public static native boolean execute(String code);

    public static native boolean executeFile(String path);

    public static native boolean load(String path);

    public static native void update(float dt);

    public static native boolean isLoaded();
    public static native void destroy();

    private NativeLua() {
    }
}