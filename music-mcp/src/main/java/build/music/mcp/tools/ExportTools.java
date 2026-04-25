package build.music.mcp.tools;

import build.music.lilypond.LilyPondEngraver;
import build.music.lilypond.LilyPondRenderer;
import build.music.mcp.CompositionContext;
import build.music.mcp.ExportArtifact;
import build.music.mcp.ExportOptions;
import build.music.mcp.ToolResult;
import build.music.midi.MidiRenderer;
import build.music.midi.MidiWriter;
import build.music.musicxml.MusicXmlRenderer;
import build.music.score.Score;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

public final class ExportTools {

    private ExportTools() {
    }

    public static ToolResult exportMidi(final CompositionContext ctx,
                                        final String filename,
                                        final ExportOptions options) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final Sequence sequence = MidiRenderer.render(score);
            final byte[] midiBytes = MidiWriter.toBytes(sequence);

            final String name = filename != null && !filename.isBlank()
                ? filename
                : sanitizeFilename(score.title()) + ".mid";

            final StringBuilder message = new StringBuilder();
            if (options.writeToDisk()) {
                final Path path = Path.of(name);
                Files.write(path, midiBytes);
                message.append("MIDI file written to: ").append(path.toAbsolutePath());
            } else {
                message.append("MIDI rendered: ").append(name);
            }

            final List<ExportArtifact> artifacts = options.returnBytes()
                ? List.of(new ExportArtifact(name, "audio/midi", midiBytes))
                : List.of();

            return ToolResult.success(message.toString(), artifacts);
        } catch (final InvalidMidiDataException e) {
            return ToolResult.error("MIDI render failed: " + e.getMessage());
        } catch (final IOException e) {
            return ToolResult.error("Failed to write MIDI file: " + e.getMessage());
        }
    }

    public static ToolResult exportLilypond(final CompositionContext ctx,
                                            final String filename,
                                            final ExportOptions options) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final String lySource = LilyPondRenderer.render(score);
            final byte[] lyBytes = lySource.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final String baseName = filename != null && !filename.isBlank()
                ? stripExtension(filename)
                : sanitizeFilename(score.title());
            final Path outputDir = Path.of(".").toAbsolutePath().normalize();
            final Path lyPath = outputDir.resolve(baseName + ".ly");
            final StringBuilder message = new StringBuilder();
            final List<ExportArtifact> artifacts = new ArrayList<>();

            if (LilyPondEngraver.isAvailable()) {
                try {
                    final Path pdfPath = LilyPondEngraver.engravePdf(lySource,
                        options.writeToDisk() ? outputDir : Files.createTempDirectory("music-ly"),
                        baseName);
                    if (options.writeToDisk()) {
                        message.append("LilyPond source written to: ").append(lyPath)
                            .append("\nPDF engraved to: ").append(pdfPath);
                    } else {
                        message.append("LilyPond engraved: ").append(baseName).append(".pdf");
                    }
                    if (options.returnBytes()) {
                        artifacts.add(new ExportArtifact(baseName + ".ly", "text/x-lilypond", lyBytes));
                        artifacts.add(new ExportArtifact(baseName + ".pdf", "application/pdf",
                            Files.readAllBytes(pdfPath)));
                    }
                } catch (final IOException e) {
                    if (options.writeToDisk()) {
                        Files.writeString(lyPath, lySource);
                        message.append("LilyPond source written to: ").append(lyPath)
                            .append("\nNote: LilyPond engraving failed: ").append(e.getMessage());
                    } else {
                        message.append("LilyPond rendered: ").append(baseName).append(".ly")
                            .append("\nNote: LilyPond engraving failed: ").append(e.getMessage());
                    }
                    if (options.returnBytes()) {
                        artifacts.add(new ExportArtifact(baseName + ".ly", "text/x-lilypond", lyBytes));
                    }
                }
            } else {
                if (options.writeToDisk()) {
                    Files.writeString(lyPath, lySource);
                    message.append("LilyPond source written to: ").append(lyPath)
                        .append("\nNote: LilyPond not found on PATH — .ly file written but PDF not generated.");
                } else {
                    message.append("LilyPond rendered: ").append(baseName).append(".ly")
                        .append("\nNote: LilyPond not found on PATH — PDF not generated.");
                }
                if (options.returnBytes()) {
                    artifacts.add(new ExportArtifact(baseName + ".ly", "text/x-lilypond", lyBytes));
                }
            }

            // lySource is always returned as data so the agent can read the notation inline
            return new ToolResult(true, message.toString(), lySource,
                artifacts.isEmpty() ? List.of() : artifacts);
        } catch (final IOException e) {
            return ToolResult.error("Failed to write LilyPond file: " + e.getMessage());
        }
    }

    public static ToolResult exportAll(final CompositionContext ctx,
                                       final String folderName,
                                       final ExportOptions options) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final String baseName = folderName != null && !folderName.isBlank()
                ? folderName
                : sanitizeFilename(score.title());

            final StringBuilder message = new StringBuilder();
            final List<ExportArtifact> artifacts = new ArrayList<>();

            // MIDI
            final Sequence sequence = MidiRenderer.render(score);
            final byte[] midiBytes = MidiWriter.toBytes(sequence);

            // LilyPond
            final String lySource = LilyPondRenderer.render(score);
            final byte[] lyBytes = lySource.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            if (options.writeToDisk()) {
                final Path tracksDir = Path.of(".").toAbsolutePath().normalize().resolve("generated_tracks");
                Files.createDirectories(tracksDir);
                int next = 1;
                try (var entries = Files.list(tracksDir)) {
                    next = (int) entries.filter(Files::isDirectory).count() + 1;
                }
                final String numberedName = next + "_" + baseName;
                final Path outputDir = tracksDir.resolve(numberedName);
                Files.createDirectories(outputDir);

                final Path midiPath = outputDir.resolve(baseName + ".mid");
                Files.write(midiPath, midiBytes);

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

                // JSON snapshot
                final Path jsonPath = outputDir.resolve(baseName + ".json");
                try {
                    final String json = SaveLoadTools.marshalToJson(ctx.snapshot(), ctx);
                    Files.writeString(jsonPath, json);
                    message.append("\n  JSON:      ").append(jsonPath);
                } catch (final IOException e) {
                    message.append("\n  JSON:      (snapshot write failed: ").append(e.getMessage()).append(")");
                }

                // Session log
                final List<String> logLines = ctx.sessionLogLines();
                if (!logLines.isEmpty()) {
                    final Path sessionLogPath = outputDir.resolve("session.jsonl");
                    Files.writeString(sessionLogPath, String.join("\n", logLines) + "\n");
                    message.append("\n  Session:   ").append(sessionLogPath);
                }
            } else {
                message.append("Exported: ").append(baseName);
            }

            if (options.returnBytes()) {
                artifacts.add(new ExportArtifact(baseName + ".mid", "audio/midi", midiBytes));
                artifacts.add(new ExportArtifact(baseName + ".ly", "text/x-lilypond", lyBytes));
            }

            return ToolResult.success(message.toString(), artifacts.isEmpty() ? List.of() : artifacts);
        } catch (final InvalidMidiDataException e) {
            return ToolResult.error("MIDI render failed: " + e.getMessage());
        } catch (final IOException e) {
            return ToolResult.error("Export failed: " + e.getMessage());
        }
    }

    public static ToolResult exportMusicXml(final CompositionContext ctx,
                                             final String filename,
                                             final ExportOptions options) {
        if (ctx.voiceNames().isEmpty()) {
            return ToolResult.error("No voices to export. Create at least one voice first.");
        }
        try {
            final Score score = ctx.buildScore();
            final String xmlSource = MusicXmlRenderer.render(score);
            final byte[] xmlBytes = xmlSource.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final String baseName = filename != null && !filename.isBlank()
                ? stripExtension(filename)
                : sanitizeFilename(score.title());
            final String outName = baseName + ".musicxml";

            final StringBuilder message = new StringBuilder();
            if (options.writeToDisk()) {
                final java.nio.file.Path path = java.nio.file.Path.of(outName);
                java.nio.file.Files.writeString(path, xmlSource);
                message.append("MusicXML written to: ").append(path.toAbsolutePath());
            } else {
                message.append("MusicXML rendered: ").append(outName);
            }

            final List<ExportArtifact> artifacts = options.returnBytes()
                ? List.of(new ExportArtifact(outName, "application/vnd.recordare.musicxml+xml", xmlBytes))
                : List.of();

            return ToolResult.success(message.toString(), artifacts);
        } catch (final java.io.IOException e) {
            return ToolResult.error("Failed to write MusicXML: " + e.getMessage());
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
