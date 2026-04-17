package build.music.midi;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import javax.sound.midi.InvalidMidiDataException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pre-conversion canary benchmark for the codemodel reframing (Tier 1, Phase 1).
 *
 * Measures heap delta and wall time for rendering a 1000-event Voice to MIDI via
 * MidiRenderer.render(score). Run on main before the conversion starts to establish
 * the baseline. Re-run after Phase 6 to verify the delta is acceptable.
 *
 * Thresholds (from ROADMAP.md Phase 1):
 *   - memory: re-run result must be less than ~5x baseline
 *   - wall time: re-run result must be less than ~3x baseline
 *
 * PRE-CONVERSION BASELINE (recorded 2026-04-13, Java 25, Linux):
 *   wall time : 1 ms   (stable across 5 runs after one throw-away warm-up)
 *   heap delta: 850 KB (totalMemory - freeMemory before/after render call)
 *
 * POST-CONVERSION NUMBERS (recorded 2026-04-14, after codemodel-reframe Tier 1):
 *   wall time : 2 ms   (2x baseline — well within 3x threshold)
 *   heap delta: 870 KB (1.02x baseline — well within 5x threshold)
 */
class RenderBenchmark {

    /** Pitches to cycle through — covers a two-octave C major scale (14 pitches). */
    private static final SpelledPitch[] PITCHES = {
        SpelledPitch.parse("C4"), SpelledPitch.parse("D4"), SpelledPitch.parse("E4"),
        SpelledPitch.parse("F4"), SpelledPitch.parse("G4"), SpelledPitch.parse("A4"),
        SpelledPitch.parse("B4"), SpelledPitch.parse("C5"), SpelledPitch.parse("D5"),
        SpelledPitch.parse("E5"), SpelledPitch.parse("F5"), SpelledPitch.parse("G5"),
        SpelledPitch.parse("A5"), SpelledPitch.parse("B5"),
    };

    /** Durations to cycle through — mix of quarters and eighths for realism. */
    private static final RhythmicValue[] DURATIONS = {
        RhythmicValue.QUARTER, RhythmicValue.QUARTER,
        RhythmicValue.EIGHTH, RhythmicValue.EIGHTH,
    };

    private static List<NoteEvent> build1000Events() {
        List<NoteEvent> events = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            SpelledPitch pitch = PITCHES[i % PITCHES.length];
            RhythmicValue dur = DURATIONS[i % DURATIONS.length];
            events.add(Note.of(pitch, dur));
        }
        return events;
    }

    @Test
    void canaryBenchmark_render1000Events() throws InvalidMidiDataException {
        List<NoteEvent> events = build1000Events();
        Voice voice = Voice.of("bench", events);
        Score score = Score.builder("Benchmark")
            .part(Part.piano("Piano", voice))
            .build();

        // Throw-away warm-up run to load classes before measuring.
        MidiRenderer.render(score);

        // --- Measurement ---
        Runtime rt = Runtime.getRuntime();

        // GC before measurement to reduce noise.
        rt.gc();
        Thread.yield();

        long heapBefore = rt.totalMemory() - rt.freeMemory();
        long timeBefore = System.nanoTime();

        var seq = MidiRenderer.render(score);

        long timeAfter = System.nanoTime();
        long heapAfter = rt.totalMemory() - rt.freeMemory();

        long wallMs = (timeAfter - timeBefore) / 1_000_000;
        long heapDeltaKb = (heapAfter - heapBefore) / 1024;

        System.out.printf(
            "[RenderBenchmark] wall=%d ms  heapDelta=%d KB  tracks=%d%n",
            wallMs, heapDeltaKb, seq.getTracks().length);

        // Sanity: result must be a valid sequence.
        assertNotNull(seq);
    }
}
