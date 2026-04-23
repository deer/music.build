package build.music.transform;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.Pitch;
import build.music.pitch.SpelledPitch;

import java.util.Objects;

/**
 * Mirrors a pitch around an axis. If axis is C4, then D4 (a M2 above) maps to Bb3 (a M2 below).
 */
public record Invert(Pitch axis) implements PitchTransform {

    public Invert {
        Objects.requireNonNull(axis, "axis must not be null");
    }

    @Override
    public Pitch apply(final Pitch input) {
        final SpelledPitch axisSpelled = axis.spelled();
        final SpelledPitch inputSpelled = input.spelled();
        final int semitoneDelta = inputSpelled.midi() - axisSpelled.midi();
        final int targetMidi = axisSpelled.midi() - semitoneDelta;
        final int nameStepsDelta = inputSpelled.name().ordinal() - axisSpelled.name().ordinal();
        final int newNameOrdinal = ((axisSpelled.name().ordinal() - nameStepsDelta) % 7 + 7) % 7;
        final NoteName newName = NoteName.values()[newNameOrdinal];
        for (int oct = axisSpelled.octave() - 2; oct <= axisSpelled.octave() + 2; oct++) {
            final int accOffset = targetMidi - (oct + 1) * 12 - newName.semitonesAboveC();
            if (accOffset >= -2 && accOffset <= 2) {
                final Accidental acc = Accidental.fromOffset(accOffset);
                try {
                    return SpelledPitch.of(newName, acc, oct);
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        throw new IllegalArgumentException("Cannot compute inversion of " + input + " around " + axis);
    }
}
