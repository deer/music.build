package build.music.mcp;

import build.serve.mcp.McpServer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionContextRegistryTests {

    @Test
    void sameSessionIdReturnsSameContext() throws Exception {
        final var registry = new SessionContextRegistry();
        final CompositionContext[] results = new CompositionContext[2];

        ScopedValue.where(McpServer.SESSION_ID, "session-abc").call(() -> {
            results[0] = registry.get();
            results[1] = registry.get();
            return null;
        });

        assertSame(results[0], results[1]);
    }

    @Test
    void differentSessionIdsReturnDifferentContexts() throws Exception {
        final var registry = new SessionContextRegistry();

        final CompositionContext ctxA = ScopedValue.where(McpServer.SESSION_ID, "session-a")
            .call(registry::get);
        final CompositionContext ctxB = ScopedValue.where(McpServer.SESSION_ID, "session-b")
            .call(registry::get);

        assertNotSame(ctxA, ctxB);
    }

    @Test
    void unboundSessionIdFallsBackToLocal() throws Exception {
        final var registry = new SessionContextRegistry();

        final CompositionContext ctxUnbound = registry.get();
        final CompositionContext ctxLocal = ScopedValue.where(McpServer.SESSION_ID, "local")
            .call(registry::get);

        assertSame(ctxUnbound, ctxLocal);
    }

    @Test
    void getForSessionReturnsEmptyForUnknownSession() {
        final var registry = new SessionContextRegistry();

        final Optional<CompositionContext> result = registry.getForSession("no-such-session");

        assertTrue(result.isEmpty());
    }

    @Test
    void getForSessionReturnsContextAfterFirstAccess() throws Exception {
        final var registry = new SessionContextRegistry();

        final CompositionContext created = ScopedValue.where(McpServer.SESSION_ID, "session-xyz")
            .call(registry::get);
        final Optional<CompositionContext> found = registry.getForSession("session-xyz");

        assertTrue(found.isPresent());
        assertSame(created, found.get());
    }

    @Test
    void sessionCountReflectsActiveContexts() throws Exception {
        final var registry = new SessionContextRegistry();
        assertEquals(0, registry.sessionCount());

        ScopedValue.where(McpServer.SESSION_ID, "s1").call(registry::get);
        ScopedValue.where(McpServer.SESSION_ID, "s2").call(registry::get);

        assertEquals(2, registry.sessionCount());
    }
}
