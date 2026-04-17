package build.music.voice;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.time.Fraction;
import build.music.time.TimeSignature;

import java.util.List;
import java.util.Objects;

/**
 * A view into a single measure of a voice.
 */
public record MeasureSlice(int measureNumber, List<NoteEvent> events, Fraction totalDuration) {

    public MeasureSlice {
        Objects.requireNonNull(events, "events must not be null");
        Objects.requireNonNull(totalDuration, "totalDuration must not be null");
        events = List.copyOf(events);
    }

    /** Does this measure have the correct total duration for the time signature? */
    public boolean isComplete(final TimeSignature ts) {
        return totalDuration.compareTo(ts.measureDuration()) == 0;
    }

    /** Pitches present in this measure (excluding rests). */
    public List<SpelledPitch> pitches() {
        return events.stream()
            .filter(e -> e instanceof Note)
            .map(e -> ((Note) e).pitch().spelled())
            .toList();
    }
}
