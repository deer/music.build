package build.music.mcp;

import build.music.core.Note;
import build.music.mcp.tools.DrumPresets;
import build.music.mcp.tools.ExportTools;
import build.music.mcp.tools.FormTools;
import build.music.mcp.tools.HarmonyTools;
import build.music.mcp.tools.ScoreTools;
import build.music.mcp.tools.TransformTools;
import build.music.mcp.tools.VoiceOpTools;
import build.music.mcp.tools.VoiceTools;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.score.Score;
import build.music.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Builds a jazz composition programmatically so you can explore the full model
 * structure (Score, Voice, NoteEvent, FormalPlan, Section, Key, BarChords …)
 * in the IntelliJ debugger.
 * <p>
 * Set a breakpoint on the final assertion and inspect `ctx` and `snapshot`.
 * <p>
 * Structure: ABA′ in A minor, 4/4 swing, 120 BPM
 * A  (8 bars) — minor jazz melody + walking bass + charleston comp + swing drums
 * B  (8 bars) — inverted + transposed variant, new comp colour
 * A′ (8 bars) — repeat of A with a ritardando on the final 2 bars
 */
class SnapshotExplorationTest {

    @Test
    void exploreCompositionModel() {
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {

            // ── Global metadata ───────────────────────────────────────────────
            ScoreTools.setMetadata(ctx, "Exploration Study", 120, "4/4");
            HarmonyTools.setKey(ctx, "A minor");
            ctx.setSwingRatio(new Fraction(2, 3));

            // ── A section bar chords (8 bars: i–i–iv–iv–ii°–V–i–V) ───────────
            HarmonyTools.setBarChords(ctx,
                "1:Am7 2:Am7 3:Dm7 4:Dm7 5:Bm7b5 6:E7 7:Am7 8:E7");

            // ── A section melody (8 bars) ─────────────────────────────────────
            VoiceTools.createVoice(ctx, "melody",
                "E5/q D5/q C5/h " +           // bar 1 — Am7
                    "r/q A4/q B4/q C5/q " +        // bar 2
                    "A4/dq G4/e F4/h " +           // bar 3 — Dm7
                    "r/h E4/q G4/q " +             // bar 4
                    "F4/q E4/q D4/q C4/q " +       // bar 5 — Bm7b5
                    "B3/q C4/e D4/e E4/h " +       // bar 6 — E7 tension
                    "A4/dh G4/q " +                // bar 7 — resolution
                    "r/q E4/q A4/q r/q");          // bar 8 — pickup feel

            VoiceTools.setDynamics(ctx, "melody", "mf");
            ScoreTools.assignInstrument(ctx, "melody", "piano");

            // Save the opening 4-note cell as a motif for B-section development
            TransformTools.saveMotif(ctx, "melody", "cell", 0, 4);

            // ── Walking bass auto-generated from bar chords ───────────────────
            HarmonyTools.walkingBass(ctx, "bass", 2, 8, null, null);
            VoiceTools.setDynamics(ctx, "bass", "f");
            ScoreTools.assignInstrument(ctx, "bass", "electric_bass");

            // ── Charleston comp on Rhodes ─────────────────────────────────────
            HarmonyTools.comp(ctx, "comp", 3, "charleston", 8, null);
            VoiceTools.setDynamics(ctx, "comp", "mp");
            ScoreTools.assignInstrument(ctx, "comp", "rhodes");

            // ── Swing drums: 1 bar preset → repeat to 8 ──────────────────────
            DrumPresets.loadPreset(ctx, "swing", 1);
            VoiceOpTools.repeat(ctx, "drums_kick", 8, "drums_kick");
            VoiceOpTools.repeat(ctx, "drums_snare", 8, "drums_snare");
            VoiceOpTools.repeat(ctx, "drums_hihat", 8, "drums_hihat");

            // ── Snapshot A section ────────────────────────────────────────────
            FormTools.createSection(ctx, "A", null, 8);

            // ── Build B section: invert the saved cell, repeat, transpose ─────
            VoiceOpTools.deleteVoice(ctx, "melody");
            VoiceOpTools.deleteVoice(ctx, "bass");
            VoiceOpTools.deleteVoice(ctx, "comp");
            VoiceOpTools.deleteVoice(ctx, "drums_kick");
            VoiceOpTools.deleteVoice(ctx, "drums_snare");
            VoiceOpTools.deleteVoice(ctx, "drums_hihat");

            HarmonyTools.setBarChords(ctx,
                "1:Cm7 2:Cm7 3:Fm7 4:Fm7 5:Dm7b5 6:G7 7:Cm7 8:G7");

            // Invert the saved cell around C5, repeat 4×, then transpose up m3
            VoiceTools.createVoiceFromMotif(ctx, "cell_inv", "cell", "invert",
                java.util.Map.of("axis", "C5"));
            VoiceOpTools.repeat(ctx, "cell_inv", 4, "b_melody_raw");
            TransformTools.transpose(ctx, "b_melody_raw", "m3", "up", "melody");
            VoiceOpTools.deleteVoice(ctx, "cell_inv");
            VoiceOpTools.deleteVoice(ctx, "b_melody_raw");

            VoiceTools.setDynamics(ctx, "melody", "f");
            ScoreTools.assignInstrument(ctx, "melody", "piano");

            HarmonyTools.walkingBass(ctx, "bass", 2, 8, null, null);
            VoiceTools.setDynamics(ctx, "bass", "f");
            ScoreTools.assignInstrument(ctx, "bass", "electric_bass");

            HarmonyTools.comp(ctx, "comp", 3, "shell_voicings", 8, null);
            VoiceTools.setDynamics(ctx, "comp", "mp");
            ScoreTools.assignInstrument(ctx, "comp", "rhodes");

            DrumPresets.loadPreset(ctx, "swing", 1);
            VoiceOpTools.repeat(ctx, "drums_kick", 8, "drums_kick");
            VoiceOpTools.repeat(ctx, "drums_snare", 8, "drums_snare");
            VoiceOpTools.repeat(ctx, "drums_hihat", 8, "drums_hihat");

            FormTools.createSection(ctx, "B", null, 8);

            // ── A′ repeat + ritardando on the final 2 bars ────────────────────
            FormTools.repeatSection(ctx, "A", "A'");
            FormTools.buildScore(ctx);

            // bars 23–24 of the 24-bar assembled score
            ctx.addTempoChange(23, 24, 72, "exponential");
        });

        ExportTools.exportAll(ctx, "test", ExportOptions.diskAndBytes());

        CompositionSnapshot snapshot = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);

        // mereology: Score.composition(Note.class) must walk Score→Part→Voice→NoteEvent
        Score score = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::buildScore);
        List<Note> notes = score.composition(Note.class).toList();

        // ← set breakpoint here; explore ctx.score(), snapshot, notes,
        //   ctx.getFormalPlan(), ctx.getBarChords(), ctx.getSwingRatio() in the debugger
        assertNotNull(snapshot);
        assertFalse(notes.isEmpty(), "composition(Note.class) must return notes via mereology traversal");
    }
}
