package build.music.instrument;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Presence-based marker trait indicating that an {@link Instrument} is a transposing instrument
 * (e.g. Bb clarinet, F horn). When present, the instrument's written pitch differs from its
 * concert pitch by the transposition interval.
 * Singular: either the instrument transposes or it does not.
 */
@Singular
public enum TransposingMarker implements Trait {
    INSTANCE;

    static {
        Marshalling.registerEnum(TransposingMarker.class);
    }
}
