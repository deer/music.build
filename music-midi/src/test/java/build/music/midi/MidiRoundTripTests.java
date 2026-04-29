package build.music.midi;

import build.music.core.Chord;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.RhythmicValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        // The voice starts with a ProgramChange (instrument set by renderer), then 3 notes
        List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(4, events.size());
        assertInstanceOf(ProgramChange.class, events.get(0));
        Note n0 = (Note) events.get(1);
        assertEquals(SpelledPitch.parse("C4").midi(), n0.midi());
    }

    @Test
    void chordRoundTrip() throws Exception {
        var chord = build.music.core.Chord.of(
            List.of(SpelledPitch.parse("C4"), SpelledPitch.parse("E4"), SpelledPitch.parse("G4")),
            RhythmicValue.HALF,
            build.music.core.Velocity.MF
        );
        var voice = Voice.of("piano", List.of(chord));
        var score = Score.builder("ChordTest")
            .part(Part.piano("Piano", voice))
            .build();

        Path midiFile = tempDir.resolve("chord.mid");
        MidiWriter.write(MidiRenderer.render(score), midiFile);

        var result = MidiReader.readWithTempo(midiFile);
        List<NoteEvent> events = result.voices().get(0).events();
        assertEquals(2, events.size());
        assertInstanceOf(ProgramChange.class, events.get(0));
        Chord c = assertInstanceOf(Chord.class, events.get(1));
        assertEquals(3, c.pitches().size());
        assertEquals(SpelledPitch.parse("C4").midi(), c.pitches().get(0).midi());
        assertEquals(SpelledPitch.parse("E4").midi(), c.pitches().get(1).midi());
        assertEquals(SpelledPitch.parse("G4").midi(), c.pitches().get(2).midi());
    }

    @Test
    void controlChangeAndProgramChangeRoundTrip() throws Exception {
        var cc = new ControlChange(11, 100); // expression
        var pc = new ProgramChange(40);       // violin
        var note = Note.of(SpelledPitch.parse("A4"), RhythmicValue.QUARTER);
        var voice = Voice.of("strings", List.of(pc, cc, note));
        var score = Score.builder("CCTest")
            .part(Part.piano("Strings", voice))
            .build();

        Path midiFile = tempDir.resolve("cc.mid");
        MidiWriter.write(MidiRenderer.render(score), midiFile);

        List<NoteEvent> events = MidiReader.read(midiFile).get(0).events();
        // Renderer emits its own PC first, then our PC, CC, note
        long pcCount = events.stream().filter(e -> e instanceof ProgramChange).count();
        long ccCount = events.stream().filter(e -> e instanceof ControlChange cc2 && cc2.cc() == 11).count();
        assertTrue(pcCount >= 1, "expected at least one ProgramChange");
        assertEquals(1, ccCount, "expected one CC-11 event");
        // The note is last
        assertInstanceOf(Note.class, events.get(events.size() - 1));
    }

    @Test
    void multiChannelTrackSplitsIntoSeparateVoices() throws Exception {
        // Build a single-track MIDI sequence with notes on two channels
        int resolution = 480;
        Sequence seq = new Sequence(Sequence.PPQ, resolution);
        Track track = seq.createTrack();

        // Channel 0: C4 quarter note at tick 0
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 80), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0), resolution));

        // Channel 1: G4 quarter note at tick 0
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 1, 67, 80), 0));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 1, 67, 0), resolution));

        Path midiFile = tempDir.resolve("multichan.mid");
        MidiWriter.write(seq, midiFile);

        List<Voice> voices = MidiReader.read(midiFile);
        assertEquals(2, voices.size());

        // Each voice has exactly one note
        assertEquals(1, voices.get(0).events().size());
        assertEquals(1, voices.get(1).events().size());

        // Notes are on different pitches
        int midi0 = ((Note) voices.get(0).events().get(0)).midi();
        int midi1 = ((Note) voices.get(1).events().get(0)).midi();
        assertEquals(60, midi0); // C4 on ch0
        assertEquals(67, midi1); // G4 on ch1
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
