package build.music.harmony;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyTests {

    @Test
    void cMajorRelativeIsAMinor() {
        Key cMajor = Key.major(NoteName.C);
        Key relative = cMajor.relative();
        assertEquals(NoteName.A, relative.tonic());
        assertEquals(Accidental.NATURAL, relative.accidental());
        assertTrue(relative.minor());
    }

    @Test
    void aMinorRelativeIsCMajor() {
        Key aMinor = Key.minor(NoteName.A);
        Key relative = aMinor.relative();
        assertEquals(NoteName.C, relative.tonic());
        assertFalse(relative.minor());
    }

    @Test
    void cMajorDominantIsGMajor() {
        Key dominant = Key.major(NoteName.C).dominant();
        assertEquals(NoteName.G, dominant.tonic());
        assertEquals(Accidental.NATURAL, dominant.accidental());
        assertFalse(dominant.minor());
    }

    @Test
    void signatureAccidentals() {
        assertEquals(0,  Key.major(NoteName.C).signatureAccidentals());
        assertEquals(1,  Key.major(NoteName.G).signatureAccidentals());
        assertEquals(2,  Key.major(NoteName.D).signatureAccidentals());
        assertEquals(-1, Key.major(NoteName.F).signatureAccidentals());
        assertEquals(-2, Key.major(NoteName.B, Accidental.FLAT).signatureAccidentals());
        assertEquals(-4, Key.major(NoteName.A, Accidental.FLAT).signatureAccidentals());
        assertEquals(0,  Key.minor(NoteName.A).signatureAccidentals());
        assertEquals(1,  Key.minor(NoteName.E).signatureAccidentals());
        assertEquals(-1, Key.minor(NoteName.D).signatureAccidentals());
    }

    @Test
    void accidentalNotesForGMajor() {
        List<NoteName> notes = Key.major(NoteName.G).accidentalNotes();
        assertEquals(List.of(NoteName.F), notes);
    }

    @Test
    void accidentalNotesForDMajor() {
        List<NoteName> notes = Key.major(NoteName.D).accidentalNotes();
        assertEquals(List.of(NoteName.F, NoteName.C), notes);
    }

    @Test
    void accidentalNotesForFMajor() {
        List<NoteName> notes = Key.major(NoteName.F).accidentalNotes();
        assertEquals(List.of(NoteName.B), notes);
    }

    @Test
    void parseKey() {
        Key k = Key.parse("F# minor");
        assertEquals(NoteName.F, k.tonic());
        assertEquals(Accidental.SHARP, k.accidental());
        assertTrue(k.minor());
    }

    @Test
    void parallelKey() {
        Key cMajor = Key.major(NoteName.C);
        Key cMinor = cMajor.parallel();
        assertEquals(NoteName.C, cMinor.tonic());
        assertTrue(cMinor.minor());
    }
}
