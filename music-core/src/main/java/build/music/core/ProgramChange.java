package build.music.core;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.time.Duration;
import build.music.time.ZeroDuration;

import java.lang.invoke.MethodHandles;

/**
 * A mid-score MIDI Program Change event. Has zero duration; does not advance the tick cursor.
 * Allows instrument changes within a voice (e.g. arco to pizzicato, patch changes in synth lines).
 *
 * <p>Program numbers are 0-indexed GM program numbers (0=Acoustic Grand Piano, 40=Violin, etc).
 */
public record ProgramChange(int program) implements NoteEvent {

    @Unmarshal
    public ProgramChange {
        if (program < 0 || program > 127) {
            throw new IllegalArgumentException("Program must be 0-127, got: " + program);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> program) {
        program.set(this.program);
    }

    @Override
    public Duration duration() {
        return ZeroDuration.INSTANCE;
    }

    @Override
    public String toString() {
        return "pc:" + program;
    }

    static {
        Marshalling.register(ProgramChange.class, MethodHandles.lookup());
    }
}
