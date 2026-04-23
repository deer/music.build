package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * Number of measures in a {@link Section} as a singular trait.
 */
@Singular
public record MeasureCountTrait(int count) implements Trait {

    @Unmarshal
    public MeasureCountTrait {
        if (count <= 0) {
            throw new IllegalArgumentException("measure count must be positive, got: " + count);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> count) {
        count.set(this.count);
    }

    public static MeasureCountTrait of(final int count) {
        return new MeasureCountTrait(count);
    }

    static {
        Marshalling.register(MeasureCountTrait.class, MethodHandles.lookup());
    }
}
