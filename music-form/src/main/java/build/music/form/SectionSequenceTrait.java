package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ordered list of {@link Section}s that constitute a {@link FormalPlan}.
 * Singular: a formal plan has exactly one section sequence.
 */
@Singular
public record SectionSequenceTrait(List<Section> sections) implements Trait {

    @Unmarshal
    public SectionSequenceTrait(final Marshaller marshaller,
                                final Stream<Marshalled<Section>> sections) {
        this(sections.map(marshaller::unmarshal).toList());
    }

    public SectionSequenceTrait {
        sections = List.copyOf(Objects.requireNonNull(sections, "sections must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Section>>> sections) {
        sections.set(this.sections.stream().map(marshaller::marshal));
    }

    public static SectionSequenceTrait of(final List<Section> sections) {
        return new SectionSequenceTrait(sections);
    }

    static {
        Marshalling.register(SectionSequenceTrait.class, MethodHandles.lookup());
    }
}
