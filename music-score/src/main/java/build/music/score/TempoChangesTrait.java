package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.time.TempoChange;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ordered list of tempo change events for a {@link Score}. Singular: a score has at most
 * one tempo-change timeline.
 */
@Singular
public record TempoChangesTrait(List<TempoChange> changes) implements Trait {

    @Unmarshal
    public TempoChangesTrait(final Marshaller marshaller, final Stream<Marshalled<TempoChange>> changes) {
        this(changes.map(marshaller::unmarshal).toList());
    }

    public TempoChangesTrait {
        changes = List.copyOf(Objects.requireNonNull(changes, "changes must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Stream<Marshalled<TempoChange>>> changes) {
        changes.set(this.changes.stream().map(marshaller::marshal));
    }

    public static TempoChangesTrait of(final List<TempoChange> changes) {
        return new TempoChangesTrait(changes);
    }

    static {
        Marshalling.register(TempoChangesTrait.class, MethodHandles.lookup());
    }
}
