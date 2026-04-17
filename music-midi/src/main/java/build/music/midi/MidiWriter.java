package build.music.midi;

import java.io.IOException;
import java.nio.file.Path;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

/** Writes a MIDI Sequence to a Standard MIDI File. */
public final class MidiWriter {

    private MidiWriter() {}

    /**
     * Write a Sequence to a Standard MIDI File (type 1 — multi-track).
     */
    public static void write(final Sequence sequence, final Path path) throws IOException {
        final int written = MidiSystem.write(sequence, 1, path.toFile());
        if (written < 0) {
            throw new IOException("MidiSystem.write returned " + written + " for " + path);
        }
    }
}
