package build.music.rules;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeterRuleTests {

    @Test
    void noViolationsForCompleteMeasure() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        ));
        MeterRule rule = new MeterRule(TimeSignature.COMMON_TIME);
        assertTrue(rule.check(voice, "test").isEmpty());
    }

    @Test
    void warningForIncompleteMeasure() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER)  // only 3 beats in 4/4
        ));
        MeterRule rule = new MeterRule(TimeSignature.COMMON_TIME);
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().anyMatch(v ->
            v.severity().equals("warning") && v.message().contains("incomplete")));
    }

    @Test
    void errorForMeasureOverflow() {
        // A half + a whole exceeds the 4/4 measure (1 + 0.5 > 1)
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.HALF),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.WHOLE)  // 1.5 beats total > 1 measure
        ));
        MeterRule rule = new MeterRule(TimeSignature.COMMON_TIME);
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().anyMatch(v -> v.severity().equals("error")));
    }

    @Test
    void twoCompleteMeasures_noViolations() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.WHOLE),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.WHOLE)
        ));
        MeterRule rule = new MeterRule(TimeSignature.COMMON_TIME);
        assertTrue(rule.check(voice, "test").isEmpty());
    }
}
