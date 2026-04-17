package build.music.form;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormalPlanTests {

    private static Voice oneBarVoice(String name, String pitch) {
        return Voice.of(name, List.of(
            Note.of(SpelledPitch.parse(pitch), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse(pitch), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse(pitch), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse(pitch), RhythmicValue.QUARTER)
        ));
    }

    @Test
    void abaFormHasThreeSections() {
        Voice a = oneBarVoice("m", "C4");
        Voice b = oneBarVoice("m", "G4");

        FormalPlan plan = FormBuilder.create("Test ABA")
            .tempo(120)
            .timeSignature(TimeSignature.COMMON_TIME)
            .section("A", "m", a, 1)
            .section("B", "m", b, 1)
            .repeatSection("A")
            .build();

        assertEquals(3, plan.sections().size());
        assertEquals(3, plan.totalMeasures());
    }

    @Test
    void describeABA() {
        Voice a = oneBarVoice("m", "C4");
        Voice b = oneBarVoice("m", "G4");

        FormalPlan plan = FormBuilder.create("Test ABA")
            .section("A", "m", a, 1)
            .section("B", "m", b, 1)
            .repeatSection("A")
            .build();

        assertEquals("ABA: A(1 bar) → B(1 bar) → A(1 bar)", plan.describe());
    }

    @Test
    void toScore_singleVoice() {
        Voice a = oneBarVoice("m", "C4");
        Voice b = oneBarVoice("m", "G4");

        FormalPlan plan = FormBuilder.create("ABA Score Test")
            .section("A", "m", a, 1)
            .section("B", "m", b, 1)
            .repeatSection("A")
            .build();

        Score score = plan.toScore(Map.of("m", 0));
        assertEquals(1, score.scoreParts().size());
        assertEquals("m", score.scoreParts().get(0).name());
        // 3 sections × 4 quarter notes each = 12 notes
        assertEquals(12, score.scoreParts().get(0).voice().events().size());
    }

    @Test
    void toScore_missingVoiceGetsRests() {
        Voice melody = oneBarVoice("melody", "E4");
        Voice bass = oneBarVoice("bass", "C3");

        // Section A has both voices, Section B only has melody
        Section sectionA = Section.of("A", "A",
            Map.of("melody", melody, "bass", bass), 1, TimeSignature.COMMON_TIME);
        Section sectionB = Section.of("B", "B",
            Map.of("melody", oneBarVoice("melody", "D4")), 1, TimeSignature.COMMON_TIME);

        FormalPlan plan = FormBuilder.create("Duet ABA")
            .section("A", sectionA)
            .section("B", sectionB)
            .repeatSection("A")
            .build();

        Score score = plan.toScore(Map.of("melody", 0, "bass", 42));
        assertEquals(2, score.scoreParts().size());

        // Find bass part
        var bassPart = score.scoreParts().stream()
            .filter(p -> p.name().equals("bass"))
            .findFirst().orElseThrow();

        // bass has: A(4 notes) + B(1 rest) + A(4 notes) = 9 events
        assertEquals(9, bassPart.voice().events().size());
        // B section: should be a rest
        assertInstanceOf(Rest.class, bassPart.voice().events().get(4));
    }

    @Test
    void toScore_voiceLongerThanDeclaredMeasures_isTrimmedToSectionLength() {
        // 8-bar voice (8 × 4 quarter notes in 4/4)
        List<NoteEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < 32; i++) {
            events.add(Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER));
        }
        Voice eightBarVoice = Voice.of("v", events);

        // Section declares only 1 measure — voice is 8× longer than declared
        FormalPlan plan = FormBuilder.create("Bleed Test")
            .timeSignature(TimeSignature.COMMON_TIME)
            .section("S", "v", eightBarVoice, 1)
            .build();

        Score score = plan.toScore(Map.of("v", 0));
        Voice assembled = score.scoreParts().get(0).voice();

        // Must be exactly 4 quarter notes (1 bar in 4/4), not 32
        assertEquals(4, assembled.events().size(),
            "Voice longer than declared measures must be trimmed to section length");
    }

    @Test
    void sectionLookup() {
        Voice a = oneBarVoice("m", "C4");
        FormalPlan plan = FormBuilder.create("Test")
            .section("A", "m", a, 1)
            .build();

        assertTrue(plan.section("A").isPresent());
        assertFalse(plan.section("B").isPresent());
    }
}
