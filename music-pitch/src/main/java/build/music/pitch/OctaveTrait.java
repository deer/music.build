package build.music.pitch;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * Carries the octave number of a pitched entity as a singular trait.
 * Decomposed from {@link SpelledPitch} so queries like "all notes above octave 4" are cheap.
 */
@Singular
public record OctaveTrait(int octave) implements Trait {

    @Unmarshal
    public OctaveTrait {
    }

    @Marshal
    public void destructor(final Out<Integer> octave) {
        octave.set(this.octave);
    }

    public static OctaveTrait of(final int octave) {
        return new OctaveTrait(octave);
    }

    static {
        Marshalling.register(OctaveTrait.class, MethodHandles.lookup());
    }
}
