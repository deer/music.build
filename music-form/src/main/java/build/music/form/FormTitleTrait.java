package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/** Title of a {@link FormalPlan} as a singular trait. */
@Singular
public record FormTitleTrait(String title) implements Trait {

    @Unmarshal
    public FormTitleTrait {
        Objects.requireNonNull(title, "title must not be null");
    }

    @Marshal
    public void destructor(final Out<String> title) {
        title.set(this.title);
    }

    public static FormTitleTrait of(final String title) {
        return new FormTitleTrait(title);
    }

    static {
        Marshalling.register(FormTitleTrait.class, MethodHandles.lookup());
    }
}
