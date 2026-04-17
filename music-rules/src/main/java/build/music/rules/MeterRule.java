package build.music.rules;

import build.music.core.NoteEvent;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.TimeSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Checks that measures have the correct total duration for the time signature.
 */
public final class MeterRule implements Rule {

    private final TimeSignature timeSignature;

    public MeterRule(final TimeSignature ts) {
        this.timeSignature = Objects.requireNonNull(ts, "timeSignature must not be null");
    }

    @Override
    public String name() {
        return "meter";
    }

    @Override
    public String description() {
        return "Checks measure durations match " + timeSignature;
    }

    @Override
    public List<Violation> check(final Voice voice, final String voiceName) {
        final List<Violation> violations = new ArrayList<>();
        final Fraction measureDur = timeSignature.measureDuration();

        Fraction accumulated = Fraction.ZERO;
        int measureNum = 1;
        int noteIdx = 0;

        for (final NoteEvent event : voice.events()) {
            final Fraction eventDur = event.duration().fraction();
            accumulated = accumulated.add(eventDur);

            if (accumulated.compareTo(measureDur) > 0) {
                violations.add(Violation.error(name(),
                    "Measure " + measureNum + " overflows: contains " + accumulated +
                        " but " + timeSignature + " expects " + measureDur,
                    noteIdx, voiceName));
                // Reset to avoid cascading errors
                accumulated = Fraction.ZERO;
                measureNum++;
            } else if (accumulated.compareTo(measureDur) == 0) {
                accumulated = Fraction.ZERO;
                measureNum++;
            }
            noteIdx++;
        }

        // Check if last measure is incomplete
        if (accumulated.compareTo(Fraction.ZERO) > 0) {
            violations.add(Violation.warning(name(),
                "Last measure (measure " + measureNum + ") is incomplete: has " + accumulated +
                    " of " + measureDur,
                noteIdx - 1, voiceName));
        }

        return List.copyOf(violations);
    }
}
