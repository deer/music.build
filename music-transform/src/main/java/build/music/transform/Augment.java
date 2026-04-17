package build.music.transform;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.time.Duration;
import build.music.time.Fraction;

import java.util.List;
import java.util.Objects;

/**
 * Scales all durations in a melodic sequence by a rational factor.
 * Use factor greater than 1 to augment (lengthen), factor less than 1 to diminish (shorten).
 */
public record Augment(Fraction factor) implements MelodicTransform {

    public Augment {
        Objects.requireNonNull(factor, "factor must not be null");
        if (factor.numerator() <= 0) {
            throw new IllegalArgumentException("factor must be positive");
        }
    }

    public static Augment byFactor(final int numerator, final int denominator) {
        return new Augment(Fraction.of(numerator, denominator));
    }

    public static Augment doubling() {
        return new Augment(Fraction.of(2, 1));
    }

    public static Augment tripling() {
        return new Augment(Fraction.of(3, 1));
    }

    public static Augment halving() {
        return new Augment(Fraction.of(1, 2));
    }

    @Override
    public List<NoteEvent> apply(final List<NoteEvent> input) {
        return input.stream()
            .map(event -> scaleDuration(event, factor))
            .toList();
    }

    private static NoteEvent scaleDuration(final NoteEvent event, final Fraction factor) {
        final Duration scaled = scaledDuration(event.duration(), factor);
        return switch (event) {
            case Note n -> Note.of(n.pitch(), scaled, n.velocity(), n.articulation(), n.tied());
            case Rest r -> Rest.of(scaled);
            case Chord c -> Chord.of(c.pitches(), scaled, c.velocity());
        };
    }

    private static Duration scaledDuration(final Duration original, final Fraction factor) {
        final Fraction newFraction = original.fraction().multiply(factor);
        return new ScaledDuration(newFraction);
    }
}
