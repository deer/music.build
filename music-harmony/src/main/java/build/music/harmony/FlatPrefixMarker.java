package build.music.harmony;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Presence-based marker trait indicating that a {@link RomanNumeral} has a flat prefix (e.g. bVII).
 * Singular: either the degree is flatted or it is not.
 */
@Singular
public enum FlatPrefixMarker implements Trait {
    INSTANCE;

    static {
        Marshalling.registerEnum(FlatPrefixMarker.class);
    }
}
