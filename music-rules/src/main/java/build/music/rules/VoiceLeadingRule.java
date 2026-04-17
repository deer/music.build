package build.music.rules;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.score.Voice;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic melodic voice-leading checks: large leaps, augmented intervals, repeated notes.
 */
public final class VoiceLeadingRule implements Rule {

    @Override
    public String name() {
        return "voice_leading";
    }

    @Override
    public String description() {
        return "Checks for large leaps, augmented intervals, and excessive note repetition";
    }

    @Override
    public List<Violation> check(final Voice voice, final String voiceName) {
        final List<Violation> violations = new ArrayList<>();
        final List<NoteEvent> events = voice.events();

        Integer prevMidi = null;
        int repeatCount = 0;
        Integer lastNoteMidi = null;

        for (int i = 0; i < events.size(); i++) {
            if (!(events.get(i) instanceof Note n)) {
                prevMidi = null;
                continue;
            }

            final int midi = n.midi();

            // Track repeated notes
            if (lastNoteMidi != null && lastNoteMidi == midi) {
                repeatCount++;
                if (repeatCount > 3) {
                    violations.add(Violation.suggestion(name(),
                        "Pitch " + n.pitch() + " repeated " + (repeatCount + 1) + " times in a row",
                        i, voiceName));
                }
            } else {
                repeatCount = 0;
            }
            lastNoteMidi = midi;

            if (prevMidi != null) {
                final int semitones = Math.abs(midi - prevMidi);

                // Large leap: more than an octave
                // 13-19 semitones (minor 9th to minor 13th) → suggestion (may be intentional bass reset)
                // > 19 semitones (major 13th+) → warning
                if (semitones > 19) {
                    violations.add(Violation.warning(name(),
                        "Very large leap of " + semitones + " semitones at note " + i,
                        i, voiceName));
                } else if (semitones > 12) {
                    violations.add(Violation.suggestion(name(),
                        "Large leap of " + semitones + " semitones at note " + i +
                            " — may be intentional (bass reset, compound melody)",
                        i, voiceName));
                }

                // Augmented second (3 semitones with a step movement, e.g. G#→Bb)
                // Simple heuristic: check if interval is a minor third or augmented second (both = 3 semitones)
                // True detection requires spelled intervals; for now flag tritone (6 semitones) as suggestion
                if (semitones == 6) {
                    violations.add(Violation.suggestion(name(),
                        "Tritone leap at note " + i + " — consider a different approach",
                        i, voiceName));
                }
            }

            prevMidi = midi;
        }

        return List.copyOf(violations);
    }
}
