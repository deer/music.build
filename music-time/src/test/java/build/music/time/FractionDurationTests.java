package build.music.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractionDurationTests {

    @Test
    void fractionRoundTrips() {
        Fraction f = Fraction.of(3, 8);
        FractionDuration fd = new FractionDuration(f);
        assertEquals(f, fd.fraction());
    }

    @Test
    void absoluteDurationAtTempo() {
        // 1/4 note at 120 BPM = 500ms
        FractionDuration fd = new FractionDuration(Fraction.QUARTER);
        Tempo tempo = Tempo.of(120);
        Duration abs = fd.absolute(tempo);
        assertEquals(500, abs.toMillis());
    }

    @Test
    void compareToSmallerFraction() {
        FractionDuration eighth = new FractionDuration(Fraction.of(1, 8));
        FractionDuration quarter = new FractionDuration(Fraction.QUARTER);
        assertTrue(eighth.compareTo(quarter) < 0);
    }
}
