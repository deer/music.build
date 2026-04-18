package build.music.harmony;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.time.DottedValue;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HarmonizerTests {

    @Test
    void harmonizeReturnsOneNotePerChord() {
        Key cMajor = Key.major(NoteName.C);
        ChordProgression progression = ChordProgression.I_IV_V_I();
        // A 4-bar melody (4 quarter notes per bar)
        List<NoteEvent> melody = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.WHOLE),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.WHOLE),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.WHOLE),
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.WHOLE)
        );

        List<NoteEvent> harmony = Harmonizer.harmonize(melody, cMajor, progression, 3, TimeSignature.COMMON_TIME);
        assertEquals(4, harmony.size());
        // First chord should be C (root of I)
        assertEquals("C3", ((Note) harmony.get(0)).pitch().toString());
        // Second chord root: F (IV)
        assertEquals("F3", ((Note) harmony.get(1)).pitch().toString());
        // Third chord root: G (V)
        assertEquals("G3", ((Note) harmony.get(2)).pitch().toString());
    }

    @Test
    void suggestHarmonyForCMajorMelody() {
        Key cMajor = Key.major(NoteName.C);
        // Simple C major arpeggio melody
        List<NoteEvent> melody = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C5"), RhythmicValue.QUARTER)
        );

        ChordProgression suggestion = Harmonizer.suggestHarmony(melody, cMajor, TimeSignature.COMMON_TIME);
        assertNotNull(suggestion);
        assertFalse(suggestion.chords().isEmpty());
        // Should suggest I (C major) for a melody of C-E-G-C
        assertEquals(ScaleDegree.I, suggestion.chords().get(0).degree());
    }

    @Test
    void harmonize_waltzTime_producesDoттedHalfNotes() {
        Key cMajor = Key.major(NoteName.C);
        ChordProgression progression = ChordProgression.I_IV_V_I();
        var dottedHalf = new DottedValue(RhythmicValue.HALF, 1);
        List<NoteEvent> melody = List.of(
            Note.of(SpelledPitch.parse("C4"), dottedHalf),
            Note.of(SpelledPitch.parse("F4"), dottedHalf),
            Note.of(SpelledPitch.parse("G4"), dottedHalf),
            Note.of(SpelledPitch.parse("C4"), dottedHalf)
        );

        List<NoteEvent> harmony = Harmonizer.harmonize(melody, cMajor, progression, 3, TimeSignature.WALTZ_TIME);
        assertEquals(4, harmony.size());
        // Each chord should last one 3/4 measure = dotted half
        Fraction expected = Fraction.of(3, 4);
        harmony.forEach(e -> assertEquals(expected, e.duration().fraction(),
            "Expected dotted-half duration in 3/4, got: " + e.duration().fraction()));
    }

    @Test
    void suggestHarmonyNotEmpty() {
        Key gMajor = Key.major(NoteName.G);
        List<NoteEvent> melody = List.of(
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("A4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("B4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER)
        );
        ChordProgression suggestion = Harmonizer.suggestHarmony(melody, gMajor, TimeSignature.COMMON_TIME);
        assertNotNull(suggestion);
        assertFalse(suggestion.chords().isEmpty());
    }
}
