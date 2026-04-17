package build.music.pitch;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpelledIntervalTests {

    @Test
    void minorThird_invert_givesMajorSixth() {
        var m3 = SpelledInterval.of(IntervalQuality.MINOR, IntervalSize.THIRD);
        var inverted = m3.invert();
        assertEquals(SpelledInterval.of(IntervalQuality.MAJOR, IntervalSize.SIXTH), inverted);
    }

    @Test
    void perfectFifth_semitones_seven() {
        assertEquals(7, SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.FIFTH).semitones());
    }

    @Test
    void majorThird_semitones_four() {
        assertEquals(4, SpelledInterval.of(IntervalQuality.MAJOR, IntervalSize.THIRD).semitones());
    }

    @Test
    void minorThird_semitones_three() {
        assertEquals(3, SpelledInterval.of(IntervalQuality.MINOR, IntervalSize.THIRD).semitones());
    }

    @Test
    void augmentedFourth_semitones_six() {
        assertEquals(6, SpelledInterval.of(IntervalQuality.AUGMENTED, IntervalSize.FOURTH).semitones());
    }

    @Test
    void diminishedFourth_semitones_four() {
        // diminished 4th = perfect 4th (5) - 1 = 4 (sounds like major 3rd)
        assertEquals(4, SpelledInterval.of(IntervalQuality.DIMINISHED, IntervalSize.FOURTH).semitones());
    }

    @Test
    void perfectThird_invalid() {
        assertThrows(IllegalArgumentException.class, () ->
            SpelledInterval.of(IntervalQuality.PERFECT, IntervalSize.THIRD));
    }

    @Test
    void parse_roundTrips() {
        for (var s : List.of("m3", "P5", "A4", "d7", "M2", "P1", "M7")) {
            assertEquals(s, SpelledInterval.parse(s).toString(), "Round-trip failed: " + s);
        }
    }
}
