package build.music.midi;

import build.music.core.Chord;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.core.Rest;
import build.music.core.Velocity;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.Pitch;
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
import java.util.TreeMap;
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
 * <p>
 * Limitations:
 * - Enharmonic spelling is lost; black keys are spelled with sharps (C#, not Db)
 * - Complex MIDI features (pitch bend, aftertouch, SysEx) are ignored
 */
public final class MidiReader {

    private MidiReader() {
    }

    public record TempoMarker(long tick, Tempo tempo) {
    }

    public record MidiImport(List<Voice> voices, Tempo tempo, List<TempoMarker> tempoChanges) {
    }

    /**
     * Read a MIDI file; extract note events per track. Conductor tracks are skipped.
     */
    public static List<Voice> read(final Path path) throws IOException, InvalidMidiDataException {
        return readWithTempo(path).voices();
    }

    /**
     * Read a MIDI file; extract note events and tempo.
     */
    public static MidiImport readWithTempo(final Path path) throws IOException, InvalidMidiDataException {
        final Sequence sequence = MidiSystem.getSequence(path.toFile());
        final int resolution = sequence.getResolution(); // ticks per quarter note

        // Collect all tempo markers across all tracks, sorted by tick
        final List<TempoMarker> allTempoMarkers = new ArrayList<>();
        final List<Voice> voices = new ArrayList<>();
        int voiceIndex = 0;

        for (final Track track : sequence.getTracks()) {
            allTempoMarkers.addAll(extractTempoMarkers(track));

            final String trackName = extractTrackName(track);
            final Map<Integer, List<NoteEvent>> byChannel = extractChannelEvents(track, resolution);

            for (final Map.Entry<Integer, List<NoteEvent>> entry : byChannel.entrySet()) {
                final int channel = entry.getKey();
                final List<NoteEvent> events = entry.getValue();
                if (events.isEmpty()) {
                    continue;
                }
                final String name;
                if (trackName != null && byChannel.size() == 1) {
                    name = trackName;
                } else if (trackName != null) {
                    name = trackName + "-ch" + channel;
                } else if (byChannel.size() == 1) {
                    name = "voice-" + voiceIndex;
                } else {
                    name = "voice-" + voiceIndex + "-ch" + channel;
                }
                voices.add(Voice.of(name, events));
                voiceIndex++;
            }
        }

        allTempoMarkers.sort(Comparator.comparingLong(TempoMarker::tick));

        final Tempo initialTempo = allTempoMarkers.isEmpty()
            ? Tempo.of(120)
            : allTempoMarkers.get(0).tempo();
        final List<TempoMarker> changes = allTempoMarkers.size() > 1
            ? Collections.unmodifiableList(allTempoMarkers.subList(1, allTempoMarkers.size()))
            : List.of();

        return new MidiImport(Collections.unmodifiableList(voices), initialTempo, changes);
    }

    private static String extractTrackName(final Track track) {
        for (int i = 0; i < track.size(); i++) {
            final MidiEvent event = track.get(i);
            if (event.getMessage() instanceof MetaMessage meta && meta.getType() == 0x03) {
                return new String(meta.getData(), java.nio.charset.StandardCharsets.UTF_8).strip();
            }
        }
        return null;
    }

    private static List<TempoMarker> extractTempoMarkers(final Track track) {
        final List<TempoMarker> markers = new ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            final MidiEvent event = track.get(i);
            if (event.getMessage() instanceof MetaMessage meta && meta.getType() == 0x51) {
                final byte[] data = meta.getData();
                if (data.length >= 3) {
                    final int micros = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                    final int bpm = 60_000_000 / micros;
                    markers.add(new TempoMarker(event.getTick(), Tempo.of(Math.clamp(bpm, 1, 400))));
                }
            }
        }
        return markers;
    }

    // Segment kind constants for unified long[] format {tick, endTick, data1, data2, channel, kind}
    private static final int KIND_CC = 0;
    private static final int KIND_PC = 1;
    private static final int KIND_NOTE = 2;

    /**
     * Returns one event list per MIDI channel that has content in this track.
     * ControlChange and ProgramChange events are interleaved before the notes at their tick.
     * Notes on the same channel sharing a start tick become a Chord; others become Notes.
     * Channels are returned in ascending order via TreeMap.
     */
    private static Map<Integer, List<NoteEvent>> extractChannelEvents(final Track track, final int resolution) {
        // Map from (channel<<8|noteNum) → {startTick, velocity}
        final Map<Integer, long[]> noteOnTicks = new HashMap<>();
        // Unified segments: {tick, endTick, data1, data2, channel, kind}
        final List<long[]> allSegments = new ArrayList<>();

        for (int i = 0; i < track.size(); i++) {
            final MidiEvent event = track.get(i);
            final MidiMessage msg = event.getMessage();
            final long tick = event.getTick();

            if (msg instanceof ShortMessage sm) {
                final int cmd = sm.getCommand();
                final int channel = sm.getChannel();
                final int d1 = sm.getData1();
                final int d2 = sm.getData2();
                final int key = (channel << 8) | d1;

                if (cmd == ShortMessage.NOTE_ON && d2 > 0) {
                    noteOnTicks.put(key, new long[]{tick, d2});
                } else if (cmd == ShortMessage.NOTE_OFF || (cmd == ShortMessage.NOTE_ON && d2 == 0)) {
                    final long[] onInfo = noteOnTicks.remove(key);
                    if (onInfo != null) {
                        allSegments.add(new long[]{onInfo[0], tick, d1, onInfo[1], channel, KIND_NOTE});
                    }
                } else if (cmd == ShortMessage.CONTROL_CHANGE) {
                    allSegments.add(new long[]{tick, tick, d1, d2, channel, KIND_CC});
                } else if (cmd == ShortMessage.PROGRAM_CHANGE) {
                    allSegments.add(new long[]{tick, tick, d1, 0, channel, KIND_PC});
                }
            }
        }

        // Group segments by channel
        final Map<Integer, List<long[]>> segsByChannel = new TreeMap<>();
        for (final long[] seg : allSegments) {
            segsByChannel.computeIfAbsent((int) seg[4], k -> new ArrayList<>()).add(seg);
        }

        final Map<Integer, List<NoteEvent>> result = new TreeMap<>();
        for (final Map.Entry<Integer, List<long[]>> entry : segsByChannel.entrySet()) {
            result.put(entry.getKey(), buildEvents(entry.getValue(), resolution));
        }
        return result;
    }

    private static List<NoteEvent> buildEvents(final List<long[]> segments, final int resolution) {
        // Sort by tick, then kind (CC=0, PC=1 before NOTE=2), then data1 for note determinism
        segments.sort(Comparator.comparingLong((long[] a) -> a[0])
            .thenComparingLong(a -> a[5])
            .thenComparingLong(a -> a[2]));

        final List<NoteEvent> events = new ArrayList<>();
        long cursor = 0;
        final long minRestTicks = Math.max(1, resolution / 8);

        int i = 0;
        while (i < segments.size()) {
            final long tick = segments.get(i)[0];
            final int kind = (int) segments.get(i)[5];

            if (kind == KIND_CC) {
                events.add(new ControlChange((int) segments.get(i)[2], (int) segments.get(i)[3]));
                i++;
            } else if (kind == KIND_PC) {
                events.add(new ProgramChange((int) segments.get(i)[2]));
                i++;
            } else {
                // NOTE group: insert rest for gap, then emit Note or Chord
                if (tick - cursor >= minRestTicks) {
                    events.add(Rest.of(new FractionDuration(ticksToFraction(tick - cursor, resolution))));
                }

                int j = i;
                while (j < segments.size() && segments.get(j)[0] == tick && (int) segments.get(j)[5] == KIND_NOTE) {
                    j++;
                }
                final List<long[]> group = segments.subList(i, j);
                i = j;

                if (group.size() == 1) {
                    final long[] seg = group.get(0);
                    final Fraction dur = ticksToFraction(seg[1] - tick, resolution);
                    final SpelledPitch pitch = midiToSpelledPitch((int) seg[2]);
                    final Velocity vel = Velocity.of(Math.clamp((int) seg[3], 1, 127));
                    events.add(Note.of(pitch, new FractionDuration(dur), vel,
                        build.music.core.Articulation.NORMAL, false));
                    cursor = seg[1];
                } else {
                    final List<Pitch> pitches = new ArrayList<>();
                    long maxEnd = tick;
                    int velSum = 0;
                    for (final long[] seg : group) {
                        pitches.add(midiToSpelledPitch((int) seg[2]));
                        if (seg[1] > maxEnd) {
                            maxEnd = seg[1];
                        }
                        velSum += (int) seg[3];
                    }
                    final Fraction dur = ticksToFraction(maxEnd - tick, resolution);
                    final Velocity vel = Velocity.of(Math.clamp(velSum / group.size(), 1, 127));
                    events.add(Chord.of(pitches, new FractionDuration(dur), vel));
                    cursor = maxEnd;
                }
            }
        }

        return Collections.unmodifiableList(events);
    }

    /**
     * Convert ticks to a Fraction of a whole note. Snaps to common rhythmic values.
     */
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
            case 0 -> SpelledPitch.of(NoteName.C, Accidental.NATURAL, octave);
            case 1 -> SpelledPitch.of(NoteName.C, Accidental.SHARP, octave);
            case 2 -> SpelledPitch.of(NoteName.D, Accidental.NATURAL, octave);
            case 3 -> SpelledPitch.of(NoteName.D, Accidental.SHARP, octave);
            case 4 -> SpelledPitch.of(NoteName.E, Accidental.NATURAL, octave);
            case 5 -> SpelledPitch.of(NoteName.F, Accidental.NATURAL, octave);
            case 6 -> SpelledPitch.of(NoteName.F, Accidental.SHARP, octave);
            case 7 -> SpelledPitch.of(NoteName.G, Accidental.NATURAL, octave);
            case 8 -> SpelledPitch.of(NoteName.G, Accidental.SHARP, octave);
            case 9 -> SpelledPitch.of(NoteName.A, Accidental.NATURAL, octave);
            case 10 -> SpelledPitch.of(NoteName.A, Accidental.SHARP, octave);
            case 11 -> SpelledPitch.of(NoteName.B, Accidental.NATURAL, octave);
            default -> throw new IllegalArgumentException("Unexpected semitone: " + semitone);
        };
    }
}
