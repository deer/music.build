package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * Named section boundary at a 1-based bar position in the MIDI timeline.
 */
public record SectionMarker(String name, int startBar) {

    @Unmarshal
    public SectionMarker {
    }

    @Marshal
    public void destructor(final Out<String> name, final Out<Integer> startBar) {
        name.set(this.name);
        startBar.set(this.startBar);
    }

    static {
        Marshalling.register(SectionMarker.class, MethodHandles.lookup());
    }
}
