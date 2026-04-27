package build.music.time;

import build.codemodel.foundation.descriptor.Singular;

/**
 * A zero-length duration. Used by instantaneous events (ControlChange, ProgramChange)
 * that occupy a point in time without advancing the tick cursor.
 */
@Singular
public record ZeroDuration() implements Duration {

    public static final ZeroDuration INSTANCE = new ZeroDuration();

    @Override
    public Fraction fraction() {
        return Fraction.ZERO;
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return java.time.Duration.ZERO;
    }
}
