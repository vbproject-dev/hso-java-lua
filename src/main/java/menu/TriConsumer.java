package menu;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A p, B yOrN, C args);
}
