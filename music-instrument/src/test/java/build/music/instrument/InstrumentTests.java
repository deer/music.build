package build.music.instrument;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentTests {

    @Test
    void fluteRangeContainsMiddleC() {
        assertTrue(Instruments.FLUTE.writtenRange().contains(SpelledPitch.parse("C4")));
    }

    @Test
    void fluteRangeDoesNotContainC2() {
        assertFalse(Instruments.FLUTE.writtenRange().contains(SpelledPitch.parse("C2")));
    }

    @Test
    void pianoRangeIsWide() {
        assertTrue(Instruments.PIANO.writtenRange().semitoneSpan() > 80);
    }

    @Test
    void canPlay_allInRange() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G5"), RhythmicValue.QUARTER)
        );
        assertTrue(Instruments.FLUTE.canPlay(events));
    }

    @Test
    void canPlay_noteOutOfRange() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C2"), RhythmicValue.QUARTER)
        );
        assertFalse(Instruments.FLUTE.canPlay(events));
    }

    @Test
    void outOfRangeNotes_findsCorrectIndices() {
        List<NoteEvent> events = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C2"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER)
        );
        List<Integer> violations = Instruments.FLUTE.outOfRangeNotes(events);
        assertEquals(List.of(1), violations);
    }

    @Test
    void byName() {
        assertTrue(Instruments.byName("Flute").isPresent());
        assertTrue(Instruments.byName("Piano").isPresent());
        assertFalse(Instruments.byName("Kazoo").isPresent());
    }

    @Test
    void byFamily_woodwinds() {
        List<Instrument> woodwinds = Instruments.byFamily(InstrumentFamily.WOODWIND);
        assertFalse(woodwinds.isEmpty());
        assertTrue(woodwinds.stream().allMatch(i -> i.family() == InstrumentFamily.WOODWIND));
    }

    @Test
    void ensembleHasCorrectSize() {
        assertEquals(4, Ensemble.STRING_QUARTET.instruments().size());
        assertEquals(5, Ensemble.WOODWIND_QUINTET.instruments().size());
        assertEquals(3, Ensemble.PIANO_TRIO.instruments().size());
    }

    @Test
    void clef() {
        assertEquals("treble", Instruments.FLUTE.clef());
        assertEquals("bass",   Instruments.TUBA.clef());
    }
}
