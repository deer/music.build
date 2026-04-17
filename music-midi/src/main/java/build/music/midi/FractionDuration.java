package build.music.midi;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.time.Duration;
import build.music.time.Fraction;
import build.music.time.Tempo;

import java.lang.invoke.MethodHandles;

/**
 * Package-private Duration backed by an arbitrary Fraction. Used by MidiReader.
 */
record FractionDuration(Fraction fraction) implements Duration {

    @Unmarshal
    public FractionDuration {
    }

    @Marshal
    public void destructor(final Out<Fraction> fraction) {
        fraction.set(this.fraction);
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return tempo.durationOf(fraction);
    }

    static {
        Marshalling.register(FractionDuration.class, MethodHandles.lookup());
    }
}
