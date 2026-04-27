package build.music.harmony;

import build.music.core.Chord;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;

import java.util.List;

/**
 * Transpose within a key by scale steps (diatonic transposition).
 * Unlike chromatic transposition, diatonic transposition preserves membership in the key:
 * each pitch moves to the nearest scale degree in the given direction.
 */
public final class DiatonicTranspose {

    private DiatonicTranspose() {
    }

    /**
     * Transpose a pitch up or down by the given number of scale steps within the key.
     * <p>
     * In C major: C4 up 2 steps = E4 (moves by diatonic third, not major third).
     * In C major: E4 up 1 step = F4 (minor second, not major second).
     * In C major: B4 up 1 step = C5 (wraps octave).
     *
     * @param pitch the pitch to transpose (must be in the scale)
     * @param key   the key defining the scale
     * @param steps number of scale steps (positive = up, negative = down)
     * @return the transposed pitch
     * @throws IllegalArgumentException if the pitch is not in the key's scale
     */
    public static SpelledPitch transpose(final SpelledPitch pitch, final Key key, final int steps) {
        final Scale scale = key.scale();

        // Find the scale degree of the given pitch (search across nearby octaves)
        int degree = -1;
        int scaleOctave = pitch.octave();

        outer:
        for (int tryOct = pitch.octave() - 1; tryOct <= pitch.octave() + 1; tryOct++) {
            final int degreeCount = scale.type().degreeCount();
            for (int d = 1; d <= degreeCount; d++) {
                final SpelledPitch p = scale.degree(d, tryOct);
                if (p.midi() == pitch.midi()) {
                    degree = d;
                    scaleOctave = tryOct;
                    break outer;
                }
            }
        }

        if (degree == -1) {
            throw new IllegalArgumentException(
                "Pitch " + pitch + " is not in scale " + scale + " (key: " + key + ")");
        }

        final int degreeCount = scale.type().degreeCount();
        final int newDegRaw = degree - 1 + steps; // 0-based
        final int octaveShift = Math.floorDiv(newDegRaw, degreeCount);
        final int newDeg = Math.floorMod(newDegRaw, degreeCount) + 1; // back to 1-based

        return scale.degree(newDeg, scaleOctave + octaveShift);
    }

    /**
     * Transpose an entire note sequence diatonically.
     * Rests are passed through unchanged. Non-scale pitches throw IllegalArgumentException.
     */
    public static List<NoteEvent> transpose(final List<NoteEvent> events, final Key key, final int steps) {
        return events.stream().map(event -> switch (event) {
            case Note n -> {
                final SpelledPitch newPitch = transpose(n.pitch().spelled(), key, steps);
                yield (NoteEvent) Note.of(newPitch, n.duration(), n.velocity(), n.articulation(), n.tied());
            }
            case Rest r -> r;
            case Chord c -> c;
            case ControlChange cc -> cc;
            case ProgramChange pc -> pc;
        }).toList();
    }
}
