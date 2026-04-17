package build.music.time;

import build.base.marshalling.Marshalling;

public enum RhythmicValue implements Duration {
    WHOLE, HALF, QUARTER, EIGHTH, SIXTEENTH, THIRTY_SECOND, SIXTY_FOURTH;

    @Override
    public Fraction fraction() {
        return Fraction.of(1, 1 << ordinal()); // 1/1, 1/2, 1/4, 1/8, ...
    }

    public RhythmicValue subdivide() {
        if (this == SIXTY_FOURTH) {
            throw new IllegalStateException("Cannot subdivide SIXTY_FOURTH");
        }
        return values()[ordinal() + 1];
    }

    public RhythmicValue augment() {
        if (this == WHOLE) {
            throw new IllegalStateException("Cannot augment WHOLE");
        }
        return values()[ordinal() - 1];
    }

    @Override
    public java.time.Duration absolute(final Tempo tempo) {
        return tempo.durationOf(fraction());
    }

    public String symbol() {
        return switch (this) {
            case WHOLE -> "w";
            case HALF -> "h";
            case QUARTER -> "q";
            case EIGHTH -> "e";
            case SIXTEENTH -> "s";
            case THIRTY_SECOND -> "t";
            case SIXTY_FOURTH -> "x";
        };
    }

    static {
        Marshalling.registerEnum(RhythmicValue.class);
    }
}
