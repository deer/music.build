package build.music.midi;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.Sequence;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MidiRoundTripTests {

    @TempDir
    Path tempDir;

    @Test
    void writeAndReadBack() throws Exception {
        var c4 = Note.of(SpelledPitch.parse("C4"), RhythmicValue.QUARTER);
        var e4 = Note.of(SpelledPitch.parse("E4"), RhythmicValue.QUARTER);
        var g4 = Note.of(SpelledPitch.parse("G4"), RhythmicValue.QUARTER);
        var voice = Voice.of("melody", List.of(c4, e4, g4));
        var score = Score.builder("Test")
            .part(Part.piano("Piano", voice))
            .build();

        Sequence seq = MidiRenderer.render(score);
        Path midiFile = tempDir.resolve("test.mid");
        MidiWriter.write(seq, midiFile);
        assertTrue(midiFile.toFile().exists());
        assertTrue(midiFile.toFile().length() > 0);

        var result = MidiReader.readWithTempo(midiFile);
        assertEquals(120, result.tempo().bpm());
        assertFalse(result.voices().isEmpty());

        // The voice should have 3 notes with correct MIDI numbers
        List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(3, events.size());
        Note n0 = (Note) events.get(0);
        assertEquals(SpelledPitch.parse("C4").midi(), n0.midi());
    }

    @Test
    void midiToSpelledPitchNaturals() {
        assertEquals(SpelledPitch.parse("C4"), MidiReader.midiToSpelledPitch(60));
        assertEquals(SpelledPitch.parse("D4"), MidiReader.midiToSpelledPitch(62));
        assertEquals(SpelledPitch.parse("E4"), MidiReader.midiToSpelledPitch(64));
        assertEquals(SpelledPitch.parse("A4"), MidiReader.midiToSpelledPitch(69));
    }

    @Test
    void midiToSpelledPitchSharps() {
        assertEquals(SpelledPitch.parse("C#4"), MidiReader.midiToSpelledPitch(61));
        assertEquals(SpelledPitch.parse("F#4"), MidiReader.midiToSpelledPitch(66));
    }
}
