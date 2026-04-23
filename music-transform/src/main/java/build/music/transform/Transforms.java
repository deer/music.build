package build.music.transform;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledInterval;

import java.util.List;

/**
 * Static factory and combinator methods for transforms.
 */
public final class Transforms {

    private Transforms() {
    }

    @SafeVarargs
    public static <T> Transform<T> compose(final Transform<T>... transforms) {
        Transform<T> result = Transform.identity();
        for (final Transform<T> t : transforms) {
            result = result.andThen(t);
        }
        return result;
    }

    /**
     * Transpose all pitched events in a sequence.
     */
    public static List<NoteEvent> transposePitches(final List<NoteEvent> events, final SpelledInterval interval) {
        return events.stream()
            .map(event -> switch (event) {
                case Note n -> (NoteEvent) n.transpose(interval);
                case Rest r -> r; // Rest unchanged
                case Chord c -> (NoteEvent) c.transpose(interval);
            })
            .toList();
    }

    /**
     * Retrograde inversion: reverse order then invert pitches around axis.
     */
    public static MelodicTransform retrogradeInversion(final Pitch axis) {
        final Retrograde retrograde = new Retrograde();
        final Invert invert = new Invert(axis);
        return events -> {
            final List<NoteEvent> retro = retrograde.apply(events);
            return retro.stream()
                .map(event -> switch (event) {
                    case Note n ->
                        (NoteEvent) Note.of(invert.apply(n.pitch()), n.duration(), n.velocity(), n.articulation(), n.tied());
                    case build.music.core.Rest r -> r;
                    case Chord c -> (NoteEvent) Chord.of(
                        c.pitches().stream().map(p -> invert.apply(p)).toList(),
                        c.duration(), c.velocity());
                })
                .toList();
        };
    }
}
