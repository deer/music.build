package build.music.voice;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeasureSliceTests {

    @Test
    void isComplete_whenExactDuration() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER)
        );
        Fraction total = Fraction.of(4, 4);
        MeasureSlice slice = new MeasureSlice(1, events, total);
        assertTrue(slice.isComplete(TimeSignature.COMMON_TIME));
    }

    @Test
    void isIncomplete_whenShort() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER)
        );
        MeasureSlice slice = new MeasureSlice(1, events, Fraction.QUARTER);
        assertFalse(slice.isComplete(TimeSignature.COMMON_TIME));
    }

    @Test
    void pitchesExcludesRests() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Rest.of(RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Rest.of(RhythmicValue.QUARTER)
        );
        MeasureSlice slice = new MeasureSlice(1, events, Fraction.ONE);
        List<SpelledPitch> pitches = slice.pitches();
        assertEquals(2, pitches.size());
        assertEquals("C4", pitches.get(0).toString());
        assertEquals("E4", pitches.get(1).toString());
    }
}
