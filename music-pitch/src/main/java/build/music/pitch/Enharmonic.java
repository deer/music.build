package build.music.pitch;

import java.util.ArrayList;
import java.util.List;

public final class Enharmonic {

    private Enharmonic() {}

    public static boolean areEnharmonic(final SpelledPitch a, final SpelledPitch b) {
        return a.midi() == b.midi() && !a.equals(b);
    }

    public static SpelledPitch simplify(final SpelledPitch pitch) {
        if (pitch.accidental() == Accidental.NATURAL) {
            return pitch;
        }
        final int target = pitch.midi();
        for (final Accidental acc : List.of(Accidental.NATURAL, Accidental.FLAT, Accidental.SHARP,
                Accidental.DOUBLE_FLAT, Accidental.DOUBLE_SHARP)) {
            for (final NoteName name : NoteName.values()) {
                final int raw = (pitch.octave() + 1) * 12 + name.semitonesAboveC() + acc.semitoneOffset();
                if (raw == target) {
                    try {
                        return SpelledPitch.of(name, acc, pitch.octave());
                    } catch (final IllegalArgumentException ignored) {}
                }
                final int rawAdj = (pitch.octave()) * 12 + name.semitonesAboveC() + acc.semitoneOffset();
                if (rawAdj == target) {
                    try {
                        return SpelledPitch.of(name, acc, pitch.octave() - 1);
                    } catch (final IllegalArgumentException ignored) {}
                }
            }
        }
        return pitch;
    }

    public static List<SpelledPitch> allEnharmonics(final SpelledPitch pitch) {
        final List<SpelledPitch> result = new ArrayList<>();
        final int target = pitch.midi();
        for (final NoteName name : NoteName.values()) {
            for (final Accidental acc : Accidental.values()) {
                for (int octave = -1; octave <= 10; octave++) {
                    final int midi = (octave + 1) * 12 + name.semitonesAboveC() + acc.semitoneOffset();
                    if (midi == target && midi >= 0 && midi <= 127) {
                        try {
                            result.add(SpelledPitch.of(name, acc, octave));
                        } catch (final IllegalArgumentException ignored) {}
                    }
                }
            }
        }
        return List.copyOf(result);
    }
}
