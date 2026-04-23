package build.music.midi;

import java.util.concurrent.CountDownLatch;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

/**
 * Plays a MIDI Sequence through the system synthesizer.
 */
public final class MidiPlayer implements AutoCloseable {

    private final Sequencer sequencer;

    public MidiPlayer() throws MidiUnavailableException {
        sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        // Connect sequencer to default synthesizer if not already connected
        try {
            final Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());
        } catch (final MidiUnavailableException e) {
            // Headless environment — sequencer alone won't produce sound, but won't crash
        }
    }

    /**
     * Play the sequence, blocking until complete.
     */
    public void play(final Sequence sequence) throws InvalidMidiDataException {
        final CountDownLatch latch = new CountDownLatch(1);
        sequencer.addMetaEventListener(msg -> {
            if (msg.getType() == 0x2F) { // end of track
                latch.countDown();
            }
        });
        sequencer.setSequence(sequence);
        sequencer.setTickPosition(0);
        sequencer.start();
        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            sequencer.stop();
        }
    }

    /**
     * Play without blocking.
     */
    public void playAsync(final Sequence sequence) throws InvalidMidiDataException {
        sequencer.setSequence(sequence);
        sequencer.setTickPosition(0);
        sequencer.start();
    }

    /**
     * Stop current playback.
     */
    public void stop() {
        sequencer.stop();
    }

    /**
     * Whether playback is currently running.
     */
    public boolean isPlaying() {
        return sequencer.isRunning();
    }

    @Override
    public void close() {
        sequencer.stop();
        sequencer.close();
    }
}
