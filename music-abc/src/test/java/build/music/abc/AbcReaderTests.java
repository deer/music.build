package build.music.abc;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AbcReaderTests {

    private static final String SIMPLE_TUNE =
        "X:1\n" +
            "T:Simple Scale\n" +
            "M:4/4\n" +
            "L:1/4\n" +
            "K:C\n" +
            "C D E F | G A B c |\n";

    @Test
    void parsesTitle() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        assertEquals("Simple Scale", result.title());
    }

    @Test
    void parsesNoteCount() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        assertEquals(1, result.voices().size());
        final Voice voice = result.voices().get(0);
        assertEquals(8, voice.events().size());
    }

    @Test
    void parsesOctaves() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        final List<NoteEvent> events = result.voices().get(0).events();
        // C (uppercase) = C3, c (lowercase) = C4
        final Note c3 = (Note) events.get(0);
        assertEquals(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 3), c3.pitch().spelled());
        final Note c4 = (Note) events.get(7);
        assertEquals(SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4), c4.pitch().spelled());
    }

    @Test
    void parsesNoteLength() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        final Note c3 = (Note) result.voices().get(0).events().get(0);
        assertEquals(Fraction.of(1, 4), ((FractionDuration) c3.duration()).fraction());
    }

    @Test
    void keySignatureAppliesAccidentals() {
        final String gMajorTune =
            "X:1\n" +
                "T:G Scale\n" +
                "L:1/8\n" +
                "K:G\n" +
                "G A B c d e f g |\n";
        final AbcReader.AbcImport result = AbcReader.read(gMajorTune);
        final List<NoteEvent> events = result.voices().get(0).events();
        // f (index 6) should be F# due to G major key signature
        final Note f = (Note) events.get(6);
        assertEquals(NoteName.F, f.pitch().spelled().name());
        assertEquals(Accidental.SHARP, f.pitch().spelled().accidental());
    }

    @Test
    void explicitAccidentalOverridesKey() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:G\n" +
                "=f |\n";  // explicit natural overrides F# from G major
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final Note f = (Note) result.voices().get(0).events().get(0);
        assertEquals(Accidental.NATURAL, f.pitch().spelled().accidental());
    }

    @Test
    void measureAccidentalPersistsWithinBar() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "^f f | f |\n";  // first f# applies to second f in same bar; third f resets
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        final Note f1 = (Note) events.get(0);
        final Note f2 = (Note) events.get(1);
        final Note f3 = (Note) events.get(2);
        assertEquals(Accidental.SHARP, f1.pitch().spelled().accidental());
        assertEquals(Accidental.SHARP, f2.pitch().spelled().accidental());
        assertEquals(Accidental.NATURAL, f3.pitch().spelled().accidental());
    }

    @Test
    void octaveMarkers() {
        final String tune =
            "X:1\n" +
                "L:1/4\n" +
                "K:C\n" +
                "C, c' C,, c'' |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(2, ((Note) events.get(0)).pitch().spelled().octave()); // C, = C2
        assertEquals(5, ((Note) events.get(1)).pitch().spelled().octave()); // c' = C5
        assertEquals(1, ((Note) events.get(2)).pitch().spelled().octave()); // C,, = C1
        assertEquals(6, ((Note) events.get(3)).pitch().spelled().octave()); // c'' = C6
    }

    @Test
    void restParsed() {
        final String tune =
            "X:1\n" +
                "L:1/4\n" +
                "K:C\n" +
                "C z E |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(3, events.size());
        assertInstanceOf(Rest.class, events.get(1));
    }

    @Test
    void tiedNotesMerged() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "C2-C2 |\n";  // two tied quarter notes → one half note
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(1, events.size());
        final Note note = (Note) events.get(0);
        assertEquals(Fraction.of(1, 2), ((FractionDuration) note.duration()).fraction());
    }

    @Test
    void dottedNoteLength() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "C3/2 |\n";  // 3/2 * 1/8 = 3/16 (dotted eighth)
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final Note note = (Note) result.voices().get(0).events().get(0);
        assertEquals(Fraction.of(3, 16), ((FractionDuration) note.duration()).fraction());
    }

    @Test
    void tempoFromQField() {
        final String tune =
            "X:1\n" +
                "L:1/4\n" +
                "Q:1/4=160\n" +
                "K:C\n" +
                "C |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        assertEquals(160, result.tempo().bpm());
    }

    @Test
    void defaultTempoWhenNoQField() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        assertEquals(120, result.tempo().bpm());
    }

    @Test
    void commentsIgnored() {
        final String tune =
            "X:1\n" +
                "% This is a comment\n" +
                "T:Test\n" +
                "L:1/4\n" +
                "K:C\n" +
                "C D % inline comment\n" +
                "E F |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        assertEquals(4, result.voices().get(0).events().size());
    }

    @Test
    void voiceNameDerivedFromTitle() {
        final AbcReader.AbcImport result = AbcReader.read(SIMPLE_TUNE);
        assertEquals("simple_scale", result.voices().get(0).name());
    }

    @Test
    void chordEventParsed() {
        final String tune =
            "X:1\n" +
                "L:1/4\n" +
                "K:C\n" +
                "[CEG]2 |\n";  // C-E-G chord, half note
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(1, events.size());
        assertInstanceOf(Chord.class, events.get(0));
        final Chord chord = (Chord) events.get(0);
        assertEquals(3, chord.pitches().size());
        assertEquals(Fraction.of(1, 2), ((FractionDuration) chord.duration()).fraction());
    }

    @Test
    void chordPitchesCorrect() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "[CEG] |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final Chord chord = (Chord) result.voices().get(0).events().get(0);
        // pitches sorted by midi value: C3, E3, G3
        assertEquals(NoteName.C, ((SpelledPitch) chord.pitches().get(0)).name());
        assertEquals(NoteName.E, ((SpelledPitch) chord.pitches().get(1)).name());
        assertEquals(NoteName.G, ((SpelledPitch) chord.pitches().get(2)).name());
    }

    @Test
    void tupletScalesDuration() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "(3 C D E |\n";  // triplet: 3 eighths in the time of 2 → each note = 1/12
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(3, events.size());
        // each written 1/8, scaled by 2/3 → 1/12
        assertEquals(Fraction.of(1, 12), ((FractionDuration) ((Note) events.get(0)).duration()).fraction());
        assertEquals(Fraction.of(1, 12), ((FractionDuration) ((Note) events.get(1)).duration()).fraction());
        assertEquals(Fraction.of(1, 12), ((FractionDuration) ((Note) events.get(2)).duration()).fraction());
    }

    @Test
    void tupletExplicitRatio() {
        final String tune =
            "X:1\n" +
                "L:1/8\n" +
                "K:C\n" +
                "(5:4:5 C D E F G |\n";  // 5 notes in time of 4: each 1/8 → 4/40 = 1/10
        final AbcReader.AbcImport result = AbcReader.read(tune);
        final List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(5, events.size());
        assertEquals(Fraction.of(1, 10), ((FractionDuration) ((Note) events.get(0)).duration()).fraction());
    }

    @Test
    void multiVoiceParsed() {
        final String tune =
            "X:1\n" +
                "T:Duet\n" +
                "L:1/4\n" +
                "K:C\n" +
                "V:1\n" +
                "C D E F |\n" +
                "V:2\n" +
                "G A B c |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        assertEquals(2, result.voices().size());
        assertEquals("1", result.voices().get(0).name());
        assertEquals("2", result.voices().get(1).name());
        assertEquals(4, result.voices().get(0).events().size());
        assertEquals(4, result.voices().get(1).events().size());
    }

    @Test
    void multiVoiceNoteContent() {
        final String tune =
            "X:1\n" +
                "L:1/4\n" +
                "K:C\n" +
                "V:soprano\n" +
                "c d e f |\n" +
                "V:bass\n" +
                "C, D, E, F, |\n";
        final AbcReader.AbcImport result = AbcReader.read(tune);
        assertEquals(2, result.voices().size());
        final Note firstSoprano = (Note) result.voices().get(0).events().get(0);
        final Note firstBass = (Note) result.voices().get(1).events().get(0);
        assertEquals(4, firstSoprano.pitch().spelled().octave()); // c = C4
        assertEquals(2, firstBass.pitch().spelled().octave());    // C, = C2
    }
}
