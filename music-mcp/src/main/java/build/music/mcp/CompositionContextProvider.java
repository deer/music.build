package build.music.mcp;

/**
 * Supplies a {@link CompositionContext} for the current request.
 *
 * <p>Local single-user mode: {@code () -> singletonContext}
 * <p>Hosted multi-user mode: {@code () -> contexts.computeIfAbsent(currentUserId(), k -> new CompositionContext())}
 */
@FunctionalInterface
public interface CompositionContextProvider {
    CompositionContext get();
}
