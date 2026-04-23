package build.music.harmony;

import build.music.core.ChordSymbol;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HarmonicAnalyzerTests {

    @Test
    void analyzeLabelsChordsByFunction() {
        Key cMajor = Key.major(NoteName.C);
        List<ChordSymbol> chords = List.of(
            ChordSymbol.parse("Cmaj"),
            ChordSymbol.parse("Fmaj"),
            ChordSymbol.parse("Gmaj"),
            ChordSymbol.parse("Cmaj")
        );
        List<RomanNumeral> labels = HarmonicAnalyzer.analyze(chords, cMajor);
        assertEquals(4, labels.size());
        assertEquals(ScaleDegree.I, labels.get(0).degree());
        assertEquals(ScaleDegree.IV, labels.get(1).degree());
        assertEquals(ScaleDegree.V, labels.get(2).degree());
        assertEquals(ScaleDegree.I, labels.get(3).degree());
    }

    @Test
    void detectKeyReturnsSomething() {
        // C major melody: C D E F G A B C
        List<NoteEvent> melody = List.of(
            Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("D4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("F4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("A4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("B4"), RhythmicValue.QUARTER),
            Note.of(SpelledPitch.parse("C5"), RhythmicValue.HALF)
        );
        Key detected = HarmonicAnalyzer.detectKey(melody);
        assertNotNull(detected);
        // The detected key should be C major or A minor (both fit the same pitch classes)
        assertFalse(detected.minor()); // should prefer major for this scale
        assertEquals(NoteName.C, detected.tonic());
    }

    @Test
    void findProgressionLocatesPattern() {
        Key cMajor = Key.major(NoteName.C);
        List<ChordSymbol> chords = List.of(
            ChordSymbol.parse("Cmaj"),
            ChordSymbol.parse("Fmaj"),
            ChordSymbol.parse("Gmaj"),
            ChordSymbol.parse("Cmaj"),
            ChordSymbol.parse("Fmaj"),
            ChordSymbol.parse("Gmaj"),
            ChordSymbol.parse("Cmaj")
        );
        ChordProgression pattern = ChordProgression.parse("IV V I");
        List<Integer> matches = HarmonicAnalyzer.findProgression(chords, pattern, cMajor);
        assertEquals(2, matches.size());
        assertEquals(1, matches.get(0));
        assertEquals(4, matches.get(1));
    }
}
