package build.music.instrument;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

@Singular
public enum InstrumentFamily implements Trait {
    WOODWIND, BRASS, STRING, KEYBOARD, PERCUSSION, VOCAL;

    static {
        Marshalling.registerEnum(InstrumentFamily.class);
    }
}
