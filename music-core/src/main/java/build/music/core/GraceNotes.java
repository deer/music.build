package build.music.core;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.SpelledPitch;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Grace-note pitches preceding a {@link Note}. Singular: a note carries at most one group.
 */
@Singular
public record GraceNotes(List<SpelledPitch> pitches) implements Trait {

    @Unmarshal
    public GraceNotes(final Marshaller marshaller, final Stream<Marshalled<SpelledPitch>> pitches) {
        this(pitches.map(marshaller::unmarshal).toList());
    }

    public GraceNotes {
        pitches = List.copyOf(Objects.requireNonNull(pitches));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<SpelledPitch>>> pitches) {
        pitches.set(this.pitches.stream().map(marshaller::marshal));
    }

    static {
        Marshalling.register(GraceNotes.class, MethodHandles.lookup());
    }
}
