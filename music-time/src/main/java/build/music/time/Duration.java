package build.music.time;

import build.codemodel.foundation.descriptor.Trait;

/**
 * Musical duration — implements {@link Trait} so that duration values (RhythmicValue, DottedValue,
 * Tuplet, ScaledDuration, FractionDuration) slot directly into the trait map of note-event Traitables.
 * A note event carries exactly one Duration trait (@Singular enforced at the concrete type level).
 *
 * <p>Not sealed: {@code ScaledDuration} lives in {@code music-transform} and {@code FractionDuration}
 * lives in {@code music-midi} — Java sealed interfaces cannot have permitted types in a different
 * JPMS module.
 */
public interface Duration extends Trait {
    /** Duration as a fraction of a whole note. */
    Fraction fraction();

    /** Wall-clock duration at the given tempo. */
    java.time.Duration absolute(Tempo tempo);

    default int compareTo(final Duration other) {
        return this.fraction().compareTo(other.fraction());
    }
}
