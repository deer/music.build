package build.music.pitch;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpelledPitchTests {

    @Test
    void c4_hasMidiNumber60() {
        assertEquals(60, SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4).midi());
    }

    @Test
    void a4_hasMidiNumber69() {
        assertEquals(69, SpelledPitch.of(NoteName.A, Accidental.NATURAL, 4).midi());
    }

    @Test
    void cSharp4_andDb4_sameMidi_notEqual() {
        var cSharp4 = SpelledPitch.of(NoteName.C, Accidental.SHARP, 4);
        var dFlat4 = SpelledPitch.of(NoteName.D, Accidental.FLAT, 4);
        assertEquals(61, cSharp4.midi());
        assertEquals(61, dFlat4.midi());
        assertNotEquals(cSharp4, dFlat4);
    }

    @Test
    void transpose_C4_upMajorThird_givesE4() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var result = c4.transpose(SpelledInterval.of(IntervalQuality.MAJOR, IntervalSize.THIRD));
        assertEquals(SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4), result);
    }

    @Test
    void transpose_C4_upDiminishedFourth_givesFb4() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var result = c4.transpose(SpelledInterval.of(IntervalQuality.DIMINISHED, IntervalSize.FOURTH));
        assertEquals(SpelledPitch.of(NoteName.F, Accidental.FLAT, 4), result);
        // Fb4 has same MIDI as E4 but different spelling
        assertEquals(SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4).midi(), result.midi());
    }

    @Test
    void intervalTo_C4_toE4_isMajorThird() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var e4 = SpelledPitch.of(NoteName.E, Accidental.NATURAL, 4);
        assertEquals(SpelledInterval.of(IntervalQuality.MAJOR, IntervalSize.THIRD), c4.intervalTo(e4));
    }

    @Test
    void intervalTo_C4_toEb4_isMinorThird() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var eb4 = SpelledPitch.of(NoteName.E, Accidental.FLAT, 4);
        assertEquals(SpelledInterval.of(IntervalQuality.MINOR, IntervalSize.THIRD), c4.intervalTo(eb4));
    }

    @Test
    void parse_roundTrips() {
        for (var pitch : List.of("C4", "C#4", "Bb3", "D##5", "Ebb2", "A4", "G#5")) {
            var parsed = SpelledPitch.parse(pitch);
            assertEquals(pitch, parsed.toString(), "Round-trip failed for: " + pitch);
        }
    }

    @Test
    void a4_frequency_440Hz_standardTuning() {
        var a4 = SpelledPitch.of(NoteName.A, Accidental.NATURAL, 4);
        assertEquals(440.0, a4.frequency(EqualTemperament.standard()), 0.001);
    }

    @Test
    void toString_compact() {
        assertEquals("C#4", SpelledPitch.of(NoteName.C, Accidental.SHARP, 4).toString());
        assertEquals("Bb3", SpelledPitch.of(NoteName.B, Accidental.FLAT, 3).toString());
        assertEquals("D5", SpelledPitch.of(NoteName.D, Accidental.NATURAL, 5).toString());
    }
}
