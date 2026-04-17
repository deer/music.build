package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Tempo as a singular trait — a score or voice carries at most one default tempo.
 */
@Singular
public record Tempo(int bpm, RhythmicValue beatUnit) implements Trait {

    @Unmarshal
    public Tempo {
        Objects.requireNonNull(beatUnit, "beatUnit must not be null");
        if (bpm <= 0 || bpm > 400) {
            throw new IllegalArgumentException("bpm must be 1-400, got: " + bpm);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> bpm, final Out<RhythmicValue> beatUnit) {
        bpm.set(this.bpm);
        beatUnit.set(this.beatUnit);
    }

    public static Tempo of(final int bpm) {
        return new Tempo(bpm, RhythmicValue.QUARTER);
    }

    /**
     * Wall-clock duration of one beat.
     */
    public java.time.Duration beatDuration() {
        return java.time.Duration.ofNanos((long) (60_000_000_000.0 / bpm));
    }

    /**
     * Convert a musical duration (as fraction of a whole note) to wall-clock time.
     * One beat = beatUnit.fraction() of a whole note.
     */
    public java.time.Duration durationOf(final Fraction musicalFraction) {
        // musicalFraction / beatUnit.fraction() gives number of beats
        final Fraction beats = musicalFraction.divide(beatUnit.fraction());
        final long nanos = (long) (beats.toDouble() * 60_000_000_000.0 / bpm);
        return java.time.Duration.ofNanos(nanos);
    }

    static {
        Marshalling.register(Tempo.class, MethodHandles.lookup());
    }
}
