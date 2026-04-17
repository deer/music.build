package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.core.NoteEvent;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Carries the ordered event list that constitutes a {@link Voice}.
 * The events are the voice content — a single sequence trait rather than N non-singular
 * event traits, because the sequence itself is the voice's identity.
 * Singular: a voice has exactly one event sequence.
 */
@Singular
public record EventSequenceTrait(List<NoteEvent> events) implements Trait {

    @Unmarshal
    public EventSequenceTrait(final Marshaller marshaller, final Stream<Marshalled<NoteEvent>> events) {
        this(events.map(marshaller::unmarshal).toList());
    }

    public EventSequenceTrait {
        events = List.copyOf(Objects.requireNonNull(events, "events must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Stream<Marshalled<NoteEvent>>> events) {
        events.set(this.events.stream().map(marshaller::marshal));
    }

    public static EventSequenceTrait of(final List<NoteEvent> events) {
        return new EventSequenceTrait(events);
    }

    static {
        Marshalling.register(EventSequenceTrait.class, MethodHandles.lookup());
    }
}
