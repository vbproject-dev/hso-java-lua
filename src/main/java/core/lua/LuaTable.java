package core.lua;

public final class LuaTable {
    private final long reference;

    public LuaTable(long reference) {
        this.reference = reference;
    }

    public long getReference() {
        return reference;
    }
}