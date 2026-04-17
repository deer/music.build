package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.NonSingular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Per-pass volta ending for a {@link Section}. Non-singular: a section may carry multiple
 * endings (pass 1 ending, pass 2 ending, etc.). Each {@code EndingTrait} holds a 1-based
 * pass index and the replacement tail section for that pass.
 */
@NonSingular
public record EndingTrait(int pass, Section ending) implements Trait {

    @Unmarshal
    public EndingTrait(final Marshaller marshaller,
                       final int pass,
                       final Marshalled<Section> ending) {
        this(pass, marshaller.unmarshal(ending));
    }

    public EndingTrait {
        if (pass < 1) {
            throw new IllegalArgumentException("pass must be ≥ 1, got: " + pass);
        }
        Objects.requireNonNull(ending, "ending must not be null");
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Integer> pass,
                           final Out<Marshalled<Section>> ending) {
        pass.set(this.pass);
        ending.set(marshaller.marshal(this.ending));
    }

    public static EndingTrait of(final int pass, final Section ending) {
        return new EndingTrait(pass, ending);
    }

    static {
        Marshalling.register(EndingTrait.class, MethodHandles.lookup());
    }
}
