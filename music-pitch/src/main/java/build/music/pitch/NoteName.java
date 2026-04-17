package build.music.pitch;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

@Singular
public enum NoteName implements Trait {
    C, D, E, F, G, A, B;

    public int semitonesAboveC() {
        return switch (this) {
            case C -> 0;
            case D -> 2;
            case E -> 4;
            case F -> 5;
            case G -> 7;
            case A -> 9;
            case B -> 11;
        };
    }

    public NoteName next() {
        return values()[(ordinal() + 1) % 7];
    }

    public NoteName previous() {
        return values()[(ordinal() + 6) % 7];
    }

    /**
     * Steps above this note name (0-6).
     */
    public NoteName steps(final int n) {
        return values()[((ordinal() + n) % 7 + 7) % 7];
    }

    static {
        Marshalling.registerEnum(NoteName.class);
    }
}
