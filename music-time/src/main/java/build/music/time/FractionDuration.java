package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A Duration backed by an arbitrary Fraction, for durations that don't map to a standard RhythmicValue.
 * Used by MidiReader and AbcReader when tick or beam durations don't snap to a named value.
 */
public record FractionDuration(Fraction fraction) implements Duration {

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
