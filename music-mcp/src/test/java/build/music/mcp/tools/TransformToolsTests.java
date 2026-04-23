package build.music.mcp.tools;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Velocity;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformToolsTests {

    private CompositionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompositionContext();
    }

    @Test
    void transposeUpPerfectFifth() {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");

        ToolResult result = TransformTools.transpose(ctx, "melody", "P5", "up", null);
        assertTrue(result.success(), result.message());

        assertTrue(ctx.hasVoice("melody_transposed"));
        List<NoteEvent> transposed = ctx.getVoice("melody_transposed");
        assertEquals(3, transposed.size());

        // C4 + P5 = G4 (MIDI 67), E4 + P5 = B4 (MIDI 71), G4 + P5 = D5 (MIDI 74)
        assertEquals(67, ((Note) transposed.get(0)).midi());
        assertEquals(71, ((Note) transposed.get(1)).midi());
        assertEquals(74, ((Note) transposed.get(2)).midi());
    }

    @Test
    void transposeWithCustomTargetVoice() {
        VoiceTools.createVoice(ctx, "melody", "C4/q");
        TransformTools.transpose(ctx, "melody", "M3", "up", "melody_high");
        assertTrue(ctx.hasVoice("melody_high"));
        assertFalse(ctx.hasVoice("melody_transposed"));
    }

    @Test
    void retrograde() {
        VoiceTools.createVoice(ctx, "melody", "C4/q D4/q E4/q F4/q");

        ToolResult result = TransformTools.retrograde(ctx, "melody", null);
        assertTrue(result.success(), result.message());

        List<NoteEvent> reversed = ctx.getVoice("melody_retrograde");
        assertEquals(4, reversed.size());
        // Original: C4 D4 E4 F4 → Reversed: F4 E4 D4 C4
        assertEquals(65, ((Note) reversed.get(0)).midi()); // F4
        assertEquals(64, ((Note) reversed.get(1)).midi()); // E4
        assertEquals(62, ((Note) reversed.get(2)).midi()); // D4
        assertEquals(60, ((Note) reversed.get(3)).midi()); // C4
    }

    @Test
    void saveMotifByIndexRange() {
        VoiceTools.createVoice(ctx, "melody", "C4/q D4/q E4/q F4/q G4/q A4/q B4/q C5/q");

        ToolResult result = TransformTools.saveMotif(ctx, "melody", "theme_a", 0, 4);
        assertTrue(result.success(), result.message());

        assertTrue(ctx.hasMotif("theme_a"));
        List<NoteEvent> motif = ctx.getMotif("theme_a");
        assertEquals(4, motif.size());
        assertEquals(60, ((Note) motif.get(0)).midi()); // C4
        assertEquals(65, ((Note) motif.get(3)).midi()); // F4
    }

    @Test
    void saveMotifEntireVoice() {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q");

        TransformTools.saveMotif(ctx, "melody", "full", null, null);
        assertEquals(3, ctx.getMotif("full").size());
    }

    @Test
    void augmentDoubles() {
        VoiceTools.createVoice(ctx, "melody", "C4/q E4/q");

        ToolResult result = TransformTools.augment(ctx, "melody", "2/1", null);
        assertTrue(result.success(), result.message());

        List<NoteEvent> augmented = ctx.getVoice("melody_augmented");
        assertEquals(2, augmented.size());
        // Quarter notes doubled to half notes (fraction 1/2)
        assertEquals("1/2", augmented.get(0).duration().fraction().toString());
    }

    @Test
    void transposeNonExistentVoiceReturnsError() {
        ToolResult result = TransformTools.transpose(ctx, "missing", "P5", "up", null);
        assertFalse(result.success());
        assertTrue(result.message().contains("missing"));
    }

    @Test
    void saveMotifInvalidRangeReturnsError() {
        VoiceTools.createVoice(ctx, "melody", "C4/q D4/q");

        ToolResult result = TransformTools.saveMotif(ctx, "melody", "bad", 5, 10);
        assertFalse(result.success());
    }

    @Test
    void transpose_transposesChordPitches() {
        var c4 = SpelledPitch.parse("C4");
        var e4 = SpelledPitch.parse("E4");
        var chord = Chord.of(List.of(c4, e4), RhythmicValue.QUARTER, Velocity.MF);
        ctx.createVoice("comp", List.of(chord));

        ToolResult result = TransformTools.transpose(ctx, "comp", "P5", "up", null);
        assertTrue(result.success(), result.message());

        var transposed = (Chord) ctx.getVoice("comp_transposed").get(0);
        // C4 + P5 = G4 (67), E4 + P5 = B4 (71)
        assertEquals(67, transposed.pitches().get(0).midi());
        assertEquals(71, transposed.pitches().get(1).midi());
    }

    @Test
    void invert_invertsChordPitches() {
        var c4 = SpelledPitch.parse("C4");
        var e4 = SpelledPitch.parse("E4");
        var chord = Chord.of(List.of(c4, e4), RhythmicValue.QUARTER, Velocity.MF);
        ctx.createVoice("comp", List.of(chord));

        ToolResult result = TransformTools.invert(ctx, "comp", "C4", null);
        assertTrue(result.success(), result.message());

        var inverted = (Chord) ctx.getVoice("comp_inverted").get(0);
        // C4 inverted around C4 = C4 (60); E4 inverted around C4 = Ab3 (56)
        // Chord sorts ascending: Ab3 first, then C4
        assertEquals(56, inverted.pitches().get(0).midi()); // Ab3
        assertEquals(60, inverted.pitches().get(1).midi()); // C4
    }
}
