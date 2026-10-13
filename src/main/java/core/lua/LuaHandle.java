package core.lua;


import java.lang.ref.Cleaner;

abstract class LuaHandle {
    private static final Cleaner CLEANER = Cleaner.create();

    private final long reference;

    protected LuaHandle(long reference) {
        this.reference = reference;
        CLEANER.register(this, new Release(reference));
    }

    public final long getReference() {
        return reference;
    }

    // Static on purpose: capturing `this` would keep the handle reachable forever.
    private record Release(long reference) implements Runnable {
        @Override
        public void run() {
            NativeLua.unref(reference);
        }
    }
}