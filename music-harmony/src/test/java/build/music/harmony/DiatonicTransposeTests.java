package build.music.harmony;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DiatonicTransposeTests {

    @Test
    void cUpTwoSteps_isE() {
        Key cMajor = Key.major(NoteName.C);
        SpelledPitch result = DiatonicTranspose.transpose(SpelledPitch.parse("C4"), cMajor, 2);
        assertEquals("E4", result.toString());
    }

    @Test
    void eUpOneStep_isF() {
        Key cMajor = Key.major(NoteName.C);
        SpelledPitch result = DiatonicTranspose.transpose(SpelledPitch.parse("E4"), cMajor, 1);
        assertEquals("F4", result.toString());
    }

    @Test
    void bUpOneStep_wrapsToC5() {
        Key cMajor = Key.major(NoteName.C);
        SpelledPitch result = DiatonicTranspose.transpose(SpelledPitch.parse("B4"), cMajor, 1);
        assertEquals("C5", result.toString());
    }

    @Test
    void cDownOneStep_isB3() {
        Key cMajor = Key.major(NoteName.C);
        SpelledPitch result = DiatonicTranspose.transpose(SpelledPitch.parse("C4"), cMajor, -1);
        assertEquals("B3", result.toString());
    }

    @Test
    void aMinorUpTwoSteps_isC() {
        Key aMinor = Key.minor(NoteName.A);
        SpelledPitch result = DiatonicTranspose.transpose(SpelledPitch.parse("A4"), aMinor, 2);
        assertEquals("C5", result.toString());
    }

    @Test
    void transposeList() {
        Key cMajor = Key.major(NoteName.C);
        List<NoteEvent> notes = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER)
        );
        var transposed = DiatonicTranspose.transpose(notes, cMajor, 1);
        assertEquals(3, transposed.size());
        // C→D, E→F, G→A
        assertEquals("D4", ((Note) transposed.get(0)).pitch().toString());
        assertEquals("F4", ((Note) transposed.get(1)).pitch().toString());
        assertEquals("A4", ((Note) transposed.get(2)).pitch().toString());
    }

    @Test
    void restsPassThrough() {
        Key cMajor = Key.major(NoteName.C);
        List<NoteEvent> notes = List.of(Rest.of(RhythmicValue.QUARTER));
        var transposed = DiatonicTranspose.transpose(notes, cMajor, 2);
        assertEquals(1, transposed.size());
        assertInstanceOf(Rest.class, transposed.get(0));
    }
}
