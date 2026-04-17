package build.music.transform;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.time.Duration;
import build.music.time.Fraction;
import build.music.time.Tempo;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A duration defined purely by its rational fraction of a whole note.
 */
record ScaledDuration(Fraction fraction) implements Duration {

    @Unmarshal
    ScaledDuration {
        Objects.requireNonNull(fraction, "fraction must not be null");
        if (fraction.numerator() <= 0) {
            throw new IllegalArgumentException("duration fraction must be positive");
        }
    }

    @Marshal
    public void destructor(final Out<Fraction> fraction) {
        fraction.set(this.fraction);
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return tempo.durationOf(fraction);
    }

    @Override
    public String toString() {
        return fraction.toString();
    }

    static {
        Marshalling.register(ScaledDuration.class, MethodHandles.lookup());
    }
}
