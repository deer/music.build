package build.music.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeSignatureTests {

    @Test
    void fourFour_measureDuration_isOneWholeNote() {
        assertEquals(Fraction.ONE, TimeSignature.COMMON_TIME.measureDuration());
    }

    @Test
    void threeFour_measureDuration_isThreeQuarters() {
        assertEquals(Fraction.of(3, 4), TimeSignature.WALTZ_TIME.measureDuration());
    }

    @Test
    void sixEight_isCompound() {
        assertTrue(new TimeSignature(6, 8).isCompound());
    }

    @Test
    void fourFour_isSimple() {
        assertTrue(TimeSignature.COMMON_TIME.isSimple());
    }

    @Test
    void nineEight_isCompound() {
        assertTrue(new TimeSignature(9, 8).isCompound());
    }

    @Test
    void invalidBeatUnit_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSignature(4, 3));
    }

    @Test
    void downbeat_isDownbeat() {
        var pos = new MetricPosition(0, Fraction.ZERO);
        assertTrue(pos.isDownbeat());
    }

    @Test
    void nonZeroOffset_isNotDownbeat() {
        var pos = new MetricPosition(0, Fraction.QUARTER);
        assertFalse(pos.isDownbeat());
    }
}
