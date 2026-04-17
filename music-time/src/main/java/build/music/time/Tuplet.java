package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A tuplet: {@code actual} notes in the time of {@code normal} notes of {@code unit} value.
 * E.g., triplet eighth: actual=3, normal=2, unit=EIGHTH.
 */
public record Tuplet(int actual, int normal, RhythmicValue unit) implements Duration {

    @Unmarshal
    public Tuplet {
        Objects.requireNonNull(unit, "unit must not be null");
        if (actual <= 0) {
            throw new IllegalArgumentException("actual must be positive, got: " + actual);
        }
        if (normal <= 0) {
            throw new IllegalArgumentException("normal must be positive, got: " + normal);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> actual, final Out<Integer> normal, final Out<RhythmicValue> unit) {
        actual.set(this.actual);
        normal.set(this.normal);
        unit.set(this.unit);
    }

    /** Duration of each note in this tuplet as a fraction of a whole note. */
    @Override
    public Fraction fraction() {
        return unit.fraction().multiply(normal).divide(actual);
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return tempo.durationOf(fraction());
    }

    public static Tuplet triplet(final RhythmicValue unit) {
        return new Tuplet(3, 2, unit);
    }

    static {
        Marshalling.register(Tuplet.class, MethodHandles.lookup());
    }
}
