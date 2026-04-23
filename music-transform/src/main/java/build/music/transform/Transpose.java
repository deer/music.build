package build.music.transform;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;

import java.util.Objects;

public final class Transpose implements PitchTransform {

    private final SpelledInterval interval;
    private final boolean ascending;

    private Transpose(final SpelledInterval interval, final boolean ascending) {
        Objects.requireNonNull(interval, "interval must not be null");
        this.interval = interval;
        this.ascending = ascending;
    }

    /**
     * Transpose upward by the given interval.
     */
    public Transpose(final SpelledInterval interval) {
        this(interval, true);
    }

    public SpelledInterval interval() {
        return interval;
    }

    @Override
    public Pitch apply(final Pitch input) {
        final SpelledPitch spelled = input.spelled();
        if (ascending) {
            return spelled.transpose(interval);
        } else {
            return transposeDown(spelled, interval);
        }
    }

    /**
     * Returns a Transpose that undoes this one.
     */
    public Transpose inverse() {
        return new Transpose(interval, !ascending);
    }

    public static Transpose up(final SpelledInterval interval) {
        return new Transpose(interval, true);
    }

    public static Transpose down(final SpelledInterval interval) {
        return new Transpose(interval, false);
    }

    private static SpelledPitch transposeDown(final SpelledPitch pitch, final SpelledInterval interval) {
        final int targetMidi = pitch.midi() - interval.semitones();
        final int nameStepsDelta = interval.size().steps();
        final int newNameOrdinal = ((pitch.name().ordinal() - nameStepsDelta) % 7 + 7) % 7;
        final NoteName newName = NoteName.values()[newNameOrdinal];
        // Try candidate octaves
        for (int oct = pitch.octave() - 2; oct <= pitch.octave() + 1; oct++) {
            final int accOffset = targetMidi - (oct + 1) * 12 - newName.semitonesAboveC();
            if (accOffset >= -2 && accOffset <= 2) {
                try {
                    return SpelledPitch.of(newName, Accidental.fromOffset(accOffset), oct);
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        throw new IllegalArgumentException(
            "Cannot transpose " + pitch + " down by " + interval);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transpose t)) {
            return false;
        }
        return ascending == t.ascending && interval.equals(t.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, ascending);
    }

    @Override
    public String toString() {
        return (ascending ? "Transpose[up " : "Transpose[down ") + interval + "]";
    }
}
