package build.music.core;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Named ornament attached to a {@link Note}. Singular: a note carries at most one.
 */
@Singular
public enum Ornament implements Trait {
    ROLL,    // ABC ~
    TRILL,   // ABC T
    MORDENT, // ABC M (lower mordent)
    PRALL;   // ABC P (upper mordent / pralltriller)

    static {
        Marshalling.registerEnum(Ornament.class);
    }
}
