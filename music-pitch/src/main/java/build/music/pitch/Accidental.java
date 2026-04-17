package build.music.pitch;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

@Singular
public enum Accidental implements Trait {
    DOUBLE_FLAT, FLAT, NATURAL, SHARP, DOUBLE_SHARP;

    public int semitoneOffset() {
        return switch (this) {
            case DOUBLE_FLAT -> -2;
            case FLAT -> -1;
            case NATURAL -> 0;
            case SHARP -> 1;
            case DOUBLE_SHARP -> 2;
        };
    }

    public String symbol() {
        return switch (this) {
            case DOUBLE_FLAT -> "bb";
            case FLAT -> "b";
            case NATURAL -> "";
            case SHARP -> "#";
            case DOUBLE_SHARP -> "##";
        };
    }

    public static Accidental fromOffset(final int offset) {
        return switch (offset) {
            case -2 -> DOUBLE_FLAT;
            case -1 -> FLAT;
            case 0 -> NATURAL;
            case 1 -> SHARP;
            case 2 -> DOUBLE_SHARP;
            default -> throw new IllegalArgumentException("No accidental for offset: " + offset);
        };
    }

    static {
        Marshalling.registerEnum(Accidental.class);
    }
}
