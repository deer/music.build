package build.music.instrument;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * The comfortable (idiomatic, easy-to-play) pitch range of an {@link Instrument} as a singular trait.
 */
@Singular
public record ComfortableRangeTrait(PitchRange range) implements Trait {

    @Unmarshal
    public ComfortableRangeTrait(final Marshaller marshaller, final Marshalled<PitchRange> range) {
        this(marshaller.unmarshal(range));
    }

    public ComfortableRangeTrait {
        Objects.requireNonNull(range, "range must not be null");
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Marshalled<PitchRange>> range) {
        range.set(marshaller.marshal(this.range));
    }

    public static ComfortableRangeTrait of(final PitchRange range) {
        return new ComfortableRangeTrait(range);
    }

    static {
        Marshalling.register(ComfortableRangeTrait.class, MethodHandles.lookup());
    }
}
