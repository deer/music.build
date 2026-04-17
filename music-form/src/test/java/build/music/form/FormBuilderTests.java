package build.music.form;

import build.music.core.Note;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormBuilderTests {

    private static Voice oneBarVoice(String name) {
        return Voice.of(name, List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.WHOLE)
        ));
    }

    @Test
    void buildSimplePlan() {
        FormalPlan plan = FormBuilder.create("Simple")
            .tempo(100)
            .timeSignature(TimeSignature.COMMON_TIME)
            .section("A", "m", oneBarVoice("m"), 1)
            .build();

        assertEquals("Simple", plan.title());
        assertEquals(100, plan.tempo().bpm());
        assertEquals(1, plan.sections().size());
    }

    @Test
    void repeatSection() {
        FormalPlan plan = FormBuilder.create("ABA")
            .section("A", "m", oneBarVoice("m"), 1)
            .section("B", "m", oneBarVoice("m"), 1)
            .repeatSection("A")
            .build();

        assertEquals(3, plan.sections().size());
        assertEquals("A", plan.sections().get(0).name());
        assertEquals("B", plan.sections().get(1).name());
        assertEquals("A", plan.sections().get(2).name());
    }

    @Test
    void repeatSectionWithNewLabel() {
        FormalPlan plan = FormBuilder.create("Test")
            .section("A", "m", oneBarVoice("m"), 1)
            .repeatSection("A", "A'")
            .build();

        assertEquals("A'", plan.sections().get(1).label());
    }

    @Test
    void repeatNonexistentSection_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            FormBuilder.create("Test")
                .section("A", "m", oneBarVoice("m"), 1)
                .repeatSection("Z")
                .build());
    }

    @Test
    void emptyPlan_throws() {
        assertThrows(IllegalStateException.class, () ->
            FormBuilder.create("Empty").build());
    }

    @Test
    void totalMeasures() {
        FormalPlan plan = FormBuilder.create("Test")
            .section("A", "m", oneBarVoice("m"), 4)
            .section("B", "m", oneBarVoice("m"), 8)
            .repeatSection("A")
            .build();

        assertEquals(16, plan.totalMeasures());
    }

    @Test
    void setEnding_removesEndingSectionFromPlaySequence() {
        // Ending sections must not appear as standalone sections in the assembled plan.
        // They are replacements for the tail of another section, not independent bars.
        FormalPlan plan = FormBuilder.create("Test")
            .section("A", "m", oneBarVoice("m"), 2)
            .section("A_end", "m", oneBarVoice("m"), 1)
            .setEnding("A", 1, "A_end")
            .build();

        assertEquals(1, plan.sections().size(),
            "A_end registered as an ending must not remain as a standalone section");
        assertEquals("A", plan.sections().get(0).name());
    }
}
