package build.music.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FractionTests {

    @Test
    void fraction_reducesToLowestTerms() {
        assertEquals(new Fraction(1, 2), new Fraction(2, 4));
    }

    @Test
    void fraction_normalizesNegativeDenominator() {
        assertEquals(new Fraction(-1, 2), new Fraction(1, -2));
    }

    @Test
    void fraction_zeroDenominator_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Fraction(1, 0));
    }

    @Test
    void add_quarterPlusQuarter_equalsHalf() {
        assertEquals(Fraction.HALF, Fraction.QUARTER.add(Fraction.QUARTER));
    }

    @Test
    void subtract() {
        assertEquals(Fraction.QUARTER, Fraction.HALF.subtract(Fraction.QUARTER));
    }

    @Test
    void multiply() {
        assertEquals(Fraction.QUARTER, Fraction.HALF.multiply(Fraction.HALF));
    }

    @Test
    void divide() {
        assertEquals(Fraction.of(2, 1), Fraction.ONE.divide(Fraction.HALF));
    }

    @Test
    void compareTo_ordering() {
        assertTrue(Fraction.QUARTER.compareTo(Fraction.HALF) < 0);
        assertTrue(Fraction.HALF.compareTo(Fraction.QUARTER) > 0);
        assertEquals(0, Fraction.HALF.compareTo(new Fraction(1, 2)));
    }
}
