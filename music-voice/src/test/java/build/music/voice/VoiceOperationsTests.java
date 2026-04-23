package build.music.voice;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceOperationsTests {

    private static Note note(String pitch) {
        return Note.of(SpelledPitch.parse(pitch), RhythmicValue.QUARTER);
    }

    private static Voice voice(String name, String... pitches) {
        List<NoteEvent> events = java.util.Arrays.stream(pitches)
            .map(VoiceOperationsTests::note)
            .map(n -> (NoteEvent) n)
            .toList();
        return Voice.of(name, events);
    }

    @Test
    void concatVarargs() {
        Voice a = voice("a", "C4", "D4", "E4", "F4");
        Voice b = voice("b", "G4", "A4", "B4", "C5");
        Voice ab = VoiceOperations.concat("ab", a, b);
        assertEquals(8, ab.events().size());
        assertEquals("C4", ((Note) ab.events().get(0)).pitch().toString());
        assertEquals("C5", ((Note) ab.events().get(7)).pitch().toString());
    }

    @Test
    void concatList() {
        Voice a = voice("a", "C4", "D4");
        Voice b = voice("b", "E4", "F4");
        Voice ab = VoiceOperations.concat("ab", List.of(a, b));
        assertEquals(4, ab.events().size());
    }

    @Test
    void repeat() {
        Voice motif = voice("m", "C4", "E4", "G4");
        Voice rep = VoiceOperations.repeat(motif, "m_x3", 3);
        assertEquals(9, rep.events().size());
        assertEquals("C4", ((Note) rep.events().get(0)).pitch().toString());
        assertEquals("C4", ((Note) rep.events().get(3)).pitch().toString());
        assertEquals("C4", ((Note) rep.events().get(6)).pitch().toString());
    }

    @Test
    void padToMeasure_measure1_noChange() {
        Voice v = voice("v", "C4", "D4", "E4", "F4");
        Voice padded = VoiceOperations.padToMeasure(v, 1, TimeSignature.COMMON_TIME);
        assertEquals(4, padded.events().size());
    }

    @Test
    void padToMeasure_measure2_addsOneMeasureOfRests() {
        Voice v = voice("v", "C4");
        // 4/4: measure 2 means prepend 1 whole rest
        Voice padded = VoiceOperations.padToMeasure(v, 2, TimeSignature.COMMON_TIME);
        // First event should be a rest of 1 whole note (= 1 measure)
        assertTrue(padded.events().get(0) instanceof Rest);
        assertEquals(RhythmicValue.WHOLE.fraction(), padded.events().get(0).duration().fraction());
        // Last event should be the original note
        assertInstanceOf(Note.class, padded.events().get(padded.events().size() - 1));
    }

    @Test
    void padToMeasure_measure3_addsTwoMeasuresOfRests() {
        Voice melody = voice("m", "C4", "D4", "E4", "F4");
        Voice padded = VoiceOperations.padToMeasure(melody, 3, TimeSignature.COMMON_TIME);
        // Should start with 2 whole rests
        assertEquals(2 + 4, padded.events().size());
        assertInstanceOf(Rest.class, padded.events().get(0));
        assertInstanceOf(Rest.class, padded.events().get(1));
        assertEquals("C4", ((Note) padded.events().get(2)).pitch().toString());
    }

    @Test
    void measureCount() {
        // 4 quarter notes in 4/4 = 1 measure
        Voice v = voice("v", "C4", "D4", "E4", "F4");
        assertEquals(1, VoiceOperations.measureCount(v, TimeSignature.COMMON_TIME));
    }

    @Test
    void measureCount_8Quarters_is2Measures() {
        Voice v = voice("v", "C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5");
        assertEquals(2, VoiceOperations.measureCount(v, TimeSignature.COMMON_TIME));
    }

    @Test
    void sliceByIndex() {
        Voice v = voice("v", "C4", "D4", "E4", "F4", "G4");
        Voice sliced = VoiceOperations.slice(v, "s", 1, 4);
        assertEquals(3, sliced.events().size());
        assertEquals("D4", ((Note) sliced.events().get(0)).pitch().toString());
    }

    @Test
    void splitByMeasure() {
        // 8 quarters in 4/4 = 2 measures
        Voice v = voice("v", "C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5");
        List<Voice> measures = VoiceOperations.splitByMeasure(v, TimeSignature.COMMON_TIME);
        assertEquals(2, measures.size());
        assertEquals(4, measures.get(0).events().size());
        assertEquals(4, measures.get(1).events().size());
    }

    @Test
    void merge() {
        Voice a = voice("a", "C4", "E4");
        Voice b = voice("b", "G4", "B4");
        Voice merged = VoiceOperations.merge("m", a, b);
        assertEquals(4, merged.events().size());
    }
}
