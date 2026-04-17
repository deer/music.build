package build.music.instrument;

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
import java.util.Objects;

/**
 * Upper bound of a {@link PitchRange} as a singular trait.
 * Decomposed so range queries like "instruments playable below C7" are cheap.
 */
@Singular
public record HighPitchTrait(SpelledPitch pitch) implements Trait {

    @Unmarshal
    public HighPitchTrait(final Marshaller marshaller, final Marshalled<SpelledPitch> pitch) {
        this(marshaller.unmarshal(pitch));
    }

    public HighPitchTrait {
        Objects.requireNonNull(pitch, "pitch must not be null");
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Marshalled<SpelledPitch>> pitch) {
        pitch.set(marshaller.marshal(this.pitch));
    }

    public static HighPitchTrait of(final SpelledPitch pitch) {
        return new HighPitchTrait(pitch);
    }

    static {
        Marshalling.register(HighPitchTrait.class, MethodHandles.lookup());
    }
}
