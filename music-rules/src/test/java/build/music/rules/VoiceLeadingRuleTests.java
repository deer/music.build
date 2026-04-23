package build.music.rules;

import build.music.core.Note;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceLeadingRuleTests {

    @Test
    void noViolationsForSmoothMelody() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        ));
        VoiceLeadingRule rule = new VoiceLeadingRule();
        assertTrue(rule.check(voice, "test").isEmpty());
    }

    @Test
    void suggestionForModerateLeap() {
        // C4→G5 = 19 semitones (minor 13th) — large but common in bass resets, compound melodies
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G5"), RhythmicValue.QUARTER)  // 19 semitones
        ));
        VoiceLeadingRule rule = new VoiceLeadingRule();
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().anyMatch(v -> v.severity().equals("suggestion") &&
            v.message().contains("semitones")));
    }

    @Test
    void warningForVeryLargeLeap() {
        // C4→Ab5 = 20 semitones (major 13th) — exceeds two octaves, genuine concern
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("Ab5"), RhythmicValue.QUARTER)  // 20 semitones
        ));
        VoiceLeadingRule rule = new VoiceLeadingRule();
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().anyMatch(v -> v.severity().equals("warning") &&
            v.message().contains("semitones")));
    }

    @Test
    void suggestionForRepeatedNotes() {
        Voice voice = Voice.of("test", List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)  // 5th repetition
        ));
        VoiceLeadingRule rule = new VoiceLeadingRule();
        List<Violation> violations = rule.check(voice, "test");
        assertTrue(violations.stream().anyMatch(v -> v.severity().equals("suggestion")));
    }
}
