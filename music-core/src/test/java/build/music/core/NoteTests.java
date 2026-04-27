package build.music.core;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoteTests {

    @Test
    void note_preservesAllFields() {
        var pitch = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var note = Note.of(pitch, RhythmicValue.QUARTER, Velocity.MF, Articulation.NORMAL, false);
        assertEquals(pitch, note.pitch());
        assertEquals(RhythmicValue.QUARTER, note.duration());
        assertEquals(Velocity.MF, note.velocity());
        assertEquals(Articulation.NORMAL, note.articulation());
    }

    @Test
    void noteOf_defaultsToMfAndNormal() {
        var pitch = SpelledPitch.of(NoteName.A, Accidental.NATURAL, 4);
        var note = Note.of(pitch, RhythmicValue.HALF);
        assertEquals(Velocity.MF, note.velocity());
        assertEquals(Articulation.NORMAL, note.articulation());
    }

    @Test
    void note_midi_delegatesToPitch() {
        var pitch = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var note = Note.of(pitch, RhythmicValue.QUARTER);
        assertEquals(60, note.midi());
    }

    @Test
    void rest_hasDurationButNoPitch() {
        var rest = Rest.of(RhythmicValue.QUARTER);
        assertEquals(RhythmicValue.QUARTER, rest.duration());
        assertEquals("rest/q", rest.toString());
    }

    @Test
    void withVelocity_returnsNewNote() {
        var note = Note.of(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), RhythmicValue.QUARTER);
        var louder = note.withVelocity(Velocity.FF);
        assertEquals(Velocity.FF, louder.velocity());
        assertEquals(Velocity.MF, note.velocity()); // original unchanged
    }

    @Test
    void velocity_rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Velocity.of(-1));
        assertThrows(IllegalArgumentException.class, () -> Velocity.of(128));
    }

    @Test
    void noteEvent_sealedType_matchable() {
        NoteEvent event = Note.of(SpelledPitch.of(NoteName.G, Accidental.NATURAL, 4), RhythmicValue.EIGHTH);
        String result = switch (event) {
            case Note n -> "note:" + n.midi();
            case Rest r -> "rest";
            case Chord c -> "chord";
            case ControlChange cc -> "cc";
            case ProgramChange pc -> "pc";
        };
        assertEquals("note:67", result);
    }
}
