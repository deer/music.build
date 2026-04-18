package build.music.midi;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.Pitch;
import build.music.score.Part;
import build.music.score.Score;
import build.music.time.Fraction;
import build.music.time.Tempo;
import build.music.time.TempoChange;
import build.music.time.TimeSignature;

import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Converts a Score or note sequence to a javax.sound.midi.Sequence.
 */
public final class MidiRenderer {

    private static final int TICKS_PER_QUARTER = 480;

    private MidiRenderer() {
    }

    /**
     * Render a complete Score to a MIDI Sequence (one track per Part plus conductor track 0).
     */
    public static Sequence render(final Score score) throws InvalidMidiDataException {
        final Sequence seq = new Sequence(Sequence.PPQ, TICKS_PER_QUARTER);

        // Track 0: conductor (tempo + time signature)
        final Track conductor = seq.createTrack();
        addTempo(conductor, 0, score.tempo());
        addTimeSignature(conductor, 0, score.timeSignature());
        // Emit per-bar tempo events for any gradual tempo changes
        final long ticksPerMeasure = fractionToTicks(score.timeSignature().measureDuration());
        for (final TempoChange tc : score.tempoChanges()) {
            final int[] bpms = tc.interpolatedBpms();
            for (int i = 0; i < bpms.length; i++) {
                final long tick = (tc.startBar() - 1 + i) * ticksPerMeasure;
                addTempo(conductor, tick, Tempo.of(bpms[i]));
            }
        }
        addEndOfTrack(conductor, 0);

        // Compute swing delay in ticks if swing is set
        long swingDelay = 0;
        if (score.swingRatio() != null) {
            swingDelay = Math.round((score.swingRatio().toDouble() - 0.5) * TICKS_PER_QUARTER);
        }

        for (final Part part : score.scoreParts()) {
            final Track track = seq.createTrack();
            // Program change at tick 0
            final ShortMessage pc = new ShortMessage();
            pc.setMessage(ShortMessage.PROGRAM_CHANGE, part.midiChannel(), part.midiProgram(), 0);
            track.add(new MidiEvent(pc, 0));

            final List<NoteEvent> events = part.voice().events();
            // Pre-pass: resolve tie chains.
            // tiedSoundingTicks[i] = total sounding ticks for event i (sum of tie chain durations).
            // tiedAbsorbed[i] = true if this event is the continuation of a tie (suppress its NOTE_ON/OFF).
            final long[] tiedSoundingTicks = new long[events.size()];
            final boolean[] tiedAbsorbed = new boolean[events.size()];
            for (int i = 0; i < events.size(); i++) {
                if (tiedAbsorbed[i]) {
                    continue;
                }
                tiedSoundingTicks[i] = fractionToTicks(events.get(i).duration().fraction());
                if (events.get(i) instanceof Note n && n.tied()) {
                    final int midi = n.midi();
                    int j = i + 1;
                    while (j < events.size() && events.get(j) instanceof Note next
                        && next.midi() == midi) {
                        tiedAbsorbed[j] = true;
                        tiedSoundingTicks[i] += fractionToTicks(next.duration().fraction());
                        if (!next.tied()) {
                            break;
                        }
                        j++;
                    }
                }
            }

            long tick = 0;
            for (int i = 0; i < events.size(); i++) {
                final NoteEvent event = events.get(i);
                final long notatedTicks = fractionToTicks(event.duration().fraction());
                final boolean isEighth = event.duration().fraction().equals(Fraction.EIGHTH);

                if (!tiedAbsorbed[i] && event instanceof Note n) {
                    long actualOnTick = tick;
                    final long soundingTicks;
                    if (swingDelay > 0 && isEighth) {
                        final long eighthTicks = TICKS_PER_QUARTER / 2;
                        final boolean isOffBeat = (tick / eighthTicks) % 2 == 1;
                        if (isOffBeat) {
                            actualOnTick = tick + swingDelay;
                            soundingTicks = Math.round((eighthTicks - swingDelay) * n.articulation().durationFactor());
                        } else {
                            soundingTicks = Math.round((eighthTicks + swingDelay) * n.articulation().durationFactor());
                        }
                    } else if (n.tied()) {
                        // Tied note: full chain duration, no articulation shortening
                        soundingTicks = tiedSoundingTicks[i];
                    } else {
                        soundingTicks = (long) (notatedTicks * n.articulation().durationFactor());
                    }
                    final ShortMessage on = new ShortMessage();
                    on.setMessage(ShortMessage.NOTE_ON, part.midiChannel(), n.midi(), n.velocity().value());
                    track.add(new MidiEvent(on, actualOnTick));
                    final ShortMessage off = new ShortMessage();
                    off.setMessage(ShortMessage.NOTE_OFF, part.midiChannel(), n.midi(), 0);
                    track.add(new MidiEvent(off, actualOnTick + soundingTicks));
                } else if (!tiedAbsorbed[i] && event instanceof Chord c) {
                    long chordOnTick = tick;
                    final long chordSoundingTicks;
                    if (swingDelay > 0 && isEighth) {
                        final long eighthTicks = TICKS_PER_QUARTER / 2;
                        final boolean isOffBeat = (tick / eighthTicks) % 2 == 1;
                        if (isOffBeat) {
                            chordOnTick = tick + swingDelay;
                            chordSoundingTicks = eighthTicks - swingDelay;
                        } else {
                            chordSoundingTicks = eighthTicks + swingDelay;
                        }
                    } else {
                        chordSoundingTicks = notatedTicks;
                    }
                    for (final Pitch p : c.pitches()) {
                        final ShortMessage on = new ShortMessage();
                        on.setMessage(ShortMessage.NOTE_ON, part.midiChannel(), p.midi(), c.velocity().value());
                        track.add(new MidiEvent(on, chordOnTick));
                        final ShortMessage off = new ShortMessage();
                        off.setMessage(ShortMessage.NOTE_OFF, part.midiChannel(), p.midi(), 0);
                        track.add(new MidiEvent(off, chordOnTick + chordSoundingTicks));
                    }
                }
                // Advance time (including absorbed tie continuations)
                tick += notatedTicks;
            }
            addEndOfTrack(track, tick);
        }

        return seq;
    }

    /**
     * Render a single event list to a MIDI Sequence.
     * Convenience for quick playback of a single melodic line.
     */
    public static Sequence render(
        final List<NoteEvent> events,
        final Tempo tempo,
        final int channel,
        final int program) throws InvalidMidiDataException {

        final Sequence seq = new Sequence(Sequence.PPQ, TICKS_PER_QUARTER);

        final Track conductor = seq.createTrack();
        addTempo(conductor, 0, tempo);
        addEndOfTrack(conductor, 0);

        final Track track = seq.createTrack();
        final ShortMessage pc = new ShortMessage();
        pc.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
        track.add(new MidiEvent(pc, 0));

        long tick = 0;
        for (final NoteEvent event : events) {
            final long notatedTicks = fractionToTicks(event.duration().fraction());
            if (event instanceof Note n) {
                final long soundingTicks = (long) (notatedTicks * n.articulation().durationFactor());
                final ShortMessage on = new ShortMessage();
                on.setMessage(ShortMessage.NOTE_ON, channel, n.midi(), n.velocity().value());
                track.add(new MidiEvent(on, tick));
                final ShortMessage off = new ShortMessage();
                off.setMessage(ShortMessage.NOTE_OFF, channel, n.midi(), 0);
                track.add(new MidiEvent(off, tick + soundingTicks));
            } else if (event instanceof Chord c) {
                for (final Pitch p : c.pitches()) {
                    final ShortMessage on = new ShortMessage();
                    on.setMessage(ShortMessage.NOTE_ON, channel, p.midi(), c.velocity().value());
                    track.add(new MidiEvent(on, tick));
                    final ShortMessage off = new ShortMessage();
                    off.setMessage(ShortMessage.NOTE_OFF, channel, p.midi(), 0);
                    track.add(new MidiEvent(off, tick + notatedTicks));
                }
            }
            tick += notatedTicks;
        }
        addEndOfTrack(track, tick);

        return seq;
    }

    /**
     * fraction of a whole note → MIDI ticks. Quarter = 1/4 → 480 ticks.
     */
    static long fractionToTicks(final Fraction f) {
        return Math.round(f.toDouble() * 4.0 * TICKS_PER_QUARTER);
    }

    private static void addTempo(final Track track, final long tick, final Tempo tempo) throws InvalidMidiDataException {
        final int microsPerBeat = 60_000_000 / tempo.bpm();
        final byte[] data = {
            (byte) ((microsPerBeat >> 16) & 0xFF),
            (byte) ((microsPerBeat >> 8) & 0xFF),
            (byte) (microsPerBeat & 0xFF)
        };
        final MetaMessage msg = new MetaMessage(0x51, data, data.length);
        track.add(new MidiEvent(msg, tick));
    }

    private static void addTimeSignature(final Track track, final long tick, final TimeSignature ts) throws InvalidMidiDataException {
        final int log2denom = Integer.numberOfTrailingZeros(ts.beatUnit());
        final byte[] data = {
            (byte) ts.beats(),
            (byte) log2denom,
            (byte) 24,  // MIDI clocks per quarter note
            (byte) 8    // 32nd notes per MIDI quarter note
        };
        final MetaMessage msg = new MetaMessage(0x58, data, data.length);
        track.add(new MidiEvent(msg, tick));
    }

    private static void addEndOfTrack(final Track track, final long tick) throws InvalidMidiDataException {
        final MetaMessage eot = new MetaMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, tick));
    }
}
