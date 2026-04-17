package build.music.core;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.PitchClass;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChordSymbolTests {

    @Test
    void parse_Cmaj7_roundTrips() {
        var sym = ChordSymbol.parse("Cmaj7");
        assertEquals("Cmaj7", sym.toString());
    }

    @Test
    void parse_Dm() {
        var sym = ChordSymbol.parse("Dm");
        assertEquals(NoteName.D, sym.root());
        assertEquals(Accidental.NATURAL, sym.rootAccidental());
        assertEquals(ChordQuality.MINOR, sym.quality());
    }

    @Test
    void parse_FSharpMinor() {
        var sym = ChordSymbol.parse("F#m");
        assertEquals(NoteName.F, sym.root());
        assertEquals(Accidental.SHARP, sym.rootAccidental());
        assertEquals(ChordQuality.MINOR, sym.quality());
    }

    @Test
    void parse_Bb7() {
        var sym = ChordSymbol.parse("Bb7");
        assertEquals(NoteName.B, sym.root());
        assertEquals(Accidental.FLAT, sym.rootAccidental());
        assertEquals(ChordQuality.DOMINANT_7, sym.quality());
    }

    @Test
    void cMajor_toPitches_containsCEG() {
        var sym = ChordSymbol.of(NoteName.C, Accidental.NATURAL, ChordQuality.MAJOR);
        var pitches = sym.toPitches(4);
        var pitchClasses = pitches.stream().map(p -> p.pitchClass()).toList();
        assertTrue(pitchClasses.contains(PitchClass.C));
        assertTrue(pitchClasses.contains(PitchClass.E));
        assertTrue(pitchClasses.contains(PitchClass.G));
    }

    @Test
    void parse_roundTrips_multiple() {
        for (var s : List.of("Cmaj7", "Dm", "G7", "Fdim", "Aaug", "Bm7")) {
            assertEquals(s, ChordSymbol.parse(s).toString(), "Round-trip failed: " + s);
        }
    }
}
