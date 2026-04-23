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
 * Name of an {@link Ensemble} as a singular trait.
 */
@Singular
public record EnsembleNameTrait(String name) implements Trait {

    @Unmarshal
    public EnsembleNameTrait {
        Objects.requireNonNull(name, "name must not be null");
    }

    @Marshal
    public void destructor(final Out<String> name) {
        name.set(this.name);
    }

    public static EnsembleNameTrait of(final String name) {
        return new EnsembleNameTrait(name);
    }

    static {
        Marshalling.register(EnsembleNameTrait.class, MethodHandles.lookup());
    }
}
