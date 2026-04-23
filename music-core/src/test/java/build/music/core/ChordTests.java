package build.music.core;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.PitchClass;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordTests {

    @Test
    void chord_sortsPitchesLowToHigh() {
        var e4 = SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4);
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var g4 = SpelledPitch.of(NoteName.G, Accidental.NATURAL, 4);
        var chord = Chord.of(List.of(e4, c4, g4), RhythmicValue.WHOLE, Velocity.MF);
        assertEquals(c4, chord.root());
        assertEquals(List.of(c4, e4, g4), chord.pitches());
    }

    @Test
    void chord_requiresAtLeastTwoPitches() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        assertThrows(IllegalArgumentException.class,
            () -> Chord.of(List.of(c4), RhythmicValue.WHOLE, Velocity.MF));
    }

    @Test
    void chord_containsPitchClass() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var e4 = SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4);
        var g4 = SpelledPitch.of(NoteName.G, Accidental.NATURAL, 4);
        var chord = Chord.of(List.of(c4, e4, g4), RhythmicValue.WHOLE, Velocity.MF);
        assertTrue(chord.contains(PitchClass.C));
        assertTrue(chord.contains(PitchClass.E));
        assertTrue(chord.contains(PitchClass.G));
        assertFalse(chord.contains(PitchClass.D));
    }
}
