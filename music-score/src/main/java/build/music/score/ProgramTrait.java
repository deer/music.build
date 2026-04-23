package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * MIDI program (instrument patch) as a singular trait for {@link Voice} or {@link Part}.
 */
@Singular
public record ProgramTrait(int program) implements Trait {

    @Unmarshal
    public ProgramTrait {
        if (program < 0 || program > 127) {
            throw new IllegalArgumentException("MIDI program must be 0-127, got: " + program);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> program) {
        program.set(this.program);
    }

    public static ProgramTrait of(final int program) {
        return new ProgramTrait(program);
    }

    static {
        Marshalling.register(ProgramTrait.class, MethodHandles.lookup());
    }
}
