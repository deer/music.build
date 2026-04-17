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
 * Title of a {@link Score} as a singular trait.
 */
@Singular
public record ScoreTitleTrait(String title) implements Trait {

    @Unmarshal
    public ScoreTitleTrait {
        Objects.requireNonNull(title, "title must not be null");
    }

    @Marshal
    public void destructor(final Out<String> title) {
        title.set(this.title);
    }

    public static ScoreTitleTrait of(final String title) {
        return new ScoreTitleTrait(title);
    }

    static {
        Marshalling.register(ScoreTitleTrait.class, MethodHandles.lookup());
    }
}
