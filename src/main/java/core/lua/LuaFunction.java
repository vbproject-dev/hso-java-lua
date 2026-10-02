package core.lua;
public final class LuaFunction {
    private final long reference;

    public LuaFunction(long reference) {
        this.reference = reference;
    }

    public long getReference() {
        return reference;
    }
}