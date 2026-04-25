package build.music.mcp.tools;

import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.transport.json.JsonTransport;
import build.music.mcp.CompositionContext;
import build.music.mcp.CompositionSnapshot;
import build.music.mcp.MusicMarshalling;
import build.music.mcp.ToolResult;
import build.music.midi.MidiReader;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.score.Voice;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;

/**
 * Tools for saving and loading composition snapshots via codemodel marshalling.
 *
 * <p>{@code score.save} marshals the current {@link CompositionContext} to a JSON file
 * under {@code generated_tracks/}. {@code score.load} reads it back and restores the context.
 */
public final class SaveLoadTools {

    private SaveLoadTools() {
    }

    /**
     * Tool: score.save — serialize the current composition to a JSON snapshot file.
     *
     * @param ctx      the active composition context
     * @param filename the filename (without path) under {@code generated_tracks/}, e.g. {@code "mysong.json"}
     */
    public static ToolResult save(final CompositionContext ctx, final String filename) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to save. Create at least one voice first.");
        }
        try {
            final Path tracksDir = Path.of("generated_tracks");
            Files.createDirectories(tracksDir);
            final Path outputPath = tracksDir.resolve(filename.endsWith(".json") ? filename : filename + ".json");

            final CompositionSnapshot snapshot = ctx.snapshot();
            final String json = marshalToJson(snapshot, ctx);

            Files.writeString(outputPath, json);
            return ToolResult.success("Saved composition snapshot to: " + outputPath.toAbsolutePath());

        } catch (final IOException e) {
            return ToolResult.error("Failed to save snapshot: " + e.getMessage());
        }
    }

    /**
     * Tool: score.load — restore a composition from a JSON snapshot file anywhere under
     * {@code generated_tracks/}.
     *
     * <p>Search order:
     * <ol>
     *   <li>Direct top-level match: {@code generated_tracks/{filename}.json} — this is where
     *       {@code score.save} writes.</li>
     *   <li>Recursive search for {@code {filename}.json} in any subfolder — this finds files
     *       that {@code export.all} writes to numbered bundle folders like
     *       {@code generated_tracks/5_kingston_dusk/kingston_dusk.json}. If multiple matches
     *       exist (e.g. the same composition was re-exported several times), the most recently
     *       modified one wins.</li>
     * </ol>
     *
     * @param ctx      the active composition context (will be reset and repopulated)
     * @param filename the bare name (no path, no extension required), e.g. {@code "kingston_dusk"}
     */
    public static ToolResult load(final CompositionContext ctx, final String filename) {
        try {
            final Path tracksDir = Path.of("generated_tracks");
            final String target = filename.endsWith(".json") ? filename : filename + ".json";

            Path inputPath = null;

            // 1. Direct top-level match (where score.save writes).
            final Path directPath = tracksDir.resolve(target);
            if (Files.exists(directPath)) {
                inputPath = directPath;
            } else if (Files.exists(tracksDir)) {
                // 2. Recursive search for {target} anywhere under generated_tracks.
                //    Multiple matches → most recently modified wins.
                try (var stream = Files.walk(tracksDir)) {
                    inputPath = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equals(target))
                        .reduce((a, b) -> {
                            try {
                                return Files.getLastModifiedTime(b)
                                    .compareTo(Files.getLastModifiedTime(a)) > 0 ? b : a;
                            } catch (final IOException e) {
                                return a;
                            }
                        })
                        .orElse(null);
                }
            }

            if (inputPath == null) {
                return ToolResult.error(
                    "Snapshot file not found: no '" + target + "' at " +
                        tracksDir.toAbsolutePath() + " or in any subfolder.");
            }

            final String json = Files.readString(inputPath);
            final CompositionSnapshot snapshot = unmarshalFromJson(json, ctx);
            ctx.restoreFrom(snapshot);

            return ToolResult.success(
                "Loaded composition snapshot from: " + inputPath.toAbsolutePath() + "\n" +
                    ctx.describe());

        } catch (final IOException e) {
            return ToolResult.error("Failed to load snapshot: " + e.getMessage());
        }
    }

    /**
     * Tool: score.load_midi — import a MIDI file as voices into the current context.
     * Adds one voice per non-empty track. Sets tempo from the MIDI file.
     * Existing voices are preserved; use score.clear first if you want a fresh start.
     *
     * @param path path to the MIDI file (absolute or relative to the working directory)
     */
    public static ToolResult loadMidi(final CompositionContext ctx, final String path) {
        try {
            final Path midiPath = Path.of(path);
            if (!Files.exists(midiPath)) {
                return ToolResult.error("File not found: " + midiPath.toAbsolutePath());
            }
            final MidiReader.MidiImport imported = MidiReader.readWithTempo(midiPath);
            ctx.setTempo(imported.tempo());
            for (final Voice voice : imported.voices()) {
                ctx.createVoice(voice.name(), voice.events());
            }
            final List<String> names = imported.voices().stream().map(Voice::name).toList();
            return ToolResult.success(
                "Loaded " + imported.voices().size() + " voice(s) from " + path +
                    " at " + imported.tempo().bpm() + " BPM. Voice names: " + names +
                    ". Notes use sharp spellings — use transform.transpose if re-spelling is needed.");
        } catch (final IOException | InvalidMidiDataException e) {
            return ToolResult.error("Failed to load MIDI: " + e.getMessage());
        }
    }

    // ── Marshalling helpers ───────────────────────────────────────────────────

    public static String marshalToJson(final CompositionSnapshot snapshot, final CompositionContext ctx) throws IOException {
        final Marshaller marshaller = MusicMarshalling.newMarshaller();
        final JsonTransport transport = MusicMarshalling.configuredTransport(ctx.codeModel().getNameProvider());

        final Marshalled<CompositionSnapshot> marshalled = marshaller.marshal(snapshot);

        final JsonFactory factory = JsonFactory.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();
        final StringWriter writer = new StringWriter();
        try (var generator = factory.createGenerator(writer)) {
            generator.useDefaultPrettyPrinter();
            transport.write(marshalled, generator);
        }
        return writer.toString();
    }

    public static CompositionSnapshot unmarshalFromJson(final String json, final CompositionContext ctx) throws IOException {
        final Marshaller marshaller = MusicMarshalling.newMarshaller();
        marshaller.bind(MusicCodeModel.class).to(ctx.codeModel());
        final JsonTransport transport = MusicMarshalling.configuredTransport(ctx.codeModel().getNameProvider());

        final JsonFactory factory = JsonFactory.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();
        try (var parser = factory.createParser(new StringReader(json))) {
            final Marshalled<CompositionSnapshot> transported = transport.read(parser);
            return marshaller.unmarshal(transported);
        }
    }
}
