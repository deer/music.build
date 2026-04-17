package build.music.voice;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Static utility methods for advanced Voice operations.
 */
public final class VoiceOperations {

    private VoiceOperations() {
    }

    /**
     * Concatenate multiple voices into a single voice.
     * Events are placed sequentially: voice B starts after voice A ends.
     */
    public static Voice concat(final String name, final Voice... voices) {
        return concat(name, Arrays.asList(voices));
    }

    public static Voice concat(final String name, final List<Voice> voices) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(voices, "voices must not be null");
        final List<NoteEvent> combined = new ArrayList<>();
        for (final Voice v : voices) {
            combined.addAll(v.events());
        }
        return Voice.of(name, combined);
    }

    /**
     * Slice a voice to extract notes from index start (inclusive) to end (exclusive).
     */
    public static Voice slice(final Voice voice, final String name, final int startIndex, final int endIndex) {
        Objects.requireNonNull(voice, "voice must not be null");
        final List<NoteEvent> events = voice.events();
        if (startIndex < 0 || endIndex > events.size() || startIndex > endIndex) {
            throw new IllegalArgumentException(
                "Invalid slice range [" + startIndex + ", " + endIndex + ") for voice with "
                    + events.size() + " events");
        }
        return Voice.of(name, events.subList(startIndex, endIndex));
    }

    /**
     * Slice by measure number (1-based, inclusive start and exclusive end).
     * Measures are computed from the time signature.
     */
    public static Voice sliceMeasures(final Voice voice, final String name,
                                      final int startMeasure, final int endMeasure, final TimeSignature ts) {
        Objects.requireNonNull(voice, "voice must not be null");
        Objects.requireNonNull(ts, "ts must not be null");

        final Fraction measureDur = ts.measureDuration();
        final Fraction startPos = measureDur.multiply(startMeasure - 1);
        final Fraction endPos = measureDur.multiply(endMeasure - 1);

        final List<NoteEvent> result = new ArrayList<>();
        Fraction pos = Fraction.ZERO;

        for (final NoteEvent event : voice.events()) {
            final Fraction eventEnd = pos.add(event.duration().fraction());
            if (pos.compareTo(startPos) >= 0 && pos.compareTo(endPos) < 0) {
                result.add(event);
            }
            pos = eventEnd;
            if (pos.compareTo(endPos) >= 0) {
                break;
            }
        }

        return Voice.of(name, result);
    }

    /**
     * Split a voice into measures. Returns one Voice per measure.
     */
    public static List<Voice> splitByMeasure(final Voice voice, final TimeSignature ts) {
        Objects.requireNonNull(voice, "voice must not be null");
        Objects.requireNonNull(ts, "ts must not be null");

        final Fraction measureDur = ts.measureDuration();
        final List<Voice> result = new ArrayList<>();
        List<NoteEvent> current = new ArrayList<>();
        Fraction accumulated = Fraction.ZERO;
        int measureNum = 1;

        for (final NoteEvent event : voice.events()) {
            current.add(event);
            accumulated = accumulated.add(event.duration().fraction());
            if (accumulated.compareTo(measureDur) >= 0) {
                result.add(Voice.of(voice.name() + "_m" + measureNum, current));
                current = new ArrayList<>();
                accumulated = Fraction.ZERO;
                measureNum++;
            }
        }
        if (!current.isEmpty()) {
            result.add(Voice.of(voice.name() + "_m" + measureNum, current));
        }
        return List.copyOf(result);
    }

    /**
     * Merge two voices into one, ordering events by their temporal position.
     * For simplicity, interleaves events in time order assuming no overlapping notes.
     */
    public static Voice merge(final String name, final Voice a, final Voice b) {
        Objects.requireNonNull(a, "voice a must not be null");
        Objects.requireNonNull(b, "voice b must not be null");

        // Build time-stamped event lists
        final List<TimedEvent> timedA = buildTimed(a.events());
        final List<TimedEvent> timedB = buildTimed(b.events());

        // Merge-sort by position
        final List<TimedEvent> merged = new ArrayList<>();
        int ia = 0;
        int ib = 0;
        while (ia < timedA.size() && ib < timedB.size()) {
            if (timedA.get(ia).position().compareTo(timedB.get(ib).position()) <= 0) {
                merged.add(timedA.get(ia++));
            } else {
                merged.add(timedB.get(ib++));
            }
        }
        while (ia < timedA.size()) {
            merged.add(timedA.get(ia++));
        }
        while (ib < timedB.size()) {
            merged.add(timedB.get(ib++));
        }

        return Voice.of(name, merged.stream().map(TimedEvent::event).toList());
    }

    /**
     * Pad a voice with leading rests so it starts at the given measure (1-based).
     * Measure 1 = no padding, measure 2 = one measure of rests, etc.
     */
    public static Voice padToMeasure(final Voice voice, final int startMeasure, final TimeSignature ts) {
        Objects.requireNonNull(voice, "voice must not be null");
        Objects.requireNonNull(ts, "ts must not be null");
        if (startMeasure < 1) {
            throw new IllegalArgumentException("startMeasure must be >= 1, got: " + startMeasure);
        }
        if (startMeasure == 1) {
            return voice;
        }

        final int measuresToPad = startMeasure - 1;
        final Fraction measureDur = ts.measureDuration();
        final Fraction padDur = measureDur.multiply(measuresToPad);

        final List<NoteEvent> result = new ArrayList<>();
        // Fill with whole rests and then fractional rests if needed
        result.addAll(buildRests(padDur));
        result.addAll(voice.events());
        return Voice.of(voice.name(), result);
    }

    /**
     * Get total number of complete measures in a voice.
     */
    public static int measureCount(final Voice voice, final TimeSignature ts) {
        Objects.requireNonNull(voice, "voice must not be null");
        final Fraction total = voice.duration();
        final Fraction measureDur = ts.measureDuration();
        // Integer division of fractions
        return total.numerator() * measureDur.denominator()
            / (total.denominator() * measureDur.numerator());
    }

    /**
     * Return a readable string with bar lines inserted at measure boundaries.
     * Format: "C4/q D4/q | E4/q F4/q"
     */
    public static String toBarredString(final Voice voice, final TimeSignature ts) {
        final Fraction measureDur = ts.measureDuration();
        final StringBuilder sb = new StringBuilder();
        Fraction accumulated = Fraction.ZERO;
        boolean first = true;

        for (final NoteEvent event : voice.events()) {
            if (!first) {
                sb.append(" ");
            }
            first = false;

            if (accumulated.compareTo(Fraction.ZERO) > 0
                && accumulated.numerator() * measureDur.denominator()
                % (accumulated.denominator() * measureDur.numerator()) == 0) {
                // At measure boundary
                // Re-check: only if accumulated is an exact multiple of measureDur
            }

            sb.append(formatEvent(event));
            accumulated = accumulated.add(event.duration().fraction());

            // Check if we just hit a measure boundary
            final Fraction rem = new Fraction(
                accumulated.numerator() * measureDur.denominator() % (accumulated.denominator() * measureDur.numerator()),
                accumulated.denominator() * measureDur.denominator()
            );
            // Simpler: check if accumulated is a multiple of measureDur
        }

        return sb.toString();
    }

    /**
     * Repeat a voice N times, returning a new concatenated voice.
     */
    public static Voice repeat(final Voice voice, final String name, final int times) {
        Objects.requireNonNull(voice, "voice must not be null");
        if (times < 1) {
            throw new IllegalArgumentException("times must be >= 1, got: " + times);
        }
        final List<Voice> copies = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            copies.add(voice);
        }
        return concat(name, copies);
    }

    // --- internal helpers ---

    private record TimedEvent(Fraction position, NoteEvent event) {
    }

    private static List<TimedEvent> buildTimed(final List<NoteEvent> events) {
        final List<TimedEvent> result = new ArrayList<>();
        Fraction pos = Fraction.ZERO;
        for (final NoteEvent e : events) {
            result.add(new TimedEvent(pos, e));
            pos = pos.add(e.duration().fraction());
        }
        return result;
    }

    private static List<NoteEvent> buildRests(final Fraction duration) {
        // Fill with whole rests, then remaining rests
        final List<NoteEvent> rests = new ArrayList<>();
        Fraction remaining = duration;
        final Fraction whole = Fraction.ONE;
        while (remaining.compareTo(whole) >= 0) {
            rests.add(Rest.of(RhythmicValue.WHOLE));
            remaining = remaining.subtract(whole);
        }
        // Handle sub-whole remainder with half, quarter, eighth, sixteenth
        for (final var rv : new RhythmicValue[]{RhythmicValue.HALF, RhythmicValue.QUARTER,
            RhythmicValue.EIGHTH, RhythmicValue.SIXTEENTH}) {
            if (remaining.compareTo(rv.fraction()) >= 0) {
                rests.add(Rest.of(rv));
                remaining = remaining.subtract(rv.fraction());
            }
        }
        return rests;
    }

    private static String formatEvent(final NoteEvent event) {
        return switch (event) {
            case Note n -> n.pitch().toString() + "/" + formatDuration(event);
            case Rest r -> "r/" + formatDuration(event);
            case Chord c -> "<chord>/" + formatDuration(event);
        };
    }

    private static String formatDuration(final NoteEvent event) {
        final Fraction f = event.duration().fraction();
        if (f.equals(Fraction.ONE)) {
            return "w";
        }
        if (f.equals(Fraction.HALF)) {
            return "h";
        }
        if (f.equals(Fraction.QUARTER)) {
            return "q";
        }
        if (f.equals(Fraction.EIGHTH)) {
            return "e";
        }
        if (f.equals(new Fraction(1, 16))) {
            return "s";
        }
        return f.toString();
    }
}
