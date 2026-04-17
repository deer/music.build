package build.music.mcp;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.core.NoteEvent;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable snapshot of a named motif: a short note sequence stored alongside
 * the score in a {@link CompositionSnapshot}.
 */
public record MotifSnapshot(String name, List<NoteEvent> events) {

    @Unmarshal
    public MotifSnapshot(final Marshaller marshaller,
                         final String name,
                         final Stream<Marshalled<NoteEvent>> events) {
        this(name, events.map(marshaller::unmarshal).toList());
    }

    public MotifSnapshot {
        Objects.requireNonNull(name, "name must not be null");
        events = List.copyOf(Objects.requireNonNull(events, "events must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<String> name,
                           final Out<Stream<Marshalled<NoteEvent>>> events) {
        name.set(this.name);
        events.set(this.events.stream().map(marshaller::marshal));
    }

    static {
        Marshalling.register(MotifSnapshot.class, MethodHandles.lookup());
    }
}
