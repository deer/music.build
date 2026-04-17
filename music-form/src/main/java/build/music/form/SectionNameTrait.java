package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Structural name of a {@link Section} (used as map key in parent structures). Singular.
 */
@Singular
public record SectionNameTrait(String name) implements Trait {

    @Unmarshal
    public SectionNameTrait {
        Objects.requireNonNull(name, "name must not be null");
    }

    @Marshal
    public void destructor(final Out<String> name) {
        name.set(this.name);
    }

    public static SectionNameTrait of(final String name) {
        return new SectionNameTrait(name);
    }

    static {
        Marshalling.register(SectionNameTrait.class, MethodHandles.lookup());
    }
}
