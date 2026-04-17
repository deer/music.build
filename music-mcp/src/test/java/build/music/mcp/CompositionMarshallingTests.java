package build.music.mcp;

import build.music.core.Articulation;
import build.music.core.ChordQuality;
import build.music.core.ChordSymbol;
import build.music.core.Velocity;
import build.music.mcp.tools.CreateNoteTools;
import build.music.mcp.tools.HarmonyTools;
import build.music.mcp.tools.SaveLoadTools;
import build.music.mcp.tools.ScoreTools;
import build.music.mcp.tools.VoiceTools;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Round-trip test: builds a non-trivial {@link CompositionSnapshot}, marshals it to JSON via
 * {@code configuredTransport}, reads it back, unmarshals, and asserts structural equality.
 *
 * <p>This validates the codemodel marshalling path introduced in Phase 5 of the
 * codemodel reframing.
 */
class CompositionMarshallingTests {

    @Test
    void roundTripsNonTrivialSnapshot() throws Exception {
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {
            // Two voices
            VoiceTools.createVoice(ctx, "melody", "C4/q E4/q G4/q C5/h");
            VoiceTools.createVoice(ctx, "bass", "C2/h G2/q C2/q");

            // Key + metadata
            ScoreTools.setMetadata(ctx, "Round Trip Test", null, null);
            HarmonyTools.setKey(ctx, "C major");

            // Swing ratio
            ctx.setSwingRatio(new Fraction(2, 3));

            // Bar chords
            ctx.setBarChords(Map.of(
                1, ChordSymbol.of(NoteName.C, Accidental.NATURAL, ChordQuality.MAJOR),
                2, ChordSymbol.of(NoteName.G, Accidental.NATURAL, ChordQuality.DOMINANT_7)
            ));

            // Tempo change
            ctx.addTempoChange(3, 4, 80, "linear");

            // Dynamics and articulation (baked into notes in snapshot)
            ctx.setDynamics("melody", Velocity.MF);
            ctx.setArticulation("melody", Articulation.STACCATO);

            // Motif
            ctx.saveMotif("opening", CreateNoteTools.parseNoteSequence("E4/q D4/q C4/h"));
        });

        // ── Snapshot → JSON → Snapshot ────────────────────────────────────────
        CompositionSnapshot original = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        String json = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.marshalToJson(original, ctx));

        assertFalse(json.isBlank(), "marshalled JSON must not be empty");

        CompositionSnapshot restored = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.unmarshalFromJson(json, ctx));

        // ── Assert structural equality ─────────────────────────────────────────
        assertEquals(original.score().title(), restored.score().title(), "title must round-trip");
        assertEquals(original.score().scoreParts().size(), restored.score().scoreParts().size(), "part count must round-trip");
        assertEquals(original.score().tempo(), restored.score().tempo(), "tempo must round-trip");
        assertEquals(original.motifs().size(), restored.motifs().size(), "motif count must round-trip");
    }

    @Test
    void snapshotPreservesVoiceCount() {
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {
            VoiceTools.createVoice(ctx, "v1", "C4/q D4/q E4/q F4/q");
            VoiceTools.createVoice(ctx, "v2", "G3/h E3/h");
            VoiceTools.createVoice(ctx, "v3", "r/w");
        });

        CompositionSnapshot snap = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        assertEquals(3, snap.score().scoreParts().size());
        assertEquals(CompositionSnapshot.SCHEMA_VERSION_STRING, snap.schemaVersion().get());
    }

    @Test
    void snapshotRestoreRoundTrip() {
        CompositionContext original = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, original.codeModel()).run(() -> {
            VoiceTools.createVoice(original, "piano", "C4/q E4/q G4/q");
            ScoreTools.setMetadata(original, "Restore Test", 120, null);
            HarmonyTools.setKey(original, "G major");
        });

        CompositionSnapshot snap = ScopedValue.where(MusicCodeModel.CURRENT, original.codeModel())
            .call(original::snapshot);

        CompositionContext restored = new CompositionContext();
        ScopedValue.where(MusicCodeModel.CURRENT, restored.codeModel()).run(() -> {
            restored.restoreFrom(snap);
        });

        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getTempo().bpm(), restored.getTempo().bpm());
        assertEquals(original.voiceNames(), restored.voiceNames());
    }

    @Test
    void emptySnapshotRoundTrips() throws Exception {
        CompositionContext ctx = new CompositionContext();
        CompositionSnapshot original = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        String json = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.marshalToJson(original, ctx));
        CompositionSnapshot restored = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.unmarshalFromJson(json, ctx));
        assertEquals(original.score().title(), restored.score().title());
        assertEquals(original.score().scoreParts().size(), restored.score().scoreParts().size());
        assertEquals(original.schemaVersion(), restored.schemaVersion());
    }

    @Test
    void tempoChangesRoundTrip() throws Exception {
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {
            VoiceTools.createVoice(ctx, "v", "C4/q D4/q E4/q F4/q");
            ctx.addTempoChange(2, 4, 80, "linear");
            ctx.addTempoChange(6, 8, 120, "exponential");
        });

        CompositionSnapshot original = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        assertEquals(2, original.score().tempoChanges().size());

        String json = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.marshalToJson(original, ctx));
        CompositionSnapshot restored = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.unmarshalFromJson(json, ctx));

        assertEquals(original.score().tempoChanges(), restored.score().tempoChanges());
    }

    @Test
    void dottedValueRoundTrips() throws Exception {
        // Regression: DottedValue duration must survive marshal/unmarshal without ClassCastException
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {
            // C4 dotted quarter (3/8), D4 dotted half (3/4), rest dotted eighth (3/16)
            var c4dotted = build.music.core.Note.of(
                build.music.pitch.SpelledPitch.of(build.music.pitch.NoteName.C, build.music.pitch.Accidental.NATURAL, 4),
                new build.music.time.DottedValue(build.music.time.RhythmicValue.QUARTER, 1));
            var d4dotted = build.music.core.Note.of(
                build.music.pitch.SpelledPitch.of(build.music.pitch.NoteName.D, build.music.pitch.Accidental.NATURAL, 4),
                new build.music.time.DottedValue(build.music.time.RhythmicValue.HALF, 1));
            var restDotted = build.music.core.Rest.of(
                new build.music.time.DottedValue(build.music.time.RhythmicValue.EIGHTH, 1));
            ctx.createVoice("dotted", java.util.List.of(c4dotted, d4dotted, restDotted));
        });

        CompositionSnapshot original = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        String json = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.marshalToJson(original, ctx));
        CompositionSnapshot restored = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.unmarshalFromJson(json, ctx));

        var restoredVoice = restored.score().part("dotted").orElseThrow().voice();
        assertEquals(3, restoredVoice.events().size());
        // Check durations survived round-trip
        assertEquals(build.music.time.Fraction.of(3, 8), restoredVoice.events().get(0).duration().fraction());
        assertEquals(build.music.time.Fraction.of(3, 4), restoredVoice.events().get(1).duration().fraction());
        assertEquals(build.music.time.Fraction.of(3, 16), restoredVoice.events().get(2).duration().fraction());
    }

    @Test
    void tupletRoundTrips() throws Exception {
        // Regression: Tuplet duration must survive marshal/unmarshal
        CompositionContext ctx = new CompositionContext();

        ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel()).run(() -> {
            // triplet quarter (3-in-2: each note = 2/3 of a quarter = 1/6 whole)
            var tuplet = new build.music.time.Tuplet(3, 2, build.music.time.RhythmicValue.QUARTER);
            var n1 = build.music.core.Note.of(
                build.music.pitch.SpelledPitch.of(build.music.pitch.NoteName.C, build.music.pitch.Accidental.NATURAL, 4), tuplet);
            var n2 = build.music.core.Note.of(
                build.music.pitch.SpelledPitch.of(build.music.pitch.NoteName.D, build.music.pitch.Accidental.NATURAL, 4), tuplet);
            var n3 = build.music.core.Note.of(
                build.music.pitch.SpelledPitch.of(build.music.pitch.NoteName.E, build.music.pitch.Accidental.NATURAL, 4), tuplet);
            ctx.createVoice("triplets", java.util.List.of(n1, n2, n3));
        });

        CompositionSnapshot original = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(ctx::snapshot);
        String json = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.marshalToJson(original, ctx));
        CompositionSnapshot restored = ScopedValue.where(MusicCodeModel.CURRENT, ctx.codeModel())
            .call(() -> SaveLoadTools.unmarshalFromJson(json, ctx));

        var restoredVoice = restored.score().part("triplets").orElseThrow().voice();
        assertEquals(3, restoredVoice.events().size());
        // Each triplet quarter = 2/3 * 1/4 = 1/6 of a whole note
        build.music.time.Fraction expected = build.music.time.Fraction.of(1, 6);
        assertEquals(expected, restoredVoice.events().get(0).duration().fraction());
        assertEquals(expected, restoredVoice.events().get(1).duration().fraction());
        assertEquals(expected, restoredVoice.events().get(2).duration().fraction());
    }
}
