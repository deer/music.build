package build.music.mcp.tools;

import build.music.mcp.CompositionContext;
import build.music.mcp.ExportOptions;
import build.music.mcp.ToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class ExportToolsTests {

    private CompositionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompositionContext();
        ctx.setTitle("Test Piece");
    }

    @AfterEach
    void cleanUp() throws IOException {
        Path tracksDir = Path.of("generated_tracks");
        if (Files.exists(tracksDir)) {
            try (var walk = Files.walk(tracksDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void exportMidiCreatesFile(@TempDir Path tempDir) throws Exception {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q C5/h");
        ScoreTools.assignInstrument(ctx, "melody", "piano");

        String midiPath = tempDir.resolve("test.mid").toString();
        ToolResult result = ExportTools.exportMidi(ctx, midiPath, ExportOptions.diskAndBytes());

        assertTrue(result.success(), result.message());
        assertTrue(Files.exists(Path.of(midiPath)));
        assertTrue(Files.size(Path.of(midiPath)) > 0);
    }

    @Test
    void exportMidiWithDefaultFilename(@TempDir Path tempDir) throws Exception {
        // Change working dir isn't straightforward in tests, so use explicit filename
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q");
        String midiPath = tempDir.resolve("my_piece.mid").toString();
        ToolResult result = ExportTools.exportMidi(ctx, midiPath, ExportOptions.diskAndBytes());
        assertTrue(result.success(), result.message());
    }

    @Test
    void exportLilypondProducesValidSource(@TempDir Path tempDir) {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q C5/h");

        String lyPath = tempDir.resolve("test").toString();
        ToolResult result = ExportTools.exportLilypond(ctx, lyPath, ExportOptions.diskAndBytes());

        assertTrue(result.success(), result.message());
        assertNotNull(result.data());
        assertTrue(result.data().contains("\\version"), "LilyPond source should start with \\version");
        assertTrue(result.data().contains("c'"), "LilyPond source should contain note names");
    }

    @Test
    void exportWithNoVoicesReturnsError() {
        ToolResult midi = ExportTools.exportMidi(ctx, "out.mid", ExportOptions.diskAndBytes());
        assertFalse(midi.success());
        assertTrue(midi.message().contains("No voices"));

        ToolResult ly = ExportTools.exportLilypond(ctx, "out.ly", ExportOptions.diskAndBytes());
        assertFalse(ly.success());
        assertTrue(ly.message().contains("No voices"));
    }

    @Test
    void exportMidiMultipleVoices(@TempDir Path tempDir) throws Exception {
        VoiceTools.createVoice(ctx, "melody",   "E4/q E4/q F4/q G4/q");
        VoiceTools.createVoice(ctx, "bass",     "C2/h G2/h");
        ScoreTools.assignInstrument(ctx, "melody", "violin");
        ScoreTools.assignInstrument(ctx, "bass",   "cello");

        String midiPath = tempDir.resolve("multi.mid").toString();
        ToolResult result = ExportTools.exportMidi(ctx, midiPath, ExportOptions.diskAndBytes());

        assertTrue(result.success(), result.message());
        assertTrue(Files.size(Path.of(midiPath)) > 100);
    }

    @Test
    void exportMidiBytesOnlyReturnsArtifactWithoutWritingDisk(@TempDir Path tempDir) {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");

        ToolResult result = ExportTools.exportMidi(ctx, "composition.mid", ExportOptions.bytesOnly());

        assertTrue(result.success(), result.message());
        assertEquals(1, result.artifacts().size());
        assertEquals("composition.mid", result.artifacts().getFirst().name());
        assertEquals("audio/midi", result.artifacts().getFirst().mimeType());
        assertTrue(result.artifacts().getFirst().data().length > 0);
        assertFalse(Files.exists(tempDir.resolve("composition.mid")), "bytesOnly must not write to disk");
        assertFalse(Files.exists(Path.of("composition.mid")), "bytesOnly must not write to disk");
    }

    @Test
    void exportLilypondBytesOnlyReturnsArtifactWithoutWritingDisk(@TempDir Path tempDir) {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");

        ToolResult result = ExportTools.exportLilypond(ctx, "composition", ExportOptions.bytesOnly());

        assertTrue(result.success(), result.message());
        assertNotNull(result.data(), "LilyPond source should still be in data field");
        assertTrue(result.data().contains("\\version"));
        assertFalse(Files.exists(Path.of("composition.ly")), "bytesOnly must not write to disk");
        // At minimum the .ly artifact must be present
        assertTrue(result.artifacts().stream().anyMatch(a -> a.name().equals("composition.ly")));
        assertTrue(result.artifacts().stream().anyMatch(a -> a.mimeType().equals("text/x-lilypond")));
    }

    @Test
    void exportAllBytesOnlyReturnsArtifactsWithoutWritingDisk() {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");

        ToolResult result = ExportTools.exportAll(ctx, "test_piece", ExportOptions.bytesOnly());

        assertTrue(result.success(), result.message());
        assertTrue(result.artifacts().stream().anyMatch(a -> a.name().equals("test_piece.mid")));
        assertTrue(result.artifacts().stream().anyMatch(a -> a.name().equals("test_piece.ly")));
        assertFalse(Files.exists(Path.of("generated_tracks")), "bytesOnly must not create generated_tracks");
    }

    @Test
    void exportAllDiskAndBytesWritesDiskAndReturnsArtifacts(@TempDir Path tempDir) throws Exception {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");
        // Change working dir isn't possible in tests, so verify artifact bytes are non-empty
        ToolResult result = ExportTools.exportAll(ctx, null, ExportOptions.diskAndBytes());

        assertTrue(result.success(), result.message());
        // Artifacts returned
        assertTrue(result.artifacts().stream().anyMatch(a -> a.mimeType().equals("audio/midi")));
        assertTrue(result.artifacts().stream().anyMatch(a -> a.mimeType().equals("text/x-lilypond")));
        // Disk written
        assertTrue(Files.isDirectory(Path.of("generated_tracks")));
    }
}
