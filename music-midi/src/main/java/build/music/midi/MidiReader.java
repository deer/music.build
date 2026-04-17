package build.music.midi;

import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.Tempo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Reads a Standard MIDI File back into Voice objects.
 *
 * Limitations:
 * - Enharmonic spelling is lost; black keys are spelled with sharps (C#, not Db)
 * - Complex MIDI features (pitch bend, aftertouch, SysEx) are ignored
 */
public final class MidiReader {

    private MidiReader() {}

    public record MidiImport(List<Voice> voices, Tempo tempo) {}

    /** Read a MIDI file; extract note events per track. Conductor tracks are skipped. */
    public static List<Voice> read(final Path path) throws IOException, InvalidMidiDataException {
        return readWithTempo(path).voices();
    }

    /** Read a MIDI file; extract note events and tempo. */
    public static MidiImport readWithTempo(final Path path) throws IOException, InvalidMidiDataException {
        final Sequence sequence = MidiSystem.getSequence(path.toFile());
        final int resolution = sequence.getResolution(); // ticks per quarter note

        Tempo tempo = Tempo.of(120);
        final List<Voice> voices = new ArrayList<>();
        int voiceIndex = 0;

        for (final Track track : sequence.getTracks()) {
            // Extract tempo from any track (usually track 0)
            final Tempo extracted = extractTempo(track);
            if (extracted != null) {
                tempo = extracted;
            }

            final List<NoteEvent> events = extractNoteEvents(track, resolution);
            if (!events.isEmpty()) {
                voices.add(Voice.of("voice-" + voiceIndex++, events));
            }
        }

        return new MidiImport(Collections.unmodifiableList(voices), tempo);
    }

    private static Tempo extractTempo(final Track track) {
        for (int i = 0; i < track.size(); i++) {
            final MidiEvent event = track.get(i);
            if (event.getMessage() instanceof MetaMessage meta && meta.getType() == 0x51) {
                final byte[] data = meta.getData();
                if (data.length >= 3) {
                    final int micros = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                    final int bpm = 60_000_000 / micros;
                    return Tempo.of(Math.clamp(bpm, 1, 400));
                }
            }
        }
        return null;
    }

    private static List<NoteEvent> extractNoteEvents(final Track track, final int resolution) {
        // Map from (channel<<8|noteNum) → tick of NOTE_ON
        final Map<Integer, Long> noteOnTicks = new HashMap<>();
        // Sorted list of (startTick, endTick, midiNote)
        final List<long[]> noteSegments = new ArrayList<>();

        for (int i = 0; i < track.size(); i++) {
            final MidiEvent event = track.get(i);
            final MidiMessage msg = event.getMessage();
            final long tick = event.getTick();

            if (msg instanceof ShortMessage sm) {
                final int cmd = sm.getCommand();
                final int channel = sm.getChannel();
                final int noteNum = sm.getData1();
                final int velocity = sm.getData2();
                final int key = (channel << 8) | noteNum;

                if (cmd == ShortMessage.NOTE_ON && velocity > 0) {
                    noteOnTicks.put(key, tick);
                } else if (cmd == ShortMessage.NOTE_OFF || (cmd == ShortMessage.NOTE_ON && velocity == 0)) {
                    final Long onTick = noteOnTicks.remove(key);
                    if (onTick != null) {
                        noteSegments.add(new long[]{onTick, tick, noteNum});
                    }
                }
            }
        }

        if (noteSegments.isEmpty()) {
            return List.of();
        }

        // Sort by start tick
        noteSegments.sort(Comparator.comparingLong(a -> a[0]));

        final List<NoteEvent> events = new ArrayList<>();
        long cursor = 0;

        // Minimum gap (in ticks) to treat as a rest: 1/32 of a whole note = resolution/8
        final long minRestTicks = Math.max(1, resolution / 8);

        for (final long[] seg : noteSegments) {
            final long startTick = seg[0];
            final long endTick = seg[1];
            final int midiNote = (int) seg[2];

            // Gap before this note → rest (ignore tiny articulation gaps)
            if (startTick - cursor >= minRestTicks) {
                final Fraction restDur = ticksToFraction(startTick - cursor, resolution);
                events.add(Rest.of(new FractionDuration(restDur)));
            }

            final Fraction noteDur = ticksToFraction(endTick - startTick, resolution);
            final SpelledPitch pitch = midiToSpelledPitch(midiNote);
            events.add(Note.of(pitch, new FractionDuration(noteDur)));
            cursor = endTick;
        }

        return Collections.unmodifiableList(events);
    }

    /** Convert ticks to a Fraction of a whole note. Snaps to common rhythmic values. */
    private static Fraction ticksToFraction(final long ticks, final int resolution) {
        // resolution = ticks per quarter note; quarter = 1/4 of whole note
        // fraction = ticks / (resolution * 4)
        final double raw = (double) ticks / (resolution * 4.0);

        // Snap to standard rhythmic fractions
        final Fraction[] standards = {
            Fraction.of(1, 1),   // whole
            Fraction.of(1, 2),   // half
            Fraction.of(3, 8),   // dotted quarter
            Fraction.of(1, 4),   // quarter
            Fraction.of(3, 16),  // dotted eighth
            Fraction.of(1, 8),   // eighth
            Fraction.of(3, 32),  // dotted sixteenth
            Fraction.of(1, 16),  // sixteenth
            Fraction.of(1, 32),  // thirty-second
        };

        Fraction best = standards[0];
        double bestDiff = Math.abs(standards[0].toDouble() - raw);
        for (final Fraction f : standards) {
            final double diff = Math.abs(f.toDouble() - raw);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = f;
            }
        }
        // If close enough to snapped value use it; otherwise use exact rational
        if (bestDiff < 0.01) {
            return best;
        }
        // Exact: ticks / (resolution*4) = ticks / (4*resolution)
        return Fraction.of((int) ticks, resolution * 4);
    }

    /**
     * Convert a MIDI note number to a SpelledPitch.
     * Black keys spelled with sharps (C#, D#, F#, G#, A#).
     */
    static SpelledPitch midiToSpelledPitch(final int midiNote) {
        final int octave = (midiNote / 12) - 1;
        final int semitone = midiNote % 12;
        // Prefer sharps for black keys
        return switch (semitone) {
            case 0  -> SpelledPitch.of(NoteName.C, Accidental.NATURAL, octave);
            case 1  -> SpelledPitch.of(NoteName.C, Accidental.SHARP, octave);
            case 2  -> SpelledPitch.of(NoteName.D, Accidental.NATURAL, octave);
            case 3  -> SpelledPitch.of(NoteName.D, Accidental.SHARP, octave);
            case 4  -> SpelledPitch.of(NoteName.E, Accidental.NATURAL, octave);
            case 5  -> SpelledPitch.of(NoteName.F, Accidental.NATURAL, octave);
            case 6  -> SpelledPitch.of(NoteName.F, Accidental.SHARP, octave);
            case 7  -> SpelledPitch.of(NoteName.G, Accidental.NATURAL, octave);
            case 8  -> SpelledPitch.of(NoteName.G, Accidental.SHARP, octave);
            case 9  -> SpelledPitch.of(NoteName.A, Accidental.NATURAL, octave);
            case 10 -> SpelledPitch.of(NoteName.A, Accidental.SHARP, octave);
            case 11 -> SpelledPitch.of(NoteName.B, Accidental.NATURAL, octave);
            default -> throw new IllegalArgumentException("Unexpected semitone: " + semitone);
        };
    }
}
