package build.music.harmony;

import build.music.core.Articulation;
import build.music.core.Chord;
import build.music.core.ChordSymbol;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.pitch.SpelledPitch;
import build.music.time.DottedValue;
import build.music.time.Duration;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds harmonic accompaniment to a melody.
 */
public final class Harmonizer {

    private Harmonizer() {}

    /**
     * Harmonize a melody with block chord roots based on a chord progression.
     * Each chord in the progression spans one measure (measureDuration from the time signature).
     * The returned voice contains the root of each chord as a whole-measure note.
     *
     * @param melody         the melodic note events
     * @param key            the key to harmonize in
     * @param progression    the chord progression (one chord per measure)
     * @param voicingOctave  the octave to voice chord roots in
     * @return list of NoteEvents: chord roots, one per measure
     */
    public static List<NoteEvent> harmonize(
            final List<NoteEvent> melody,
            final Key key,
            final ChordProgression progression,
            final int voicingOctave,
            final TimeSignature ts) {

        final List<ChordSymbol> chords = progression.inKey(key);
        final List<NoteEvent> result = new ArrayList<>();
        final Duration chordDuration = fractionToDuration(ts.measureDuration());

        for (final ChordSymbol chord : chords) {
            final SpelledPitch rootPitch = SpelledPitch.of(chord.root(), chord.rootAccidental(), voicingOctave);
            result.add(Note.of(rootPitch, chordDuration));
        }

        return List.copyOf(result);
    }

    /**
     * Generate a walking bass line whose pattern adapts to the time signature.
     *
     * Patterns per bar:
     *   4/4 — root, fifth, third, approach (classic 4-note walking bass)
     *   3/4 — root, fifth, fifth
     *   6/8 — root (dotted-quarter), fifth (dotted-quarter)
     *   2/4 — root, approach
     *   other — root, then (beats−1) fifths, each of beat-unit duration
     *
     * @param chords  resolved chord symbols, one per bar
     * @param key     the key context (for scale-tone approach notes)
     * @param octave  bass octave (2 is typical: C2–B2 range)
     * @param ts      time signature determining beats per bar
     * @return list of NoteEvents, one bar's worth of notes per chord
     */
    public static List<NoteEvent> walkingBass(
            final List<ChordSymbol> chords, final Key key, final int octave, final TimeSignature ts) {
        final Scale scale = key.scale();
        final List<NoteEvent> result = new ArrayList<>();

        for (int i = 0; i < chords.size(); i++) {
            final List<SpelledPitch> tones = chords.get(i).toPitches(octave);
            final SpelledPitch root  = tones.get(0);
            final SpelledPitch fifth = tones.size() >= 3 ? tones.get(2) : tones.get(0);
            final SpelledPitch third = tones.size() >= 2 ? tones.get(1) : tones.get(0);

            final SpelledPitch approach;
            if (i < chords.size() - 1) {
                final SpelledPitch nextRoot = chords.get(i + 1).toPitches(octave).get(0);
                approach = findApproachBelow(scale, nextRoot, octave);
            } else {
                approach = root;
            }

            final int beats = ts.beats();
            final int beatUnit = ts.beatUnit();

            if (beats == 4 && beatUnit == 4) {
                // 4/4: root, fifth, third, approach
                final Duration q = RhythmicValue.QUARTER;
                result.add(Note.of(root,     q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(fifth,    q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(third,    q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(approach, q, Velocity.MF, Articulation.NORMAL, false));
            } else if (beats == 3 && beatUnit == 4) {
                // 3/4: root, fifth, fifth
                final Duration q = RhythmicValue.QUARTER;
                result.add(Note.of(root,  q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(fifth, q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(fifth, q, Velocity.MF, Articulation.NORMAL, false));
            } else if (beats == 6 && beatUnit == 8) {
                // 6/8: root (dotted-quarter), fifth (dotted-quarter)
                final Duration dq = new DottedValue(RhythmicValue.QUARTER, 1);
                result.add(Note.of(root,  dq, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(fifth, dq, Velocity.MF, Articulation.NORMAL, false));
            } else if (beats == 2 && beatUnit == 4) {
                // 2/4: root, approach
                final Duration q = RhythmicValue.QUARTER;
                result.add(Note.of(root,     q, Velocity.MF, Articulation.NORMAL, false));
                result.add(Note.of(approach, q, Velocity.MF, Articulation.NORMAL, false));
            } else {
                // fallback: root + (beats−1) fifths, each of beat-unit duration
                final Duration bd = beatDuration(beatUnit);
                result.add(Note.of(root, bd, Velocity.MF, Articulation.NORMAL, false));
                for (int b = 1; b < beats; b++) {
                    result.add(Note.of(fifth, bd, Velocity.MF, Articulation.NORMAL, false));
                }
            }
        }

        return List.copyOf(result);
    }

    private static Duration beatDuration(final int beatUnit) {
        return switch (beatUnit) {
            case 1  -> RhythmicValue.WHOLE;
            case 2  -> RhythmicValue.HALF;
            case 4  -> RhythmicValue.QUARTER;
            case 8  -> RhythmicValue.EIGHTH;
            case 16 -> RhythmicValue.SIXTEENTH;
            default -> RhythmicValue.QUARTER;
        };
    }

    /**
     * Find the highest scale pitch that is 1–2 semitones below targetPitch.
     * Falls back to the target pitch itself if no such scale tone is found.
     */
    private static SpelledPitch findApproachBelow(final Scale scale, final SpelledPitch targetPitch, final int octave) {
        final int targetMidi = targetPitch.midi();
        SpelledPitch best = null;

        // Check both the bass octave and one octave down (for wrap-around at octave boundary)
        for (int oct = octave - 1; oct <= octave; oct++) {
            for (final SpelledPitch p : scale.pitches(oct)) {
                final int dist = targetMidi - p.midi();
                if (dist >= 1 && dist <= 2) {
                    if (best == null || p.midi() > best.midi()) {
                        best = p;
                    }
                }
            }
        }

        return best != null ? best : targetPitch;
    }

    /**
     * Generate a comping (chord accompaniment) voice over bar-level chord changes.
     * The pattern adapts to the time signature.
     *
     * Styles:
     *   "quarter_stabs"   — rest then chord(s) on remaining beats
     *   "on_beat"         — block chord on every beat
     *   "eighth_pump"     — block chord on every eighth note
     *   "shell_voicings"  — root + 7th (shell) on off-beats
     *   "charleston"      — dotted quarter + eighth + quarter (4/4 only; warning in other meters)
     *
     * @param chords    resolved chord symbols, one per bar
     * @param octave    voicing octave (e.g. 3 for mid-range Rhodes comping)
     * @param style     comping style name
     * @param ts        time signature determining beats per bar
     * @return list of NoteEvents (Chord and Rest objects)
     */
    public static List<NoteEvent> comp(
            final List<ChordSymbol> chords, final int octave, final String style, final TimeSignature ts) {
        final List<NoteEvent> result = new ArrayList<>();
        for (final ChordSymbol chord : chords) {
            result.addAll(compBar(chord, octave, style, ts));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<NoteEvent> compBar(
            final ChordSymbol chord, final int octave, final String style, final TimeSignature ts) {
        final List<SpelledPitch> spelled = chord.toPitches(octave);
        final List<build.music.pitch.Pitch> tones = (List<build.music.pitch.Pitch>) (List<?>) spelled;
        final List<build.music.pitch.Pitch> shellTones = spelled.size() >= 4
            ? List.of(spelled.get(0), spelled.get(3))
            : List.of(spelled.get(0), spelled.get(Math.min(2, spelled.size() - 1)));

        final Duration q  = RhythmicValue.QUARTER;
        final Duration e  = RhythmicValue.EIGHTH;
        final Duration dq = new DottedValue(RhythmicValue.QUARTER, 1);
        final Rest rq = Rest.of(q);
        final Rest rdq = Rest.of(dq);

        final int beats    = ts.beats();
        final int beatUnit = ts.beatUnit();
        final String styleKey = style.toLowerCase();

        // 6/8: two dotted-quarter pulses per bar
        if (beats == 6 && beatUnit == 8) {
            final Chord fullDQ  = Chord.of(tones,     dq, Velocity.MF);
            final Chord shellDQ = Chord.of(shellTones, dq, Velocity.MF);
            return switch (styleKey) {
                case "quarter_stabs", "shell_voicings" -> List.of(rdq, shellDQ);
                case "on_beat"    -> List.of(fullDQ, fullDQ);
                case "eighth_pump" -> {
                    final var pump = Chord.of(tones, e, Velocity.MF);
                    yield List.of(pump, pump, pump, pump, pump, pump);
                }
                case "charleston" -> List.of(fullDQ, fullDQ); // graceful fallback
                default -> throw new IllegalArgumentException(unknownStyleMsg(style));
            };
        }

        // 3/4: three quarter-note beats per bar
        if (beats == 3 && beatUnit == 4) {
            final Chord full  = Chord.of(tones,     q, Velocity.MF);
            final Chord shell = Chord.of(shellTones, q, Velocity.MF);
            return switch (styleKey) {
                case "quarter_stabs" -> List.of(rq, full,  full);
                case "on_beat"       -> List.of(full, full, full);
                case "eighth_pump"   -> {
                    final var pump = Chord.of(tones, e, Velocity.MF);
                    yield List.of(pump, pump, pump, pump, pump, pump);
                }
                case "shell_voicings" -> List.of(rq, shell, shell);
                case "charleston"     -> List.of(rq, full,  full); // graceful fallback
                default -> throw new IllegalArgumentException(unknownStyleMsg(style));
            };
        }

        // 2/4: two quarter-note beats per bar
        if (beats == 2 && beatUnit == 4) {
            final Chord full  = Chord.of(tones,     q, Velocity.MF);
            final Chord shell = Chord.of(shellTones, q, Velocity.MF);
            return switch (styleKey) {
                case "quarter_stabs", "shell_voicings" -> List.of(rq, shell);
                case "on_beat"       -> List.of(full, full);
                case "eighth_pump"   -> {
                    final var pump = Chord.of(tones, e, Velocity.MF);
                    yield List.of(pump, pump, pump, pump);
                }
                case "charleston" -> List.of(rq, full); // graceful fallback
                default -> throw new IllegalArgumentException(unknownStyleMsg(style));
            };
        }

        // 4/4 (and fallback for other simple meters): original patterns
        final Chord full  = Chord.of(tones,     q, Velocity.MF);
        final Chord shell = Chord.of(shellTones, q, Velocity.MF);
        return switch (styleKey) {
            case "quarter_stabs"  -> List.of(rq, full, rq, full);
            case "on_beat"        -> List.of(full, full, full, full);
            case "eighth_pump"    -> List.of(
                Chord.of(tones, e, Velocity.MF), Chord.of(tones, e, Velocity.MF),
                Chord.of(tones, e, Velocity.MF), Chord.of(tones, e, Velocity.MF),
                Chord.of(tones, e, Velocity.MF), Chord.of(tones, e, Velocity.MF),
                Chord.of(tones, e, Velocity.MF), Chord.of(tones, e, Velocity.MF));
            case "shell_voicings" -> List.of(rq, shell, rq, shell);
            case "charleston"     -> List.of(
                Chord.of(tones, dq, Velocity.MF),
                Chord.of(tones, e,  Velocity.MF),
                Chord.of(tones, q,  Velocity.MF));
            default -> throw new IllegalArgumentException(unknownStyleMsg(style));
        };
    }

    /**
     * Maps a measure-duration fraction to a concrete Duration.
     * Handles whole, half, dotted-half (3/4, 6/8), and common compound meters.
     * Falls back to the nearest standard RhythmicValue for unrecognised fractions.
     */
    static Duration fractionToDuration(final Fraction f) {
        for (final RhythmicValue rv : RhythmicValue.values()) {
            if (rv.fraction().equals(f)) {
                return rv;
            }
        }
        // Check dotted values (duration = rv * 3/2)
        for (final RhythmicValue rv : RhythmicValue.values()) {
            if (rv.fraction().multiply(Fraction.of(3, 2)).equals(f)) {
                return new DottedValue(rv, 1);
            }
        }
        // Double-dotted (duration = rv * 7/4)
        for (final RhythmicValue rv : RhythmicValue.values()) {
            if (rv.fraction().multiply(Fraction.of(7, 4)).equals(f)) {
                return new DottedValue(rv, 2);
            }
        }
        // Fallback: nearest RhythmicValue
        RhythmicValue best = RhythmicValue.WHOLE;
        final double target = f.toDouble();
        double bestDist = Math.abs(best.fraction().toDouble() - target);
        for (final RhythmicValue rv : RhythmicValue.values()) {
            final double dist = Math.abs(rv.fraction().toDouble() - target);
            if (dist < bestDist) {
                bestDist = dist;
                best = rv;
            }
        }
        return best;
    }

    private static String unknownStyleMsg(final String style) {
        return "Unknown comp style '" + style + "'. Valid styles: " +
            "quarter_stabs, on_beat, eighth_pump, shell_voicings, charleston.";
    }

    /**
     * Suggest a chord progression for a melody using a simple heuristic.
     * For each measure, finds the diatonic triad that contains the most melody notes
     * (weighted by duration).
     *
     * @param melody melody note events
     * @param key    the key context
     * @param ts     time signature (defines measure boundaries)
     * @return suggested ChordProgression
     */
    public static ChordProgression suggestHarmony(
            final List<NoteEvent> melody,
            final Key key,
            final TimeSignature ts) {

        final Fraction measureDur = ts.measureDuration();
        final List<RomanNumeral> diatonic = key.minor()
            ? RomanNumeral.diatonicMinor()
            : RomanNumeral.diatonicMajor();
        final List<ChordSymbol> diatonicChords = diatonic.stream()
            .map(rn -> rn.chordInKey(key))
            .toList();

        final List<RomanNumeral> result = new ArrayList<>();
        Fraction pos = Fraction.ZERO;
        int eventIdx = 0;

        while (eventIdx < melody.size()) {
            final Fraction measureEnd = pos.add(measureDur);
            // Accumulate pitch durations within this measure
            final Map<Integer, Fraction> pitchDurations = new HashMap<>();
            Fraction accumulated = Fraction.ZERO;

            int i = eventIdx;
            while (i < melody.size() && accumulated.compareTo(measureDur) < 0) {
                final NoteEvent event = melody.get(i);
                if (event instanceof Note n) {
                    final int midi = n.midi();
                    pitchDurations.merge(midi % 12, event.duration().fraction(), Fraction::add);
                }
                accumulated = accumulated.add(event.duration().fraction());
                i++;
            }

            // Score each diatonic chord against this measure's pitches
            int bestChordIdx = 0;
            double bestScore = -1.0;
            for (int c = 0; c < diatonicChords.size(); c++) {
                final ChordSymbol chord = diatonicChords.get(c);
                final List<SpelledPitch> chordTones = chord.toPitches(4);
                double score = 0.0;
                for (final SpelledPitch tone : chordTones) {
                    final int pc = tone.midi() % 12;
                    final Fraction dur = pitchDurations.get(pc);
                    if (dur != null) {
                        score += dur.toDouble();
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestChordIdx = c;
                }
            }

            result.add(diatonic.get(bestChordIdx));
            pos = measureEnd;
            eventIdx = i;
        }

        if (result.isEmpty()) {
            result.add(RomanNumeral.parse(key.minor() ? "i" : "I"));
        }

        return ChordProgression.of(result);
    }
}
