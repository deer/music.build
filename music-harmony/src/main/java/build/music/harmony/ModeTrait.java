package build.music.harmony;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Mode of a key or scale — MAJOR or MINOR. Singular: a key has exactly one mode.
 *
 * <p>Replaces the ad-hoc {@code boolean minor} field in {@link Key} when Key is converted
 * to AbstractTraitable in Phase 4.
 */
@Singular
public enum ModeTrait implements Trait {
    MAJOR,
    MINOR;

    static {
        Marshalling.registerEnum(ModeTrait.class);
    }
}
