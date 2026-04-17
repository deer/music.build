package build.music.mcp.tools;

import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportToolsTests {

    private CompositionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompositionContext();
        ctx.setTitle("Test Piece");
    }

    @Test
    void exportMidiCreatesFile(@TempDir Path tempDir) throws Exception {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q C5/h");
        ScoreTools.assignInstrument(ctx, "melody", "piano");

        String midiPath = tempDir.resolve("test.mid").toString();
        ToolResult result = ExportTools.exportMidi(ctx, midiPath);

        assertTrue(result.success(), result.message());
        assertTrue(Files.exists(Path.of(midiPath)));
        assertTrue(Files.size(Path.of(midiPath)) > 0);
    }

    @Test
    void exportMidiWithDefaultFilename(@TempDir Path tempDir) throws Exception {
        // Change working dir isn't straightforward in tests, so use explicit filename
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q");
        String midiPath = tempDir.resolve("my_piece.mid").toString();
        ToolResult result = ExportTools.exportMidi(ctx, midiPath);
        assertTrue(result.success(), result.message());
    }

    @Test
    void exportLilypondProducesValidSource(@TempDir Path tempDir) {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q C5/h");

        String lyPath = tempDir.resolve("test").toString();
        ToolResult result = ExportTools.exportLilypond(ctx, lyPath);

        assertTrue(result.success(), result.message());
        assertNotNull(result.data());
        assertTrue(result.data().contains("\\version"), "LilyPond source should start with \\version");
        assertTrue(result.data().contains("c'"), "LilyPond source should contain note names");
    }

    @Test
    void exportWithNoVoicesReturnsError() {
        ToolResult midi = ExportTools.exportMidi(ctx, "out.mid");
        assertFalse(midi.success());
        assertTrue(midi.message().contains("No voices"));

        ToolResult ly = ExportTools.exportLilypond(ctx, "out.ly");
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
        ToolResult result = ExportTools.exportMidi(ctx, midiPath);

        assertTrue(result.success(), result.message());
        assertTrue(Files.size(Path.of(midiPath)) > 100);
    }
}
