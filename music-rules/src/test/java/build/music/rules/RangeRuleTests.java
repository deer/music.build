package build.music.rules;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.instrument.Instruments;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RangeRuleTests {

    @Test
    void noViolationsWhenInRange() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G5"), RhythmicValue.QUARTER)
        ));
        RangeRule rule = new RangeRule(Instruments.FLUTE);
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().noneMatch(v -> v.severity().equals("error")));
    }

    @Test
    void errorWhenBelowRange() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER),  // safely in comfortable range
            Note.of(SpelledPitch.parse("C2"), RhythmicValue.QUARTER)   // way below flute range
        ));
        RangeRule rule = new RangeRule(Instruments.FLUTE);
        List<Violation> violations = rule.check(voice, "test");
        assertEquals(1, violations.size());
        assertEquals("error", violations.get(0).severity());
        assertTrue(violations.get(0).message().contains("below"));
        assertEquals(1, violations.get(0).noteIndex());
    }

    @Test
    void errorWhenAboveRange() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C8"), RhythmicValue.QUARTER)  // above flute range
        ));
        RangeRule rule = new RangeRule(Instruments.FLUTE);
        List<Violation> violations = rule.check(voice, "test");
        assertEquals(1, violations.size());
        assertEquals("error", violations.get(0).severity());
        assertTrue(violations.get(0).message().contains("above"));
    }

    @Test
    void warningForExtremeRegister() {
        // Piccolo's low extreme: D5 is in range but right at the edge
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("D5"), RhythmicValue.QUARTER)
        ));
        RangeRule rule = new RangeRule(Instruments.PICCOLO);
        List<Violation> violations = rule.check(voice, "test");
        // D5 is the lowest note of piccolo — within full range but outside comfortable range
        assertFalse(violations.isEmpty());
    }
}
