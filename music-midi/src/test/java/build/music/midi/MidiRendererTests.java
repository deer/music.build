package build.music.midi;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.harmony.Key;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.SectionMarker;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void swing_appliedToOffBeatEighthChords() throws InvalidMidiDataException {
        // Two eighth-note chords: beat 1 (on-beat) and beat 2 (off-beat)
        var c4 = SpelledPitch.parse("C4");
        var e4 = SpelledPitch.parse("E4");
        var chord = Chord.of(List.of(c4, e4), RhythmicValue.EIGHTH, Velocity.MF);
        var voice = Voice.of("comp", List.of(chord, chord));
        // 2:1 swing ratio = 0.667
        var score = Score.builder("Test")
            .swingRatio(Fraction.of(2, 3))
            .part(Part.piano("Piano", voice))
            .build();

        Sequence seq = MidiRenderer.render(score);
        Track track = seq.getTracks()[1]; // part track

        // Collect all NOTE_ON ticks
        long[] onTicks = java.util.stream.IntStream.range(0, track.size())
            .mapToObj(track::get)
            .filter(e -> e.getMessage() instanceof ShortMessage sm && sm.getCommand() == ShortMessage.NOTE_ON)
            .mapToLong(MidiEvent::getTick)
            .distinct().sorted().toArray();

        // On-beat chord: tick 0; off-beat chord: tick 240 + swingDelay
        // swingDelay = round((2/3 - 0.5) * 480) = round(0.1667 * 480) = round(80) = 80
        assertEquals(0, onTicks[0], "on-beat chord must start at tick 0");
        assertEquals(320, onTicks[1], "off-beat chord must be swung to tick 320 (240 + 80)");
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

    @Test
    void conductorTrackHasScoreTitleAsTrackName() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q));
        var score = Score.builder("My Score").part(Part.piano("Piano", voice)).build();
        Sequence seq = MidiRenderer.render(score);
        Track conductor = seq.getTracks()[0];
        String name = findTrackName(conductor);
        assertEquals("My Score", name);
    }

    @Test
    void partTrackHasVoiceNameAsTrackName() throws InvalidMidiDataException {
        var voice = Voice.of("bass", List.of(C4Q));
        var score = Score.builder("Test").part(Part.piano("bass", voice)).build();
        Sequence seq = MidiRenderer.render(score);
        Track partTrack = seq.getTracks()[1];
        assertEquals("bass", findTrackName(partTrack));
    }

    @Test
    void keySignatureEmittedOnConductorTrack() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q));
        // G major = 1 sharp
        var score = Score.builder("Test")
            .key(Key.major(NoteName.G))
            .part(Part.piano("Piano", voice))
            .build();
        Sequence seq = MidiRenderer.render(score);
        Track conductor = seq.getTracks()[0];
        byte[] keySig = findMetaData(conductor, 0x59);
        assertEquals(1, keySig[0], "G major should have 1 sharp");
        assertEquals(0, keySig[1], "major key");
    }

    @Test
    void keySignatureMinorEmitted() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q));
        // A minor = 0 sharps/flats
        var score = Score.builder("Test")
            .key(Key.minor(NoteName.A))
            .part(Part.piano("Piano", voice))
            .build();
        Sequence seq = MidiRenderer.render(score);
        byte[] keySig = findMetaData(seq.getTracks()[0], 0x59);
        assertEquals(0, keySig[0], "A minor has no accidentals");
        assertEquals(1, keySig[1], "minor mode");
    }

    @Test
    void noKeySignatureWhenKeyNotSet() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q));
        var score = Score.builder("Test").part(Part.piano("Piano", voice)).build();
        Sequence seq = MidiRenderer.render(score);
        assertTrue(findMetaOrNull(seq.getTracks()[0], 0x59) == null, "no key sig expected");
    }

    @Test
    void sectionMarkersEmittedAtCorrectTicks() throws InvalidMidiDataException {
        var voice = Voice.of("melody", List.of(C4Q, E4Q, G4Q, C4Q));
        var score = Score.builder("Test")
            .sectionMarkers(List.of(
                new SectionMarker("intro", 1),
                new SectionMarker("verse", 3)
            ))
            .part(Part.piano("Piano", voice))
            .build();
        Sequence seq = MidiRenderer.render(score);

        List<MarkerEntry> found = collectMarkers(seq.getTracks()[0]);
        assertEquals(2, found.size());
        assertEquals("intro", found.get(0).name());
        assertEquals(0L, found.get(0).tick());
        assertEquals("verse", found.get(1).name());
        // bar 3 start = 2 completed measures * 4 quarters * 480 ticks = 3840
        assertEquals(3840L, found.get(1).tick());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String findTrackName(final Track track) {
        for (int i = 0; i < track.size(); i++) {
            final var msg = track.get(i).getMessage();
            if (msg instanceof MetaMessage mm && mm.getType() == 0x03) {
                return new String(mm.getData(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static byte[] findMetaData(final Track track, final int type) {
        final byte[] data = findMetaOrNull(track, type);
        if (data == null) {
            throw new AssertionError("No meta message of type 0x" + Integer.toHexString(type) + " found");
        }
        return data;
    }

    private static byte[] findMetaOrNull(final Track track, final int type) {
        for (int i = 0; i < track.size(); i++) {
            final var msg = track.get(i).getMessage();
            if (msg instanceof MetaMessage mm && mm.getType() == type) {
                return mm.getData();
            }
        }
        return null;
    }

    private record MarkerEntry(String name, long tick) { }

    private static List<MarkerEntry> collectMarkers(final Track track) {
        final List<MarkerEntry> result = new java.util.ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            final MidiEvent evt = track.get(i);
            if (evt.getMessage() instanceof MetaMessage mm && mm.getType() == 0x06) {
                result.add(new MarkerEntry(new String(mm.getData(), StandardCharsets.UTF_8), evt.getTick()));
            }
        }
        return result;
    }
}
