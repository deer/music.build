package build.music.server;

import build.base.network.option.Port;
import build.music.mcp.CompositionContext;
import build.music.mcp.ExportOptions;
import build.music.mcp.MusicMcpTools;
import build.serve.application.Launcher;
import build.serve.application.ServerApplication;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.health.HealthHandler;
import build.serve.htmx.HtmxMiddleware;
import build.serve.mcp.McpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Runnable MCP server that exposes music.build composition tools.
 * Mounts all tools at /mcp on port 3000, console UI at /.
 */
public final class MusicMcpServer extends ServerApplication.Implementation {

    private final CompositionContext context = new CompositionContext();
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) {
        final String portEnv = System.getenv("PORT");
        final int port = portEnv != null ? Integer.parseInt(portEnv) : 3000;
        Launcher.launch(new MusicMcpServer(), Port.of(port));
    }

    @Override
    protected Router configure() {
        final McpServer.Builder mcpBuilder = McpServer.builder("music.build", "0.1.0");
        MusicMcpTools.registerAll(mcpBuilder, () -> context, ExportOptions.diskAndBytes());
        final McpServer mcp = mcpBuilder.build();

        final System.Logger log = System.getLogger(MusicMcpServer.class.getName());
        mcp.toolCallEvents().subscribe(event -> {
            if (event.error().isPresent()) {
                log.log(System.Logger.Level.WARNING,
                    "[tool] {0} failed in {1}ms: {2}",
                    event.toolName(), event.durationMs(), event.error().get().getMessage());
            } else {
                log.log(System.Logger.Level.INFO,
                    "[tool] {0} completed in {1}ms",
                    event.toolName(), event.durationMs());
            }
        });

        mcp.toolCallEvents().subscribe(event -> {
            try {
                final ObjectNode line = mapper.createObjectNode();
                line.put("ts", event.timestamp().toString());
                line.put("tool", event.toolName());
                line.set("args", event.arguments());
                line.put("durationMs", event.durationMs());
                line.put("ok", event.error().isEmpty());
                event.error().ifPresent(err -> line.put("error", err.getMessage()));
                context.addSessionLogLine(mapper.writeValueAsString(line));
            } catch (final Exception e) {
                log.log(System.Logger.Level.WARNING, "Session log serialization failed: {0}", e.getMessage());
            }

            if (event.error().isEmpty()) {
                context.addSessionDisplayLine("ok\t" + event.toolName() + " (" + event.durationMs() + "ms)");
            } else {
                final String msg = event.error().get().getMessage();
                final String truncated = msg != null && msg.length() > 80 ? msg.substring(0, 80) + "…" : msg;
                context.addSessionDisplayLine("err\t" + event.toolName() + " (" + event.durationMs() + "ms) — " + truncated);
            }
        });

        final ConsoleEventBus eventBus = new ConsoleEventBus();
        mcp.toolCallEvents().subscribe(event -> {
            eventBus.publish("state-changed", "");
            if ("export.all".equals(event.toolName()) && event.error().isEmpty()) {
                eventBus.publish("tracks-changed", "");
            }
        });

        final ConsoleHandler console = new ConsoleHandler(context, eventBus);

        return RouterBuilder.create()
            .get("/health", HealthHandler.liveness())
            .get("/", console.page())
            .get("/tracks/{folder}", console.page())
            .get("/console/events", console.events())
            .get("/files/{folder}/{file}", console.file())
            .group(g -> g
                .middleware(HtmxMiddleware.htmxOnly())
                .get("/console/state", console.state())
                .post("/console/clear", console.clear())
                .post("/console/export", console.export())
                .get("/console/tracks", console.tracks())
                .get("/console/track/{folder}", console.trackDetail())
                .post("/console/track/{folder}/load", console.loadTrack())
                .delete("/console/track/{folder}", console.deleteTrack())
                .get("/console/text/{folder}/{file}", console.fileText()))
            .route("/mcp", mcp.handler())
            .build();
    }
}
