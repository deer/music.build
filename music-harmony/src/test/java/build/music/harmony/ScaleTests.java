package build.music.harmony;

import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScaleTests {

    @Test
    void cMajorPitches() {
        Scale scale = Scale.major(NoteName.C);
        List<SpelledPitch> pitches = scale.pitches(4);
        assertEquals(7, pitches.size());
        assertEquals("C4", pitches.get(0).toString());
        assertEquals("D4", pitches.get(1).toString());
        assertEquals("E4", pitches.get(2).toString());
        assertEquals("F4", pitches.get(3).toString());
        assertEquals("G4", pitches.get(4).toString());
        assertEquals("A4", pitches.get(5).toString());
        assertEquals("B4", pitches.get(6).toString());
    }

    @Test
    void fMajorPitchesSpelledCorrectly() {
        // F major has Bb, not A#
        Scale scale = Scale.major(NoteName.F);
        List<SpelledPitch> pitches = scale.pitches(4);
        assertEquals("F4", pitches.get(0).toString());
        assertEquals("G4", pitches.get(1).toString());
        assertEquals("A4", pitches.get(2).toString());
        assertEquals("Bb4", pitches.get(3).toString());
        assertEquals("C5", pitches.get(4).toString());
        assertEquals("D5", pitches.get(5).toString());
        assertEquals("E5", pitches.get(6).toString());
    }

    @Test
    void dMajorPitchesSpelledCorrectly() {
        // D major has F# and C#
        Scale scale = Scale.major(NoteName.D);
        List<SpelledPitch> pitches = scale.pitches(4);
        assertEquals("D4", pitches.get(0).toString());
        assertEquals("E4", pitches.get(1).toString());
        assertEquals("F#4", pitches.get(2).toString());
        assertEquals("G4", pitches.get(3).toString());
        assertEquals("A4", pitches.get(4).toString());
        assertEquals("B4", pitches.get(5).toString());
        assertEquals("C#5", pitches.get(6).toString());
    }

    @Test
    void abMajorPitchesSpelledCorrectly() {
        Scale scale = Scale.major(NoteName.A, Accidental.FLAT);
        List<SpelledPitch> pitches = scale.pitches(4);
        assertEquals("Ab4", pitches.get(0).toString());
        assertEquals("Bb4", pitches.get(1).toString());
        assertEquals("C5",  pitches.get(2).toString());
        assertEquals("Db5", pitches.get(3).toString());
        assertEquals("Eb5", pitches.get(4).toString());
        assertEquals("F5",  pitches.get(5).toString());
        assertEquals("G5",  pitches.get(6).toString());
    }

    @Test
    void aNaturalMinorPitches() {
        Scale scale = Scale.minor(NoteName.A);
        List<SpelledPitch> pitches = scale.pitches(4);
        assertEquals("A4", pitches.get(0).toString());
        assertEquals("B4", pitches.get(1).toString());
        assertEquals("C5", pitches.get(2).toString());
        assertEquals("D5", pitches.get(3).toString());
        assertEquals("E5", pitches.get(4).toString());
        assertEquals("F5", pitches.get(5).toString());
        assertEquals("G5", pitches.get(6).toString());
    }

    @Test
    void degreeMethod() {
        Scale scale = Scale.major(NoteName.C);
        assertEquals("C4", scale.degree(1, 4).toString());
        assertEquals("E4", scale.degree(3, 4).toString());
        assertEquals("G4", scale.degree(5, 4).toString());
    }

    @Test
    void containsPitchClass() {
        Scale cMajor = Scale.major(NoteName.C);
        assertTrue(cMajor.contains(SpelledPitch.parse("C4")));
        assertTrue(cMajor.contains(SpelledPitch.parse("G5")));
        assertFalse(cMajor.contains(SpelledPitch.parse("F#3")));
    }

    @Test
    void degreeOf() {
        Scale scale = Scale.major(NoteName.C);
        assertEquals(1, scale.degreeOf(SpelledPitch.parse("C4")).orElseThrow());
        assertEquals(5, scale.degreeOf(SpelledPitch.parse("G4")).orElseThrow());
        assertTrue(scale.degreeOf(SpelledPitch.parse("F#4")).isEmpty());
    }

    @Test
    void parse() {
        Scale s = Scale.parse("F# minor");
        assertEquals(NoteName.F, s.root());
        assertEquals(Accidental.SHARP, s.rootAccidental());
        assertEquals(ScaleType.NATURAL_MINOR, s.type());
    }

    @Test
    void gMajorPitches() {
        Scale scale = Scale.major(NoteName.G);
        List<SpelledPitch> pitches = scale.pitches(4);
        // G A B C D E F#
        assertEquals("G4", pitches.get(0).toString());
        assertEquals("A4", pitches.get(1).toString());
        assertEquals("B4", pitches.get(2).toString());
        assertEquals("C5", pitches.get(3).toString());
        assertEquals("D5", pitches.get(4).toString());
        assertEquals("E5", pitches.get(5).toString());
        assertEquals("F#5", pitches.get(6).toString());
    }
}
