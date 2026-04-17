package build.music.mcp.tools;

import build.music.lilypond.LilyPondEngraver;
import build.music.lilypond.LilyPondRenderer;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.midi.MidiRenderer;
import build.music.midi.MidiWriter;
import build.music.score.Score;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

/**
 * Tools for exporting the current composition to MIDI, LilyPond, or audio playback.
 */
public final class ExportTools {

    private ExportTools() {
    }

    /**
     * Tool: export.midi — render the current score to a MIDI file.
     *
     * @param filename optional filename; defaults to "{title}.mid"
     */
    public static ToolResult exportMidi(final CompositionContext ctx, final String filename) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final Sequence sequence = MidiRenderer.render(score);

            final String name = filename != null && !filename.isBlank()
                ? filename
                : sanitizeFilename(score.title()) + ".mid";
            final Path path = Path.of(name);
            MidiWriter.write(sequence, path);

            return ToolResult.success(
                "MIDI file written to: " + path.toAbsolutePath());
        } catch (final InvalidMidiDataException e) {
            return ToolResult.error("MIDI render failed: " + e.getMessage());
        } catch (final IOException e) {
            return ToolResult.error("Failed to write MIDI file: " + e.getMessage());
        }
    }

    /**
     * Tool: export.lilypond — render the current score to LilyPond source.
     * Writes a .ly file and, if LilyPond is installed, engraves a PDF.
     *
     * @param filename optional base filename; defaults to "{title}.ly"
     */
    public static ToolResult exportLilypond(final CompositionContext ctx, final String filename) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final String lySource = LilyPondRenderer.render(score);

            final String baseName = filename != null && !filename.isBlank()
                ? stripExtension(filename)
                : sanitizeFilename(score.title());
            final Path outputDir = Path.of(".").toAbsolutePath().normalize();
            final Path lyPath = outputDir.resolve(baseName + ".ly");
            final StringBuilder message = new StringBuilder();

            if (LilyPondEngraver.isAvailable()) {
                try {
                    final Path pdfPath = LilyPondEngraver.engravePdf(lySource, outputDir, baseName);
                    // engravePdf writes the .ly file itself
                    message.append("LilyPond source written to: ").append(lyPath)
                        .append("\nPDF engraved to: ").append(pdfPath);
                } catch (final IOException e) {
                    // Fall back to writing .ly only
                    Files.writeString(lyPath, lySource);
                    message.append("LilyPond source written to: ").append(lyPath)
                        .append("\nNote: LilyPond engraving failed: ").append(e.getMessage());
                }
            } else {
                Files.writeString(lyPath, lySource);
                message.append("LilyPond source written to: ").append(lyPath)
                    .append("\nNote: LilyPond not found on PATH — .ly file written but PDF not generated.");
            }

            return ToolResult.success(message.toString(), lySource);
        } catch (final IOException e) {
            return ToolResult.error("Failed to write LilyPond file: " + e.getMessage());
        }
    }

    /**
     * Tool: export.all — render the current score to a named folder containing MIDI, LilyPond,
     * and JSON snapshot files. Creates {folder}/{title}.mid, {folder}/{title}.ly (plus PDF if
     * LilyPond is installed), and {folder}/{title}.json (the codemodel-marshalled snapshot that
     * can be reloaded via score.load).
     *
     * @param folderName optional folder name; defaults to the sanitized score title
     */
    public static ToolResult exportAll(final CompositionContext ctx, final String folderName) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final String baseName = folderName != null && !folderName.isBlank()
                ? folderName
                : sanitizeFilename(score.title());

            final Path tracksDir = Path.of(".").toAbsolutePath().normalize().resolve("generated_tracks");
            Files.createDirectories(tracksDir);
            int next = 1;
            try (var entries = Files.list(tracksDir)) {
                next = (int) entries.filter(Files::isDirectory).count() + 1;
            }
            final String numberedName = next + "_" + baseName;
            final Path outputDir = tracksDir.resolve(numberedName);
            Files.createDirectories(outputDir);

            // MIDI
            final Sequence sequence = MidiRenderer.render(score);
            final Path midiPath = outputDir.resolve(baseName + ".mid");
            MidiWriter.write(sequence, midiPath);

            // LilyPond
            final String lySource = LilyPondRenderer.render(score);
            final StringBuilder message = new StringBuilder();
            message.append("Exported to folder: ").append(outputDir).append("\n");
            message.append("  MIDI:      ").append(midiPath).append("\n");

            if (LilyPondEngraver.isAvailable()) {
                try {
                    final Path pdfPath = LilyPondEngraver.engravePdf(lySource, outputDir, baseName);
                    message.append("  LilyPond:  ").append(outputDir.resolve(baseName + ".ly")).append("\n");
                    message.append("  PDF:       ").append(pdfPath);
                } catch (final IOException e) {
                    final Path lyPath = outputDir.resolve(baseName + ".ly");
                    Files.writeString(lyPath, lySource);
                    message.append("  LilyPond:  ").append(lyPath).append("\n");
                    message.append("  PDF:       (engraving failed: ").append(e.getMessage()).append(")");
                }
            } else {
                final Path lyPath = outputDir.resolve(baseName + ".ly");
                Files.writeString(lyPath, lySource);
                message.append("  LilyPond:  ").append(lyPath).append("\n");
                message.append("  PDF:       (LilyPond not on PATH — .ly written only)");
            }

            // JSON snapshot — the codemodel-marshalled canonical form, round-trippable via score.load
            final Path jsonPath = outputDir.resolve(baseName + ".json");
            try {
                final String json = SaveLoadTools.marshalToJson(ctx.snapshot(), ctx);
                Files.writeString(jsonPath, json);
                message.append("\n  JSON:      ").append(jsonPath);
            } catch (final IOException e) {
                message.append("\n  JSON:      (snapshot write failed: ").append(e.getMessage()).append(")");
            }

            // Session log — one JSON line per tool call, for replay and variation work
            final List<String> logLines = ctx.sessionLogLines();
            if (!logLines.isEmpty()) {
                final Path sessionLogPath = outputDir.resolve("session.jsonl");
                Files.writeString(sessionLogPath, String.join("\n", logLines) + "\n");
                message.append("\n  Session:   ").append(sessionLogPath);
            }

            return ToolResult.success(message.toString());
        } catch (final InvalidMidiDataException e) {
            return ToolResult.error("MIDI render failed: " + e.getMessage());
        } catch (final IOException e) {
            return ToolResult.error("Export failed: " + e.getMessage());
        }
    }

    // --- Internal helpers ---

    private static String sanitizeFilename(final String title) {
        return title.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String stripExtension(final String filename) {
        final int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
