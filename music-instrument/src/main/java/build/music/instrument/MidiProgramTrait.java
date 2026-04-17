package build.music.instrument;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/** General MIDI program number for an {@link Instrument} as a singular trait (0-127). */
@Singular
public record MidiProgramTrait(int program) implements Trait {

    @Unmarshal
    public MidiProgramTrait {
        if (program < 0 || program > 127) {
            throw new IllegalArgumentException("MIDI program must be 0-127, got: " + program);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> program) {
        program.set(this.program);
    }

    public static MidiProgramTrait of(final int program) {
        return new MidiProgramTrait(program);
    }

    static {
        Marshalling.register(MidiProgramTrait.class, MethodHandles.lookup());
    }
}
