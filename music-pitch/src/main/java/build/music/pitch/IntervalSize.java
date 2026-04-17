package build.music.pitch;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;


@Singular
public enum IntervalSize implements Trait {
    UNISON, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, OCTAVE;

    public int number() {
        return ordinal() + 1;
    }

    /**
     * Semitones for the "natural" (perfect or major) version of this interval.
     */
    public int baseSemitones() {
        return switch (this) {
            case UNISON -> 0;
            case SECOND -> 2;
            case THIRD -> 4;
            case FOURTH -> 5;
            case FIFTH -> 7;
            case SIXTH -> 9;
            case SEVENTH -> 11;
            case OCTAVE -> 12;
        };
    }

    /**
     * Number of diatonic steps above unison (0-based).
     */
    public int steps() {
        return ordinal();
    }

    public boolean isPerfectable() {
        return switch (this) {
            case UNISON, FOURTH, FIFTH, OCTAVE -> true;
            default -> false;
        };
    }

    public IntervalSize invert() {
        return switch (this) {
            case UNISON -> OCTAVE;
            case SECOND -> SEVENTH;
            case THIRD -> SIXTH;
            case FOURTH -> FIFTH;
            case FIFTH -> FOURTH;
            case SIXTH -> THIRD;
            case SEVENTH -> SECOND;
            case OCTAVE -> UNISON;
        };
    }

    static {
        Marshalling.registerEnum(IntervalSize.class);
    }
}
