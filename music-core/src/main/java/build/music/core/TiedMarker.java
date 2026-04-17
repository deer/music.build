package build.music.core;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

/**
 * Presence-based marker trait indicating that a {@link Note} is tied to the next event.
 * Singular: a note is either tied or it isn't.
 */
@Singular
public enum TiedMarker implements Trait {
    INSTANCE;

    static {
        Marshalling.registerEnum(TiedMarker.class);
    }
}
