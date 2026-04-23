package build.music.instrument;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Display name of an {@link Instrument} as a singular trait.
 */
@Singular
public record InstrumentNameTrait(String name) implements Trait {

    @Unmarshal
    public InstrumentNameTrait {
        Objects.requireNonNull(name, "name must not be null");
    }

    @Marshal
    public void destructor(final Out<String> name) {
        name.set(this.name);
    }

    public static InstrumentNameTrait of(final String name) {
        return new InstrumentNameTrait(name);
    }

    static {
        Marshalling.register(InstrumentNameTrait.class, MethodHandles.lookup());
    }
}
