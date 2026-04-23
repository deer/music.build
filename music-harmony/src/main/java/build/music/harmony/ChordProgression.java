package build.music.harmony;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.core.ChordSymbol;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ordered sequence of Roman numerals representing a chord progression.
 */
public final class ChordProgression
    extends AbstractTraitable {

    private ChordProgression(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public ChordProgression(@Bound final MusicCodeModel codeModel,
                            final Marshaller marshaller,
                            final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static ChordProgression of(final List<RomanNumeral> chords) {
        Objects.requireNonNull(chords, "chords must not be null");
        final List<RomanNumeral> immutable = List.copyOf(chords);
        final ChordProgression cp = new ChordProgression(MusicCodeModel.current());
        cp.addTrait(ProgressionSequenceTrait.of(immutable));
        return cp;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public List<RomanNumeral> chords() {
        return getTrait(ProgressionSequenceTrait.class).orElseThrow().numerals();
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Resolve to concrete chord symbols in the given key.
     */
    public List<ChordSymbol> inKey(final Key key) {
        return chords().stream().map(rn -> rn.chordInKey(key)).toList();
    }

    public static ChordProgression I_IV_V_I() {
        return parse("I IV V I");
    }

    public static ChordProgression I_V_vi_IV() {
        return parse("I V vi IV");
    }

    public static ChordProgression ii_V_I() {
        return parse("ii V I");
    }

    public static ChordProgression I_vi_IV_V() {
        return parse("I vi IV V");
    }

    public static ChordProgression i_bVII_bVI_V() {
        return ChordProgression.of(List.of(
            RomanNumeral.parse("i"),
            RomanNumeral.parse("bVII"),
            RomanNumeral.parse("bVI"),
            RomanNumeral.parse("V")
        ));
    }

    /**
     * 12-bar blues: I I I I IV IV I I V IV I V
     */
    public static ChordProgression twelveBarBlues() {
        return parse("I I I I IV IV I I V IV I V");
    }

    /**
     * Parse "I - IV - V - I" or "I IV V I" or "I,IV,V,I".
     */
    public static ChordProgression parse(final String progression) {
        Objects.requireNonNull(progression, "progression must not be null");
        final String[] tokens = progression.trim().split("[\\s,\\-]+");
        final List<RomanNumeral> numerals = Arrays.stream(tokens)
            .filter(t -> !t.isEmpty())
            .map(RomanNumeral::parse)
            .toList();
        if (numerals.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty chord progression");
        }
        return ChordProgression.of(numerals);
    }

    @Override
    public String toString() {
        return chords().stream().map(RomanNumeral::toString).collect(Collectors.joining(" "));
    }

    static {
        Marshalling.register(ChordProgression.class, MethodHandles.lookup());
    }
}
