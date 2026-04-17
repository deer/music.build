package build.music.time;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * Time signature as a singular trait — a score or section carries at most one time signature.
 */
@Singular
public record TimeSignature(int beats, int beatUnit) implements Trait {

    public static final TimeSignature COMMON_TIME = new TimeSignature(4, 4);
    public static final TimeSignature CUT_TIME = new TimeSignature(2, 2);
    public static final TimeSignature WALTZ_TIME = new TimeSignature(3, 4);

    @Unmarshal
    public TimeSignature {
        if (beats <= 0) {
            throw new IllegalArgumentException("beats must be positive, got: " + beats);
        }
        if (beatUnit <= 0 || Integer.bitCount(beatUnit) != 1) {
            throw new IllegalArgumentException("beatUnit must be a power of 2, got: " + beatUnit);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> beats, final Out<Integer> beatUnit) {
        beats.set(this.beats);
        beatUnit.set(this.beatUnit);
    }

    /**
     * Total duration of one measure as a fraction of a whole note.
     */
    public Fraction measureDuration() {
        return Fraction.of(beats, beatUnit);
    }

    /**
     * True for 6/8, 9/8, 12/8 etc — beats divisible by 3 with 8th-note beat unit.
     */
    public boolean isCompound() {
        return beats % 3 == 0 && beatUnit == 8;
    }

    /**
     * True if not compound.
     */
    public boolean isSimple() {
        return !isCompound();
    }

    @Override
    public String toString() {
        return beats + "/" + beatUnit;
    }

    static {
        Marshalling.register(TimeSignature.class, MethodHandles.lookup());
    }
}
