package build.music.midi;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import build.music.time.TimeSignature;
import org.junit.jupiter.api.Test;

import javax.sound.midi.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MidiRendererTests {

    private static final Note C4Q = Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER);
    private static final Note E4Q = Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER);
    private static final Note G4Q = Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER);

    @Test
    void renderScoreProducesCorrectTrackCount() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q, E4Q, G4Q));
        var score = Score.builder("Test")
            .part(Part.piano("Piano", voice))
            .build();

        Sequence seq = MidiRenderer.render(score);
        // Track 0 (conductor) + 1 per part
        assertEquals(2, seq.getTracks().length);
    }

    @Test
    void renderScoreHasCorrectResolution() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q));
        var score = Score.builder("Test").part(Part.piano("P", voice)).build();
        Sequence seq = MidiRenderer.render(score);
        assertEquals(480, seq.getResolution());
    }

    @Test
    void fractionToTicksQuarterNote() {
        assertEquals(480, MidiRenderer.fractionToTicks(Fraction.QUARTER));
    }

    @Test
    void fractionToTicksHalfNote() {
        assertEquals(960, MidiRenderer.fractionToTicks(Fraction.HALF));
    }

    @Test
    void fractionToTicksWholeNote() {
        assertEquals(1920, MidiRenderer.fractionToTicks(Fraction.ONE));
    }

    @Test
    void fractionToTicksEighthNote() {
        assertEquals(240, MidiRenderer.fractionToTicks(Fraction.EIGHTH));
    }

    @Test
    void renderSingleVoice() throws InvalidMidiDataException {
        List<NoteEvent> events = List.of(C4Q, Rest.of(RhythmicValue.QUARTER), E4Q);
        Sequence seq = MidiRenderer.render(events, Tempo.of(120), 0, 0);
        assertEquals(2, seq.getTracks().length); // conductor + 1 track
    }

    @Test
    void renderMultipleParts() throws InvalidMidiDataException {
        var v1 = Voice.of("A", List.of(C4Q));
        var v2 = Voice.of("B", List.of(E4Q));
        var score = Score.builder("Test")
            .part(Part.piano("Piano", v1))
            .part(Part.strings("Strings", v2))
            .build();
        Sequence seq = MidiRenderer.render(score);
        assertEquals(3, seq.getTracks().length); // conductor + 2 parts
    }
}
