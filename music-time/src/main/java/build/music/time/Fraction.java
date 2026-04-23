package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

public record Fraction(int numerator, int denominator) implements Comparable<Fraction> {

    public static final Fraction ZERO = new Fraction(0, 1);
    public static final Fraction ONE = new Fraction(1, 1);
    public static final Fraction HALF = new Fraction(1, 2);
    public static final Fraction QUARTER = new Fraction(1, 4);
    public static final Fraction EIGHTH = new Fraction(1, 8);

    @Unmarshal
    public Fraction {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator must not be zero");
        }
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }
        final int g = gcd(Math.abs(numerator), denominator);
        numerator /= g;
        denominator /= g;
    }

    @Marshal
    public void destructor(final Out<Integer> numerator, final Out<Integer> denominator) {
        numerator.set(this.numerator);
        denominator.set(this.denominator);
    }

    public static Fraction of(final int numerator, final int denominator) {
        return new Fraction(numerator, denominator);
    }

    private static int gcd(final int a, final int b) {
        int aCopy = a;
        int bCopy = b;
        while (bCopy != 0) {
            final int t = bCopy;
            bCopy = aCopy % bCopy;
            aCopy = t;
        }
        return aCopy;
    }

    public Fraction add(final Fraction other) {
        return new Fraction(numerator * other.denominator + other.numerator * denominator,
            denominator * other.denominator);
    }

    public Fraction subtract(final Fraction other) {
        return new Fraction(numerator * other.denominator - other.numerator * denominator,
            denominator * other.denominator);
    }

    public Fraction multiply(final Fraction other) {
        return new Fraction(numerator * other.numerator, denominator * other.denominator);
    }

    public Fraction multiply(final int scalar) {
        return new Fraction(numerator * scalar, denominator);
    }

    public Fraction divide(final Fraction other) {
        return new Fraction(numerator * other.denominator, denominator * other.numerator);
    }

    public Fraction divide(final int scalar) {
        return new Fraction(numerator, denominator * scalar);
    }

    public double toDouble() {
        return (double) numerator / denominator;
    }

    @Override
    public int compareTo(final Fraction other) {
        return Integer.compare(numerator * other.denominator, other.numerator * denominator);
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    static {
        Marshalling.register(Fraction.class, MethodHandles.lookup());
    }
}
