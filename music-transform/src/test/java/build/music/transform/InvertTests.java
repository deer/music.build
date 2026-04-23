package build.music.transform;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvertTests {

    @Test
    void invert_D4_aroundC4_givesBb3() {
        var axis = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var d4 = SpelledPitch.of(NoteName.D, Accidental.NATURAL, 4);
        var invert = new Invert(axis);
        var result = invert.apply(d4);
        // D4 is 2 semitones above C4, so result should be 2 semitones below C4 = Bb3 (MIDI 58)
        assertEquals(58, result.midi());
    }

    @Test
    void invert_axis_givesAxis() {
        var axis = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var invert = new Invert(axis);
        assertEquals(axis.midi(), invert.apply(axis).midi());
    }
}
