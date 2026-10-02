package core.lua;

public final class JavaToLua {
    private JavaToLua() {
    }

    public static native <T> T call(String function, Object[] args);

    public static native Object call(long reference, Object[] args);
}