package build.music.core;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.IntervalQuality;
import build.music.pitch.IntervalSize;
import build.music.pitch.SpelledInterval;

import java.util.List;

@Singular
public enum ChordQuality implements Trait {
    MAJOR, MINOR, DIMINISHED, AUGMENTED,
    DOMINANT_7, MAJOR_7, MINOR_7,
    HALF_DIMINISHED_7, DIMINISHED_7, MINOR_MAJOR_7, AUGMENTED_MAJOR_7,
    SUSPENDED_2, SUSPENDED_4;

    public List<SpelledInterval> intervals() {
        return switch (this) {
            case MAJOR -> List.of(P(1), M(3), P(5));
            case MINOR -> List.of(P(1), m(3), P(5));
            case DIMINISHED -> List.of(P(1), m(3), d(5));
            case AUGMENTED -> List.of(P(1), M(3), A(5));
            case DOMINANT_7 -> List.of(P(1), M(3), P(5), m(7));
            case MAJOR_7 -> List.of(P(1), M(3), P(5), M(7));
            case MINOR_7 -> List.of(P(1), m(3), P(5), m(7));
            case HALF_DIMINISHED_7 -> List.of(P(1), m(3), d(5), m(7));
            case DIMINISHED_7 -> List.of(P(1), m(3), d(5), d(7));
            case MINOR_MAJOR_7 -> List.of(P(1), m(3), P(5), M(7));
            case AUGMENTED_MAJOR_7 -> List.of(P(1), M(3), A(5), M(7));
            case SUSPENDED_2 -> List.of(P(1), M(2), P(5));
            case SUSPENDED_4 -> List.of(P(1), P(4), P(5));
        };
    }

    public String symbol() {
        return switch (this) {
            case MAJOR -> "maj";
            case MINOR -> "m";
            case DIMINISHED -> "dim";
            case AUGMENTED -> "aug";
            case DOMINANT_7 -> "7";
            case MAJOR_7 -> "maj7";
            case MINOR_7 -> "m7";
            case HALF_DIMINISHED_7 -> "m7b5";
            case DIMINISHED_7 -> "dim7";
            case MINOR_MAJOR_7 -> "mMaj7";
            case AUGMENTED_MAJOR_7 -> "augMaj7";
            case SUSPENDED_2 -> "sus2";
            case SUSPENDED_4 -> "sus4";
        };
    }

    // ── interval builder shorthands (called inside ScopedValue scope) ─────────

    private static SpelledInterval P(final int n) {
        return SpelledInterval.of(IntervalQuality.PERFECT, size(n));
    }

    private static SpelledInterval M(final int n) {
        return SpelledInterval.of(IntervalQuality.MAJOR, size(n));
    }

    private static SpelledInterval m(final int n) {
        return SpelledInterval.of(IntervalQuality.MINOR, size(n));
    }

    private static SpelledInterval A(final int n) {
        return SpelledInterval.of(IntervalQuality.AUGMENTED, size(n));
    }

    private static SpelledInterval d(final int n) {
        return SpelledInterval.of(IntervalQuality.DIMINISHED, size(n));
    }

    private static IntervalSize size(final int n) {
        return switch (n) {
            case 1 -> IntervalSize.UNISON;
            case 2 -> IntervalSize.SECOND;
            case 3 -> IntervalSize.THIRD;
            case 4 -> IntervalSize.FOURTH;
            case 5 -> IntervalSize.FIFTH;
            case 6 -> IntervalSize.SIXTH;
            case 7 -> IntervalSize.SEVENTH;
            case 8 -> IntervalSize.OCTAVE;
            default -> throw new IllegalArgumentException("Unknown interval number: " + n);
        };
    }

    static {
        Marshalling.registerEnum(ChordQuality.class);
    }
}
