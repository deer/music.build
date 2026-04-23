package build.music.time;

import java.util.Objects;

public record MetricPosition(int measure, Fraction beatOffset) implements Comparable<MetricPosition> {

    public MetricPosition {
        Objects.requireNonNull(beatOffset, "beatOffset must not be null");
        if (measure < 0) {
            throw new IllegalArgumentException("measure must be non-negative, got: " + measure);
        }
    }

    public boolean isDownbeat() {
        return beatOffset.equals(Fraction.ZERO);
    }

    /**
     * Which beat (1-based) this position falls on within the given time signature.
     */
    public int beat(final TimeSignature ts) {
        final Fraction beatUnitFraction = Fraction.of(1, ts.beatUnit());
        // beatOffset is in whole-note fractions; divide by beat-unit fraction to get beat index
        final Fraction beatsFromStart = beatOffset.divide(beatUnitFraction);
        return (int) beatsFromStart.toDouble() + 1;
    }

    @Override
    public int compareTo(final MetricPosition other) {
        final int cmp = Integer.compare(this.measure, other.measure);
        return cmp != 0 ? cmp : this.beatOffset.compareTo(other.beatOffset);
    }
}
