package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.time.Fraction;

import java.lang.invoke.MethodHandles;

/**
 * Swing ratio as a singular trait on {@link Score}. A ratio of {@code 2/3} means the first
 * eighth note of each beat is held for 2/3 and the second for 1/3 of the beat, producing a
 * standard swing feel. Stored as a {@link Fraction} to keep it exact.
 */
@Singular
public record SwingRatioTrait(Fraction ratio) implements Trait {

    @Unmarshal
    public SwingRatioTrait(final Marshaller marshaller, final Marshalled<Fraction> ratio) {
        this(marshaller.unmarshal(ratio));
    }

    public SwingRatioTrait {
        if (ratio == null) {
            throw new IllegalArgumentException("ratio must not be null");
        }
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Marshalled<Fraction>> ratio) {
        ratio.set(marshaller.marshal(this.ratio));
    }

    public static SwingRatioTrait of(final Fraction ratio) {
        return new SwingRatioTrait(ratio);
    }

    static {
        Marshalling.register(SwingRatioTrait.class, MethodHandles.lookup());
    }
}
