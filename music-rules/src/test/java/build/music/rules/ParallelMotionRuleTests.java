package build.music.rules;

import build.music.core.Note;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelMotionRuleTests {

    @Test
    void detectsParallelFifths() {
        // soprano: C4 → D4 (up M2)
        // bass:    F3 → G3 (up M2)
        // Interval 1: C4-F3 = P5 (7 semitones), Interval 2: D4-G3 = P5 (7 semitones)
        // Both move up → parallel fifths
        Voice soprano = Voice.of("s", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER)
        ));
        Voice bass = Voice.of("b", List.of(
            Note.of(SpelledPitch.parse("F3"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G3"), RhythmicValue.QUARTER)
        ));
        ParallelMotionRule rule = new ParallelMotionRule();
        List<Violation> violations = rule.checkPair(soprano, "s", bass, "b");
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).message().contains("fifths"));
    }

    @Test
    void noViolationForContraryMotion() {
        // soprano: C4 → E4 (up M3)
        // bass:    G3 → D3 (down P4)
        // contrary motion — no parallel fifth even if intervals are different
        Voice soprano = Voice.of("s", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER)
        ));
        Voice bass = Voice.of("b", List.of(
            Note.of(SpelledPitch.parse("G3"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D3"), RhythmicValue.QUARTER)
        ));
        ParallelMotionRule rule = new ParallelMotionRule();
        assertTrue(rule.checkPair(soprano, "s", bass, "b").isEmpty());
    }

    @Test
    void skipsVoicesWithDifferentRhythms() {
        Voice a = Voice.of("a", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.HALF)
        ));
        Voice b = Voice.of("b", List.of(
            Note.of(SpelledPitch.parse("F3"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G3"), RhythmicValue.QUARTER)
        ));
        ParallelMotionRule rule = new ParallelMotionRule();
        // Different event counts → skip
        assertTrue(rule.checkPair(a, "a", b, "b").isEmpty());
    }

    @Test
    void detectsParallelOctaves() {
        // Two voices moving in parallel octaves
        // Voice a: C4 → D4
        // Voice b: C3 → D3 (octave below, same direction)
        Voice a = Voice.of("a", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER)
        ));
        Voice b = Voice.of("b", List.of(
            Note.of(SpelledPitch.parse("C3"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D3"), RhythmicValue.QUARTER)
        ));
        ParallelMotionRule rule = new ParallelMotionRule();
        List<Violation> violations = rule.checkPair(a, "a", b, "b");
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).message().contains("octaves"));
    }
}
