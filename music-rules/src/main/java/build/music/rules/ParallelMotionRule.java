package build.music.rules;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.score.Voice;
import build.music.time.Fraction;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects parallel fifths and octaves between two voices.
 * Only checks voices with identical rhythmic patterns (same event count and durations).
 * When rhythms differ, the check is skipped (documented limitation).
 */
public final class ParallelMotionRule implements Rule {

    @Override
    public String name() {
        return "parallel_motion";
    }

    @Override
    public String description() {
        return "Detects parallel perfect fifths and octaves between voice pairs";
    }

    @Override
    public List<Violation> check(final Voice voice, final String voiceName) {
        return List.of(); // single-voice check not applicable
    }

    @Override
    public List<Violation> checkPair(final Voice a, final String aName, final Voice b, final String bName) {
        // Only check when both voices have the same number of events and same durations
        if (a.events().size() != b.events().size()) {
            return List.of(); // differing event counts — skip
        }

        // Verify rhythms match
        for (int i = 0; i < a.events().size(); i++) {
            final Fraction da = a.events().get(i).duration().fraction();
            final Fraction db = b.events().get(i).duration().fraction();
            if (da.compareTo(db) != 0) {
                return List.of(); // differing rhythms — skip
            }
        }

        final List<Violation> violations = new ArrayList<>();
        final List<NoteEvent> eventsA = a.events();
        final List<NoteEvent> eventsB = b.events();

        Integer prevInterval = null;
        Integer prevMidiA = null;
        Integer prevMidiB = null;

        for (int i = 0; i < eventsA.size(); i++) {
            if (!(eventsA.get(i) instanceof Note noteA) || !(eventsB.get(i) instanceof Note noteB)) {
                prevInterval = null;
                prevMidiA = null;
                prevMidiB = null;
                continue;
            }

            final int midiA = noteA.midi();
            final int midiB = noteB.midi();
            final int interval = Math.abs(midiA - midiB) % 12;

            if (prevInterval != null && prevMidiA != null && prevMidiB != null) {
                // Is the current interval a perfect fifth (7 semitones) or octave (0)?
                final boolean isPerfectFifth = interval == 7;
                final boolean isPerfectOctave = interval == 0;
                final boolean prevIsPerfectFifth = prevInterval == 7;
                final boolean prevIsPerfectOctave = prevInterval == 0;

                if ((isPerfectFifth && prevIsPerfectFifth) || (isPerfectOctave && prevIsPerfectOctave)) {
                    // Check both voices moved in the same direction (parallel motion)
                    final int motionA = Integer.compare(midiA, prevMidiA);
                    final int motionB = Integer.compare(midiB, prevMidiB);

                    if (motionA != 0 && motionB != 0 && motionA == motionB) {
                        final String type = isPerfectFifth ? "fifths" : "octaves";
                        violations.add(Violation.warning(name(),
                            "Parallel " + type + " between '" + aName + "' and '" + bName +
                                "' at notes " + (i - 1) + "-" + i,
                            i, aName + "/" + bName));
                    }
                }
            }

            prevInterval = interval;
            prevMidiA = midiA;
            prevMidiB = midiB;
        }

        return List.copyOf(violations);
    }
}
