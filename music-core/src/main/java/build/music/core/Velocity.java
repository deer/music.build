package build.music.core;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * Velocity as a singular trait — a note event carries at most one velocity.
 */
@Singular
public record Velocity(int value) implements Trait {
    public static final Velocity PPP = new Velocity(16);
    public static final Velocity PP = new Velocity(33);
    public static final Velocity P = new Velocity(49);
    public static final Velocity MP = new Velocity(64);
    public static final Velocity MF = new Velocity(80);
    public static final Velocity F = new Velocity(96);
    public static final Velocity FF = new Velocity(112);
    public static final Velocity FFF = new Velocity(127);

    @Unmarshal
    public Velocity {
        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("Velocity must be 0-127, got: " + value);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> value) {
        value.set(this.value);
    }

    public static Velocity of(final int value) {
        return new Velocity(value);
    }

    static {
        Marshalling.register(Velocity.class, MethodHandles.lookup());
    }
}
