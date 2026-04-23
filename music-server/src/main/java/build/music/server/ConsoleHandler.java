package build.music.server;

import build.base.template.HtmlOut;
import build.base.template.Template;
import build.music.mcp.CompositionContext;
import build.music.mcp.tools.ExportTools;
import build.music.mcp.tools.SaveLoadTools;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.server.console.ConsolePage;
import build.music.server.console.FileContentPartial;
import build.music.server.console.LogDisplayEntry;
import build.music.server.console.StatePartial;
import build.music.server.console.TrackDetailPartial;
import build.music.server.console.TrackEntry;
import build.music.server.console.TracksPartial;
import build.serve.foundation.Handler;
import build.serve.htmx.HtmxResponse;
import build.serve.sse.SseEvent;
import build.serve.sse.SseUpgrade;
import build.serve.template.HtmlContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class ConsoleHandler {

    private static final Map<String, String> MIME_TYPES = Map.of(
        ".mid", "audio/midi",
        ".pdf", "application/pdf",
        ".json", "application/json",
        ".jsonl", "text/plain",
        ".ly", "text/plain"
    );

    private final CompositionContext context;
    private final ConsoleEventBus eventBus;
    private final Path tracksDir;

    ConsoleHandler(final CompositionContext context, final ConsoleEventBus eventBus) {
        this.context = context;
        this.eventBus = eventBus;
        this.tracksDir = Path.of(".").toAbsolutePath().normalize().resolve("generated_tracks");
    }

    Handler page() {
        return exchange -> HtmxResponse.of(exchange.response()).send(html(new ConsolePage()));
    }

    Handler events() {
        return SseUpgrade.sse(emitter -> {
            emitter.send(SseEvent.of("connected", "ok"));
            eventBus.waitUntilClosed(emitter);
        });
    }

    Handler state() {
        return exchange -> HtmxResponse.of(exchange.response()).send(html(statePartial()));
    }

    Handler clear() {
        return exchange -> {
            context.clear();
            context.addSessionDisplayLine("sep\t─── context cleared ───");
            HtmxResponse.of(exchange.response()).send(html(statePartial()));
        };
    }

    Handler export() {
        return exchange -> {
            final long start = System.currentTimeMillis();
            try {
                final var result = ScopedValue
                    .where(MusicCodeModel.CURRENT, context.codeModel())
                    .call(() -> ExportTools.exportAll(context, null));
                final long ms = System.currentTimeMillis() - start;
                if (result.success()) {
                    context.addSessionDisplayLine("ok\texport.all (" + ms + "ms) [console]");
                    eventBus.publish("tracks-changed", "");
                } else {
                    context.addSessionDisplayLine("err\texport.all (" + ms + "ms) — " + result.message());
                }
            } catch (final Exception e) {
                final long ms = System.currentTimeMillis() - start;
                context.addSessionDisplayLine("err\texport.all (" + ms + "ms) — " + e.getMessage());
            }
            HtmxResponse.of(exchange.response()).send(html(statePartial()));
        };
    }

    Handler loadTrack() {
        return exchange -> {
            final String folder = exchange.request().pathParam("folder").orElse("");
            if (folder.contains("..") || folder.isBlank()) {
                exchange.response().status(400).send("Bad request");
                return;
            }
            final TrackEntry entry = TrackEntry.from(tracksDir.resolve(folder));
            if (!entry.hasJson()) {
                exchange.response().status(404).send("No snapshot file in this track");
                return;
            }
            final long start = System.currentTimeMillis();
            try {
                final var result = SaveLoadTools.load(context, entry.jsonFile());
                final long ms = System.currentTimeMillis() - start;
                if (result.success()) {
                    context.addSessionDisplayLine("ok\tscore.load (" + ms + "ms) [console] ← " + folder);
                } else {
                    context.addSessionDisplayLine("err\tscore.load (" + ms + "ms) — " + result.message());
                }
            } catch (final Exception e) {
                final long ms = System.currentTimeMillis() - start;
                context.addSessionDisplayLine("err\tscore.load (" + ms + "ms) — " + e.getMessage());
            }
            eventBus.publish("state-changed", "");
            HtmxResponse.of(exchange.response()).send(html(new TrackDetailPartial(entry)));
        };
    }

    Handler deleteTrack() {
        return exchange -> {
            final String folder = exchange.request().pathParam("folder").orElse("");
            if (folder.contains("..") || folder.isBlank()) {
                exchange.response().status(400).send("Bad request");
                return;
            }
            deleteRecursively(tracksDir.resolve(folder));
            eventBus.publish("tracks-changed", "");
            final List<TrackEntry> tracks = trackEntries();
            final TrackEntry active = tracks.isEmpty() ? null : tracks.getFirst();
            HtmxResponse.of(exchange.response()).send(html(new TracksPartial(tracks, active)));
        };
    }

    Handler tracks() {
        return exchange -> {
            final String selected = exchange.request().queryParam("selected").orElse("");
            final List<TrackEntry> tracks = trackEntries();
            final TrackEntry active = tracks.stream()
                .filter(t -> t.name().equals(selected))
                .findFirst()
                .orElse(tracks.isEmpty() ? null : tracks.getFirst());
            HtmxResponse.of(exchange.response()).send(html(new TracksPartial(tracks, active)));
        };
    }

    Handler trackDetail() {
        return exchange -> {
            final String folder = exchange.request().pathParam("folder").orElse("");
            if (folder.contains("..")) {
                exchange.response().status(400).send("Bad request");
                return;
            }
            final TrackEntry entry = TrackEntry.from(tracksDir.resolve(folder));
            HtmxResponse.of(exchange.response())
                .pushUrl("/tracks/" + folder)
                .trigger("stopPlayer")
                .send(html(new TrackDetailPartial(entry)));
        };
    }

    Handler file() {
        return exchange -> {
            final String folder = exchange.request().pathParam("folder").orElse("");
            final String filename = exchange.request().pathParam("file").orElse("");

            if (folder.contains("..") || filename.contains("..")) {
                exchange.response().status(400).send("Bad request");
                return;
            }

            final Path resolved = tracksDir.resolve(folder).resolve(filename);
            if (!Files.isRegularFile(resolved)) {
                exchange.response().status(404).send("Not found");
                return;
            }

            final String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.'))
                : "";
            exchange.response()
                .status(200)
                .header("Content-Type", MIME_TYPES.getOrDefault(ext, "application/octet-stream"))
                .send(Files.readAllBytes(resolved));
        };
    }

    Handler fileText() {
        return exchange -> {
            final String folder = exchange.request().pathParam("folder").orElse("");
            final String filename = exchange.request().pathParam("file").orElse("");

            if (folder.contains("..") || filename.contains("..")) {
                exchange.response().status(400).send("Bad request");
                return;
            }

            final Path resolved = tracksDir.resolve(folder).resolve(filename);
            if (!Files.isRegularFile(resolved)) {
                exchange.response().status(404).send("Not found");
                return;
            }

            HtmxResponse.of(exchange.response()).send(html(new FileContentPartial(Files.readString(resolved))));
        };
    }

    private static HtmlContent html(final Template<HtmlOut> template) {
        return () -> {
            final var out = new HtmlOut();
            template.render(out);
            return out.toString();
        };
    }

    private StatePartial statePartial() {
        final List<String> all = context.sessionDisplayLines();
        final List<LogDisplayEntry> recent = all.subList(Math.max(0, all.size() - 20), all.size())
            .stream().map(LogDisplayEntry::parse).toList();
        final List<String> descriptionLines = context.describe().lines().toList();
        return new StatePartial(descriptionLines, recent, all.size());
    }

    private List<TrackEntry> trackEntries() {
        if (!Files.isDirectory(tracksDir)) {
            return List.of();
        }
        final List<TrackEntry> result = new ArrayList<>();
        try (var entries = Files.list(tracksDir)) {
            entries.filter(Files::isDirectory)
                .sorted(Comparator.comparingInt(ConsoleHandler::numericPrefix))
                .map(TrackEntry::from)
                .forEach(result::add);
        } catch (final IOException e) {
            // return empty
        }
        return result;
    }

    private static void deleteRecursively(final Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    // best-effort
                }
            });
        } catch (final IOException e) {
            // best-effort
        }
    }

    private static int numericPrefix(final Path p) {
        final String name = p.getFileName().toString();
        final int underscore = name.indexOf('_');
        if (underscore <= 0) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(name.substring(0, underscore));
        } catch (final NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
