package build.music.harmony;

import build.music.core.ChordSymbol;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordProgressionTests {

    @Test
    void iIVVI_inCMajor() {
        List<ChordSymbol> chords = ChordProgression.I_IV_V_I().inKey(Key.major(NoteName.C));
        assertEquals(4, chords.size());
        assertEquals("Cmaj", chords.get(0).toString());
        assertEquals("Fmaj", chords.get(1).toString());
        assertEquals("Gmaj", chords.get(2).toString());
        assertEquals("Cmaj", chords.get(3).toString());
    }

    @Test
    void iiVI_inBbMajor() {
        Key bb = Key.major(NoteName.B, Accidental.FLAT);
        List<ChordSymbol> chords = ChordProgression.ii_V_I().inKey(bb);
        assertEquals(3, chords.size());
        assertEquals("Cm", chords.get(0).toString());
        assertEquals("Fmaj", chords.get(1).toString());
        assertEquals("Bbmaj", chords.get(2).toString());
    }

    @Test
    void romanNumeralV7() {
        RomanNumeral v7 = RomanNumeral.parse("V7");
        ChordSymbol chord = v7.chordInKey(Key.major(NoteName.C));
        assertEquals("G7", chord.toString());
    }

    @Test
    void romanNumeralii_inCMajor() {
        RomanNumeral ii = RomanNumeral.parse("ii");
        ChordSymbol chord = ii.chordInKey(Key.major(NoteName.C));
        assertEquals("Dm", chord.toString());
    }

    @Test
    void romanNumeralI_inGMajor() {
        RomanNumeral I = RomanNumeral.parse("I");
        ChordSymbol chord = I.chordInKey(Key.major(NoteName.G));
        assertEquals("Gmaj", chord.toString());
    }

    @Test
    void parseProgression() {
        ChordProgression prog = ChordProgression.parse("I IV V I");
        assertEquals(4, prog.chords().size());
    }

    @Test
    void parseWithDashes() {
        ChordProgression prog = ChordProgression.parse("I - IV - V - I");
        assertEquals(4, prog.chords().size());
    }

    @Test
    void viio_isDiminished() {
        RomanNumeral viio = RomanNumeral.parse("viio");
        assertEquals("viio", viio.toString());
        ChordSymbol chord = viio.chordInKey(Key.major(NoteName.C));
        assertEquals("Bdim", chord.toString());
    }

    @Test
    void diatonicMajorHasSevenChords() {
        assertEquals(7, RomanNumeral.diatonicMajor().size());
    }
}
