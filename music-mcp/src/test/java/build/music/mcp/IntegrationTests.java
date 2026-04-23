package build.music.mcp;

import build.music.mcp.ExportOptions;
import build.music.mcp.tools.ExportTools;
import build.music.mcp.tools.ScoreTools;
import build.music.mcp.tools.TransformTools;
import build.music.mcp.tools.VoiceTools;
import build.music.score.Score;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full composition workflow test — simulates what an AI agent would do
 * across multiple sequential tool calls.
 */
class IntegrationTests {

    @Test
    void fullCompositionWorkflow(@TempDir Path tempDir) {
        var ctx = new CompositionContext();

        // 1. Set up the piece
        ToolResult meta = ScoreTools.setMetadata(ctx, "Ode to Joy Fragment", 120, "4/4");
        assertTrue(meta.success(), meta.message());

        // 2. Create the melody (first 8 notes of Ode to Joy)
        ToolResult melody = VoiceTools.createVoice(ctx, "melody",
            "E4/q E4/q F4/q G4/q G4/q F4/q E4/q D4/q");
        assertTrue(melody.success(), melody.message());

        // 3. Save the first 4 notes as a motif
        ToolResult motifResult = TransformTools.saveMotif(ctx, "melody", "theme_a", 0, 4);
        assertTrue(motifResult.success(), motifResult.message());
        assertEquals(4, ctx.getMotif("theme_a").size());

        // 4. Transpose melody up a minor third
        ToolResult transposed = TransformTools.transpose(ctx, "melody", "m3", "up", "melody_high");
        assertTrue(transposed.success(), transposed.message());
        assertTrue(ctx.hasVoice("melody_high"));

        // 5. Create a retrograde of the melody
        ToolResult retro = TransformTools.retrograde(ctx, "melody", "melody_retro");
        assertTrue(retro.success(), retro.message());
        assertTrue(ctx.hasVoice("melody_retro"));

        // 6. Assign instruments
        assertTrue(ScoreTools.assignInstrument(ctx, "melody",      "piano").success());
        assertTrue(ScoreTools.assignInstrument(ctx, "melody_high", "strings").success());
        // melody_retro intentionally left unassigned (gets default)

        // 7. Score description should mention all voices
        ToolResult desc = ScoreTools.describeScore(ctx);
        assertTrue(desc.success());
        assertTrue(desc.message().contains("melody"));
        assertTrue(desc.message().contains("melody_high"));
        assertTrue(desc.message().contains("melody_retro"));

        // 8. Export MIDI
        String midiPath = tempDir.resolve("ode_to_joy.mid").toString();
        ToolResult midiResult = ExportTools.exportMidi(ctx, midiPath, ExportOptions.diskAndBytes());
        assertTrue(midiResult.success(), midiResult.message());
        assertTrue(Files.exists(Path.of(midiPath)));

        // 9. Verify the score structure
        Score score = ctx.buildScore();
        assertEquals(3, score.scoreParts().size());
        assertEquals("Ode to Joy Fragment", score.title());
        assertEquals(120, score.tempo().bpm());

        // Retrograde should have same length as original
        assertEquals(
            ctx.getVoice("melody").size(),
            ctx.getVoice("melody_retro").size()
        );
    }

    @Test
    void motifReuseWorkflow() {
        var ctx = new CompositionContext();

        // Create a short motif directly
        VoiceTools.createVoice(ctx, "seed", "C4/q E4/q G4/q");
        TransformTools.saveMotif(ctx, "seed", "arpeggio", null, null);

        // Use the motif to build multiple voices
        VoiceTools.createVoiceFromMotif(ctx, "v1", "arpeggio", null, null);
        VoiceTools.createVoiceFromMotif(ctx, "v2", "arpeggio", "transpose",
            java.util.Map.of("interval", "P5", "direction", "up"));
        VoiceTools.createVoiceFromMotif(ctx, "v3", "arpeggio", "retrograde", null);

        assertEquals(3, ctx.getVoice("v1").size());
        assertEquals(3, ctx.getVoice("v2").size());
        assertEquals(3, ctx.getVoice("v3").size());

        Score score = ctx.buildScore();
        // seed + v1 + v2 + v3 = 4 parts
        assertEquals(4, score.scoreParts().size());
    }

    @Test
    void clearAndRebuild() {
        var ctx = new CompositionContext();
        VoiceTools.createVoice(ctx, "old_voice", "C4/q");
        ScoreTools.setMetadata(ctx, "Old Title", 80, "3/4");

        ScoreTools.clearScore(ctx);

        assertTrue(ctx.voiceNames().isEmpty());
        assertEquals("Untitled", ctx.getTitle());

        // Can start fresh after clear
        VoiceTools.createVoice(ctx, "new_voice", "G4/q A4/q B4/q");
        assertEquals(1, ctx.buildScore().scoreParts().size());
    }
}
