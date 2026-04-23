package build.music.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationTests {

    @Test
    void whole_isOneWholeNote() {
        assertEquals(Fraction.ONE, RhythmicValue.WHOLE.fraction());
    }

    @Test
    void quarter_isOneQuarter() {
        assertEquals(Fraction.QUARTER, RhythmicValue.QUARTER.fraction());
    }

    @Test
    void dottedQuarter_isThreeEighths() {
        var dq = new DottedValue(RhythmicValue.QUARTER, 1);
        assertEquals(Fraction.of(3, 8), dq.fraction());
    }

    @Test
    void doubleDottedQuarter_isSevenSixteenths() {
        var ddq = new DottedValue(RhythmicValue.QUARTER, 2);
        assertEquals(Fraction.of(7, 16), ddq.fraction());
    }

    @Test
    void tripletEighth_isOneTwelfth() {
        var triplet = Tuplet.triplet(RhythmicValue.EIGHTH);
        assertEquals(Fraction.of(1, 12), triplet.fraction());
    }

    @Test
    void quarterAt120bpm_is500ms() {
        var tempo = Tempo.of(120);
        var duration = RhythmicValue.QUARTER.absolute(tempo);
        assertEquals(500, duration.toMillis());
    }

    @Test
    void dots_zero_throws() {
        assertThrows(IllegalArgumentException.class, () -> new DottedValue(RhythmicValue.QUARTER, 0));
    }

    @Test
    void dots_four_throws() {
        assertThrows(IllegalArgumentException.class, () -> new DottedValue(RhythmicValue.QUARTER, 4));
    }
}
