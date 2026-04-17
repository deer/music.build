package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * Adapter record serializing a single {@code Map.Entry<Integer, Section>} from
 * {@link Section}'s endings map. The framework cannot marshal Java Maps directly.
 */
public record SectionEndingPair(int pass, Section section) implements Trait {

    @Unmarshal
    public SectionEndingPair(final Marshaller marshaller,
                              final int pass,
                              final Marshalled<Section> section) {
        this(pass, marshaller.unmarshal(section));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Integer> pass,
                           final Out<Marshalled<Section>> section) {
        pass.set(this.pass);
        section.set(marshaller.marshal(this.section));
    }

    static {
        Marshalling.register(SectionEndingPair.class, MethodHandles.lookup());
    }
}
