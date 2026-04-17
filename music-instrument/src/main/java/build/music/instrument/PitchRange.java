package build.music.instrument;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A closed range of playable pitches for an instrument.
 */
public final class PitchRange
    extends AbstractTraitable {

    private PitchRange(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public PitchRange(@Bound final MusicCodeModel codeModel,
                      final Marshaller marshaller,
                      final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static PitchRange of(final SpelledPitch low, final SpelledPitch high) {
        Objects.requireNonNull(low, "low must not be null");
        Objects.requireNonNull(high, "high must not be null");
        if (low.midi() > high.midi()) {
            throw new IllegalArgumentException(
                "low (" + low + ") must not be higher than high (" + high + ")");
        }
        final PitchRange pr = new PitchRange(MusicCodeModel.current());
        pr.addTrait(LowPitchTrait.of(low));
        pr.addTrait(HighPitchTrait.of(high));
        return pr;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public SpelledPitch low() {
        return getTrait(LowPitchTrait.class).orElseThrow().pitch();
    }

    public SpelledPitch high() {
        return getTrait(HighPitchTrait.class).orElseThrow().pitch();
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Is this pitch within the range (inclusive)?
     */
    public boolean contains(final SpelledPitch pitch) {
        final int midi = pitch.midi();
        return midi >= low().midi() && midi <= high().midi();
    }

    /**
     * Is this pitch in a comfortable range?
     * Defined as more than 2 semitones from either extreme.
     */
    public boolean isComfortable(final SpelledPitch pitch) {
        final int midi = pitch.midi();
        return midi > low().midi() + 2 && midi < high().midi() - 2;
    }

    /**
     * How many semitones wide is this range?
     */
    public int semitoneSpan() {
        return high().midi() - low().midi();
    }

    @Override
    public String toString() {
        return low() + "-" + high();
    }

    static {
        Marshalling.register(PitchRange.class, MethodHandles.lookup());
    }
}
