package build.music.core;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.time.Duration;
import build.music.time.ZeroDuration;

import java.lang.invoke.MethodHandles;

/**
 * A MIDI Control Change event — a point-in-time message that sets a CC controller value.
 * Has zero duration; does not advance the tick cursor in the renderer.
 *
 * <p>Common CC numbers: 1=modulation, 7=volume, 10=pan, 11=expression,
 * 64=sustain, 91=reverb, 93=chorus.
 */
public record ControlChange(int cc, int value) implements NoteEvent {

    @Unmarshal
    public ControlChange {
        if (cc < 0 || cc > 127) {
            throw new IllegalArgumentException("CC number must be 0-127, got: " + cc);
        }
        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("CC value must be 0-127, got: " + value);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> cc, final Out<Integer> value) {
        cc.set(this.cc);
        value.set(this.value);
    }

    @Override
    public Duration duration() {
        return ZeroDuration.INSTANCE;
    }

    @Override
    public String toString() {
        return "cc:" + cc + ":" + value;
    }

    static {
        Marshalling.register(ControlChange.class, MethodHandles.lookup());
    }
}
