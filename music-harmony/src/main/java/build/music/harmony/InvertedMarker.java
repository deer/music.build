package build.music.harmony;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Presence-based marker trait indicating that a {@link RomanNumeral} chord is inverted.
 * Singular: either the chord is inverted or it is not.
 */
@Singular
public enum InvertedMarker implements Trait {
    INSTANCE;

    static {
        Marshalling.registerEnum(InvertedMarker.class);
    }
}
