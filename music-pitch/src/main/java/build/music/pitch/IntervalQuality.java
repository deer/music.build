package build.music.pitch;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;


@Singular
public enum IntervalQuality implements Trait {
    DIMINISHED, MINOR, PERFECT, MAJOR, AUGMENTED;

    public IntervalQuality invert() {
        return switch (this) {
            case DIMINISHED -> AUGMENTED;
            case MINOR -> MAJOR;
            case PERFECT -> PERFECT;
            case MAJOR -> MINOR;
            case AUGMENTED -> DIMINISHED;
        };
    }

    public boolean isValidFor(final IntervalSize size) {
        return switch (this) {
            case PERFECT -> size.isPerfectable();
            case MAJOR, MINOR -> !size.isPerfectable();
            case DIMINISHED, AUGMENTED -> true;
        };
    }

    public String symbol() {
        return switch (this) {
            case DIMINISHED -> "d";
            case MINOR -> "m";
            case PERFECT -> "P";
            case MAJOR -> "M";
            case AUGMENTED -> "A";
        };
    }

    static {
        Marshalling.registerEnum(IntervalQuality.class);
    }
}
