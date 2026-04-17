package build.music.pitch;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;


/**
 * Direction of a spelled interval — promoted from the ad-hoc ascending/descending boolean
 * used in build.music.transform.Transpose. Singular: an interval has one direction.
 */
@Singular
public enum IntervalDirection implements Trait {
    ASCENDING,
    DESCENDING;

    static {
        Marshalling.registerEnum(IntervalDirection.class);
    }
}
