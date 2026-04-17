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
 * The written (notated) pitch range of an {@link Instrument} as a singular trait.
 */
@Singular
public record WrittenRangeTrait(PitchRange range) implements Trait {

    @Unmarshal
    public WrittenRangeTrait(final Marshaller marshaller, final Marshalled<PitchRange> range) {
        this(marshaller.unmarshal(range));
    }

    public WrittenRangeTrait {
        Objects.requireNonNull(range, "range must not be null");
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Marshalled<PitchRange>> range) {
        range.set(marshaller.marshal(this.range));
    }

    public static WrittenRangeTrait of(final PitchRange range) {
        return new WrittenRangeTrait(range);
    }

    static {
        Marshalling.register(WrittenRangeTrait.class, MethodHandles.lookup());
    }
}
