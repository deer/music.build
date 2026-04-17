package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.core.NoteEvent;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wrapper record allowing a {@code List<List<NoteEvent>>} to be serialized by the marshalling
 * framework, which cannot handle nested generic collections directly.
 * Used by {@link StructuredVoice.Segment.Volta} to serialize its endings list.
 */
public record NoteEventList(List<NoteEvent> events) {

    @Unmarshal
    public NoteEventList(final Marshaller marshaller, final Stream<Marshalled<NoteEvent>> events) {
        this(events.map(marshaller::unmarshal).toList());
    }

    public NoteEventList {
        events = Collections.unmodifiableList(List.copyOf(events));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<NoteEvent>>> events) {
        events.set(this.events.stream().map(marshaller::marshal));
    }

    static {
        Marshalling.register(NoteEventList.class, MethodHandles.lookup());
    }
}
