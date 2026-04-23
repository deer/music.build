package build.music.core;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.NonSingular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Articulation as a non-singular trait — a note may carry multiple articulations simultaneously.
 */
@NonSingular
public enum Articulation implements Trait {
    NORMAL, STACCATO, STACCATISSIMO, TENUTO, ACCENT, MARCATO, LEGATO, PORTATO;

    /**
     * Fraction of the notated duration the note actually sounds.
     */
    public double durationFactor() {
        return switch (this) {
            case NORMAL -> 0.9;
            case STACCATO -> 0.5;
            case STACCATISSIMO -> 0.25;
            case TENUTO -> 1.0;
            case ACCENT -> 0.9;
            case MARCATO -> 0.75;
            case LEGATO -> 1.0;
            case PORTATO -> 0.75;
        };
    }

    static {
        Marshalling.registerEnum(Articulation.class);
    }
}
