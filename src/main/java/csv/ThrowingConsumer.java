package csv;

/**
 * @author Omar Muhtaseb
 * @since 2018-12-30
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
}
