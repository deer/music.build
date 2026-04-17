package build.music.harmony;

import build.music.core.ChordSymbol;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes harmonic content of note sequences.
 */
public final class HarmonicAnalyzer {

    // Krumhansl-Schmuckler key profiles (simplified)
    private static final double[] MAJOR_PROFILE = {
        6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88
    };
    private static final double[] MINOR_PROFILE = {
        6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17
    };

    private HarmonicAnalyzer() {}

    /**
     * Label each chord in the list with its Roman numeral function in the given key.
     */
    public static List<RomanNumeral> analyze(final List<ChordSymbol> chords, final Key key) {
        final List<RomanNumeral> diatonic = key.minor()
            ? RomanNumeral.diatonicMinor()
            : RomanNumeral.diatonicMajor();
        final List<ChordSymbol> diatonicChords = diatonic.stream()
            .map(rn -> rn.chordInKey(key))
            .toList();

        return chords.stream().map(chord -> {
            // Find matching diatonic Roman numeral by root
            for (int i = 0; i < diatonicChords.size(); i++) {
                final ChordSymbol dc = diatonicChords.get(i);
                if (dc.root() == chord.root() && dc.rootAccidental() == chord.rootAccidental()) {
                    return diatonic.get(i);
                }
            }
            // No match: return a generic Roman numeral based on scale degree
            return diatonic.get(0); // default to I/i
        }).toList();
    }

    /**
     * Detect the likely key of a note sequence using a simplified
     * Krumhansl-Schmuckler key-finding algorithm.
     * Correlates pitch class distribution with major/minor key profiles.
     */
    public static Key detectKey(final List<NoteEvent> events) {
        // Count pitch class durations
        final double[] pcWeights = new double[12];
        for (final NoteEvent event : events) {
            if (event instanceof Note n) {
                final int pc = n.midi() % 12;
                pcWeights[pc] += event.duration().fraction().toDouble();
            }
        }

        // Normalize
        double total = 0;
        for (final double w : pcWeights) {
            total += w;
        }
        if (total > 0) {
            for (int i = 0; i < 12; i++) {
                pcWeights[i] /= total;
            }
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        Key bestKey = Key.major(NoteName.C);

        // Test all 24 keys (12 major + 12 minor)
        for (int root = 0; root < 12; root++) {
            // Major key
            final double majorScore = correlate(pcWeights, MAJOR_PROFILE, root);
            if (majorScore > bestScore) {
                bestScore = majorScore;
                bestKey = keyFromPitchClass(root, false);
            }
            // Minor key
            final double minorScore = correlate(pcWeights, MINOR_PROFILE, root);
            if (minorScore > bestScore) {
                bestScore = minorScore;
                bestKey = keyFromPitchClass(root, true);
            }
        }

        return bestKey;
    }

    /**
     * Find all instances of a chord progression pattern within a longer chord sequence.
     * Returns the indices where the pattern starts.
     */
    public static List<Integer> findProgression(
            final List<ChordSymbol> chords, final ChordProgression pattern, final Key key) {
        final List<ChordSymbol> patternChords = pattern.inKey(key);
        final int patLen = patternChords.size();
        final List<Integer> matches = new ArrayList<>();

        for (int i = 0; i <= chords.size() - patLen; i++) {
            boolean match = true;
            for (int j = 0; j < patLen; j++) {
                final ChordSymbol a = chords.get(i + j);
                final ChordSymbol b = patternChords.get(j);
                if (a.root() != b.root() || a.rootAccidental() != b.rootAccidental()
                        || a.quality() != b.quality()) {
                    match = false;
                    break;
                }
            }
            if (match) {
                matches.add(i);
            }
        }

        return List.copyOf(matches);
    }

    private static double correlate(final double[] observed, final double[] profile, final int shift) {
        // Pearson correlation between observed[i] and profile[(i - shift + 12) % 12]
        double meanObs = 0;
        double meanPro = 0;
        for (int i = 0; i < 12; i++) {
            meanObs += observed[i];
            meanPro += profile[i];
        }
        meanObs /= 12;
        meanPro /= 12;

        double num = 0;
        double denObs = 0;
        double denPro = 0;
        for (int i = 0; i < 12; i++) {
            final double o = observed[(i + shift) % 12] - meanObs;
            final double p = profile[i] - meanPro;
            num += o * p;
            denObs += o * o;
            denPro += p * p;
        }
        final double den = Math.sqrt(denObs * denPro);
        return den == 0 ? 0 : num / den;
    }

    private static final NoteName[] PC_TO_NOTE = {
        NoteName.C, NoteName.C, NoteName.D, NoteName.D, NoteName.E,
        NoteName.F, NoteName.F, NoteName.G, NoteName.G, NoteName.A,
        NoteName.A, NoteName.B
    };
    private static final Accidental[] PC_TO_ACC = {
        Accidental.NATURAL, Accidental.SHARP, Accidental.NATURAL, Accidental.SHARP, Accidental.NATURAL,
        Accidental.NATURAL, Accidental.SHARP, Accidental.NATURAL, Accidental.SHARP, Accidental.NATURAL,
        Accidental.SHARP, Accidental.NATURAL
    };

    private static Key keyFromPitchClass(final int pc, final boolean minor) {
        return Key.of(PC_TO_NOTE[pc], PC_TO_ACC[pc], minor);
    }
}
