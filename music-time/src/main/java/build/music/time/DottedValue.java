package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A dotted rhythmic value. One dot = 3/2 × base, two dots = 7/4 × base, three dots = 15/8 × base.
 */
public record DottedValue(RhythmicValue base, int dots) implements Duration {

    @Unmarshal
    public DottedValue {
        Objects.requireNonNull(base, "base must not be null");
        if (dots < 1 || dots > 3) {
            throw new IllegalArgumentException("dots must be 1-3, got: " + dots);
        }
    }

    @Marshal
    public void destructor(final Out<RhythmicValue> base, final Out<Integer> dots) {
        base.set(this.base);
        dots.set(this.dots);
    }

    @Override
    public Fraction fraction() {
        // fraction = base * (2 - 1/2^dots) = base * (2^dots*2 - 1) / 2^dots... simplify:
        // 1 dot: base * 3/2
        // 2 dots: base * 7/4
        // 3 dots: base * 15/8
        final int multiplier = (1 << (dots + 1)) - 1; // 3, 7, 15
        final int divisor = 1 << dots;                  // 2, 4, 8
        return base.fraction().multiply(Fraction.of(multiplier, divisor));
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return tempo.durationOf(fraction());
    }

    static {
        Marshalling.register(DottedValue.class, MethodHandles.lookup());
    }
}
