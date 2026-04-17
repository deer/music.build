package build.music.rules;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.instrument.Instrument;
import build.music.score.Voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Checks that all notes in a voice are within an instrument's written range.
 */
public final class RangeRule implements Rule {

    private final Instrument instrument;

    public RangeRule(final Instrument instrument) {
        this.instrument = Objects.requireNonNull(instrument, "instrument must not be null");
    }

    @Override
    public String name() {
        return "range";
    }

    @Override
    public String description() {
        return "Notes must be within " + instrument.name() + " range (" + instrument.writtenRange() + ")";
    }

    @Override
    public List<Violation> check(final Voice voice, final String voiceName) {
        final List<Violation> violations = new ArrayList<>();
        final List<NoteEvent> events = voice.events();

        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) instanceof Note n) {
                final var pitch = n.pitch().spelled();
                if (!instrument.writtenRange().contains(pitch)) {
                    final boolean belowRange = pitch.midi() < instrument.writtenRange().low().midi();
                    final String dir = belowRange ? "below" : "above";
                    violations.add(Violation.error(name(),
                        "Note " + pitch + " is " + dir + " " + instrument.name() +
                            " range (" + instrument.writtenRange() + ")",
                        i, voiceName));
                } else if (!instrument.comfortableRange().isComfortable(pitch)) {
                    violations.add(Violation.warning(name(),
                        "Note " + pitch + " is in the extreme register of " + instrument.name(),
                        i, voiceName));
                }
            }
        }

        return List.copyOf(violations);
    }
}
