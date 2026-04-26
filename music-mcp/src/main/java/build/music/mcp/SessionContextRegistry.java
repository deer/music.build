package build.music.mcp;

import build.serve.mcp.McpServer;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-session {@link CompositionContextProvider} that keys contexts on the MCP session ID.
 *
 * <p>Reads {@link McpServer#SESSION_ID} from the current ScopedValue binding on each {@link #get()} call.
 * Sessions idle for more than {@value #IDLE_EVICT_MINUTES} minutes are evicted by a background daemon thread.
 *
 * <p>Use this in hosted deployments ({@code mcp.music.build}) where multiple agents connect concurrently.
 * The local single-user server ({build.music.server.MusicMcpServer}) uses the simpler
 * {@code () -> singletonContext} provider and does not need this class.
 */
public final class SessionContextRegistry implements CompositionContextProvider {

    static final int IDLE_EVICT_MINUTES = 30;
    private static final long IDLE_EVICT_MS = IDLE_EVICT_MINUTES * 60_000L;
    private static final long SWEEP_INTERVAL_MS = 10 * 60_000L;

    private record Entry(CompositionContext context, AtomicLong lastAccessMs) {
        static Entry create() {
            return new Entry(new CompositionContext(), new AtomicLong(System.currentTimeMillis()));
        }
    }

    private final ConcurrentHashMap<String, Entry> contexts = new ConcurrentHashMap<>();

    public SessionContextRegistry() {
        final Thread sweeper = new Thread(this::sweep, "session-eviction");
        sweeper.setDaemon(true);
        sweeper.start();
    }

    @Override
    public CompositionContext get() {
        final String key = McpServer.SESSION_ID.orElse("local");
        final Entry entry = contexts.computeIfAbsent(key, id -> Entry.create());
        entry.lastAccessMs().set(System.currentTimeMillis());
        return entry.context();
    }

    /**
     * Returns the context for a specific session ID without reading the ScopedValue.
     * Use this in {@link build.serve.mcp.McpServer#toolCallEvents()} subscribers, which run
     * after the ScopedValue scope has closed.
     * Returns an empty Optional if no context exists for the given session.
     */
    public Optional<CompositionContext> getForSession(final String sessionId) {
        final Entry entry = contexts.get(sessionId);
        return entry != null ? Optional.of(entry.context()) : Optional.empty();
    }

    /**
     * Returns the number of active sessions currently held in memory.
     */
    public int sessionCount() {
        return contexts.size();
    }

    private void sweep() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(SWEEP_INTERVAL_MS);
                final long cutoff = System.currentTimeMillis() - IDLE_EVICT_MS;
                contexts.entrySet().removeIf(e -> e.getValue().lastAccessMs().get() < cutoff);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
