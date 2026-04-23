package build.music.instrument;

import build.music.pitch.SpelledPitch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PitchRangeTests {

    @Test
    void contains() {
        PitchRange range = PitchRange.of(SpelledPitch.parse("C4"), SpelledPitch.parse("C5"));
        assertTrue(range.contains(SpelledPitch.parse("C4")));
        assertTrue(range.contains(SpelledPitch.parse("G4")));
        assertTrue(range.contains(SpelledPitch.parse("C5")));
        assertFalse(range.contains(SpelledPitch.parse("B3")));
        assertFalse(range.contains(SpelledPitch.parse("D5")));
    }

    @Test
    void isComfortable() {
        PitchRange range = PitchRange.of(SpelledPitch.parse("C4"), SpelledPitch.parse("C7"));
        assertTrue(range.isComfortable(SpelledPitch.parse("G4")));
        // Bottom extreme should be uncomfortable
        assertFalse(range.isComfortable(SpelledPitch.parse("C4")));
        assertFalse(range.isComfortable(SpelledPitch.parse("D4")));
    }

    @Test
    void semitoneSpan() {
        PitchRange range = PitchRange.of(SpelledPitch.parse("C4"), SpelledPitch.parse("C5"));
        assertEquals(12, range.semitoneSpan());
    }

    @Test
    void lowMustBeBeforeHigh() {
        assertThrows(IllegalArgumentException.class, () ->
            PitchRange.of(SpelledPitch.parse("C5"), SpelledPitch.parse("C4")));
    }
}
