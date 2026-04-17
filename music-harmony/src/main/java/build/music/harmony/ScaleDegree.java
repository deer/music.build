package build.music.harmony;

import build.base.marshalling.Marshalling;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

@Singular
public enum ScaleDegree implements Trait {
    I(1), II(2), III(3), IV(4), V(5), VI(6), VII(7);

    private final int number;

    ScaleDegree(final int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    static {
        Marshalling.registerEnum(ScaleDegree.class);
    }
}
