package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Name of a {@link Part} as a singular trait.
 */
@Singular
public record PartNameTrait(String name) implements Trait {

    @Unmarshal
    public PartNameTrait {
        Objects.requireNonNull(name, "name must not be null");
    }

    @Marshal
    public void destructor(final Out<String> name) {
        name.set(this.name);
    }

    public static PartNameTrait of(final String name) {
        return new PartNameTrait(name);
    }

    static {
        Marshalling.register(PartNameTrait.class, MethodHandles.lookup());
    }
}
