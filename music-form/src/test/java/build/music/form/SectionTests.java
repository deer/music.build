package build.music.form;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SectionTests {

    private static Voice oneBarVoice(String name) {
        return Voice.of(name, List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        ));
    }

    @Test
    void duration() {
        Section s = Section.of("A", "A", Map.of("m", oneBarVoice("m")),
            4, TimeSignature.COMMON_TIME);
        // 4 measures × 4/4 = 4 whole notes
        assertEquals(Fraction.of(4, 1), s.duration());
    }

    @Test
    void voiceAccess() {
        Voice v = oneBarVoice("melody");
        Section s = Section.of("A", "A", Map.of("melody", v), 1, TimeSignature.COMMON_TIME);
        assertTrue(s.voice("melody").isPresent());
        assertFalse(s.voice("bass").isPresent());
    }

    @Test
    void voiceNames() {
        Section s = Section.of("A", "A",
            Map.of("melody", oneBarVoice("melody"), "bass", oneBarVoice("bass")),
            1, TimeSignature.COMMON_TIME);
        assertEquals(2, s.voiceNames().size());
        assertTrue(s.voiceNames().contains("melody"));
        assertTrue(s.voiceNames().contains("bass"));
    }
}
