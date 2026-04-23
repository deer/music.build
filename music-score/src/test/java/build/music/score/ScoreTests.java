package build.music.score;

import build.music.core.Note;
import build.music.core.Rest;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreTests {

    private static final Note C4Q = Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER);
    private static final Note E4Q = Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER);
    private static final Note G4Q = Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER);
    private static final Rest REST_Q = Rest.of(RhythmicValue.QUARTER);

    @Test
    void voiceDuration() {
        var voice = Voice.of("test", List.of(C4Q, E4Q, G4Q));
        assertEquals(Fraction.of(3, 4), voice.duration());
    }

    @Test
    void voiceWithRest() {
        var voice = Voice.of("test", List.of(C4Q, REST_Q));
        assertEquals(Fraction.HALF, voice.duration());
    }

    @Test
    void voiceTranspose() {
        var voice = Voice.of("test", List.of(C4Q, E4Q));
        var transposed = voice.transpose(SpelledInterval.parse("M3"));
        var events = transposed.events();
        assertEquals(SpelledPitch.parse("E4"), ((Note) events.get(0)).pitch().spelled());
        assertEquals(SpelledPitch.parse("G#4"), ((Note) events.get(1)).pitch().spelled());
    }

    @Test
    void voiceTransformReverse() {
        var voice = Voice.of("test", List.of(C4Q, E4Q, G4Q));
        var reversed = voice.transform(events -> events.reversed());
        var events = reversed.events();
        assertEquals(SpelledPitch.parse("G4"), ((Note) events.get(0)).pitch().spelled());
        assertEquals(SpelledPitch.parse("C4"), ((Note) events.get(2)).pitch().spelled());
    }

    @Test
    void voiceEventsIsUnmodifiable() {
        var voice = Voice.of("test", List.of(C4Q));
        assertThrows(UnsupportedOperationException.class,
            () -> voice.events().add(E4Q));
    }

    @Test
    void partFactories() {
        var voice = Voice.of("v", List.of(C4Q));
        var piano = Part.piano("piano", voice);
        assertEquals(0, piano.midiChannel());
        assertEquals(0, piano.midiProgram());

        var strings = Part.strings("strings", voice);
        assertEquals(48, strings.midiProgram());
    }

    @Test
    void scoreBuilder() {
        var voice = Voice.of("melody", List.of(C4Q, E4Q, G4Q, C4Q));
        var score = Score.builder("Test")
            .timeSignature(TimeSignature.COMMON_TIME)
            .tempo(Tempo.of(100))
            .part(Part.piano("Piano", voice))
            .build();

        assertEquals("Test", score.title());
        assertEquals(Tempo.of(100), score.tempo());
        assertEquals(1, score.scoreParts().size());
        assertTrue(score.part("Piano").isPresent());
        assertFalse(score.part("Violin").isPresent());
    }

    @Test
    void scoreDuration() {
        var short_ = Voice.of("short", List.of(C4Q));
        var long_ = Voice.of("long", List.of(C4Q, E4Q, G4Q, C4Q));
        var score = Score.builder("Test")
            .part(Part.piano("A", short_))
            .part(Part.strings("B", long_))
            .build();
        assertEquals(Fraction.ONE, score.duration());
    }
}
