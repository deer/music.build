package build.music.pitch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnharmonicTests {

    @Test
    void cSharp4_andDb4_areEnharmonic() {
        var cs4 = SpelledPitch.of(NoteName.C, Accidental.SHARP, 4);
        var db4 = SpelledPitch.of(NoteName.D, Accidental.FLAT, 4);
        assertTrue(Enharmonic.areEnharmonic(cs4, db4));
    }

    @Test
    void c4_andC4_areNotEnharmonic() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        assertFalse(Enharmonic.areEnharmonic(c4, c4));
    }

    @Test
    void c4_andD4_areNotEnharmonic() {
        var c4 = SpelledPitch.of(NoteName.C, Accidental.NATURAL, 4);
        var d4 = SpelledPitch.of(NoteName.D, Accidental.NATURAL, 4);
        assertFalse(Enharmonic.areEnharmonic(c4, d4));
    }

    @Test
    void allEnharmonics_containsBothSpellings() {
        var cs4 = SpelledPitch.of(NoteName.C, Accidental.SHARP, 4);
        var enharmonics = Enharmonic.allEnharmonics(cs4);
        assertTrue(enharmonics.contains(SpelledPitch.of(NoteName.D, Accidental.FLAT, 4)));
        assertTrue(enharmonics.contains(cs4));
    }
}
