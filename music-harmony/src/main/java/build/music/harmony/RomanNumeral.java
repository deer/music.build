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
import build.music.core.ChordQuality;
import build.music.core.ChordSymbol;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A Roman numeral chord label with quality (I, ii, V7, viio, etc.).
 * Upper-case = major chord, lower-case = minor chord.
 */
public final class RomanNumeral
    extends AbstractTraitable {

    private RomanNumeral(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public RomanNumeral(@Bound final MusicCodeModel codeModel,
                        final Marshaller marshaller,
                        final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static RomanNumeral of(final ScaleDegree degree, final ChordQuality quality, final boolean inverted) {
        Objects.requireNonNull(degree, "degree must not be null");
        Objects.requireNonNull(quality, "quality must not be null");
        final RomanNumeral rn = new RomanNumeral(MusicCodeModel.current());
        rn.addTrait(degree);
        rn.addTrait(quality);
        if (inverted) {
            rn.addTrait(InvertedMarker.INSTANCE);
        }
        return rn;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public ScaleDegree degree() {
        return getTrait(ScaleDegree.class).orElseThrow();
    }

    public ChordQuality quality() {
        return getTrait(ChordQuality.class).orElseThrow();
    }

    public boolean inverted() {
        return hasTrait(InvertedMarker.class);
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Build the concrete chord symbol for this Roman numeral in the given key.
     */
    public ChordSymbol chordInKey(final Key key) {
        final SpelledPitch tonicPitch = key.scale().degree(degree().number(), 4);
        return ChordSymbol.of(tonicPitch.name(), tonicPitch.accidental(), quality());
    }

    /**
     * Parse Roman numerals: "I", "ii", "IV", "V7", "viio", "bVII", "iii", "vi", etc.
     */
    public static RomanNumeral parse(final String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        final String s = symbol.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty Roman numeral");
        }

        int start = 0;

        boolean hasFlatPrefix = false;
        if (s.charAt(0) == 'b' && s.length() > 1) {
            final char next = s.charAt(1);
            if (next == 'I' || next == 'V' || next == 'i' || next == 'v') {
                hasFlatPrefix = true;
                start = 1;
            }
        }

        int end = start;
        while (end < s.length() && "IViv".indexOf(s.charAt(end)) >= 0) {
            end++;
        }

        if (end == start) {
            throw new IllegalArgumentException("Cannot parse Roman numeral: '" + symbol + "'");
        }

        final String numStr = s.substring(start, end);
        final String suffix = s.substring(end);

        final boolean isUpper = Character.isUpperCase(numStr.charAt(0));
        final String upperNum = numStr.toUpperCase();

        final ScaleDegree degree = switch (upperNum) {
            case "I" -> ScaleDegree.I;
            case "II" -> ScaleDegree.II;
            case "III" -> ScaleDegree.III;
            case "IV" -> ScaleDegree.IV;
            case "V" -> ScaleDegree.V;
            case "VI" -> ScaleDegree.VI;
            case "VII" -> ScaleDegree.VII;
            default -> throw new IllegalArgumentException("Cannot parse Roman numeral: '" + numStr + "'");
        };

        final ChordQuality quality;
        if (isUpper) {
            quality = switch (suffix) {
                case "7" -> ChordQuality.DOMINANT_7;
                case "maj7" -> ChordQuality.MAJOR_7;
                case "aug" -> ChordQuality.AUGMENTED;
                default -> ChordQuality.MAJOR;
            };
        } else {
            quality = switch (suffix) {
                case "o", "dim" -> ChordQuality.DIMINISHED;
                case "7" -> ChordQuality.MINOR_7;
                case "o7" -> ChordQuality.DIMINISHED_7;
                case "m7b5" -> ChordQuality.HALF_DIMINISHED_7;
                default -> ChordQuality.MINOR;
            };
        }

        return RomanNumeral.of(degree, quality, false);
    }

    /**
     * Standard diatonic triads for major keys: I ii iii IV V vi viio
     */
    public static List<RomanNumeral> diatonicMajor() {
        return List.of(
            RomanNumeral.of(ScaleDegree.I, ChordQuality.MAJOR, false),
            RomanNumeral.of(ScaleDegree.II, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.III, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.IV, ChordQuality.MAJOR, false),
            RomanNumeral.of(ScaleDegree.V, ChordQuality.MAJOR, false),
            RomanNumeral.of(ScaleDegree.VI, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.VII, ChordQuality.DIMINISHED, false)
        );
    }

    /**
     * Standard diatonic triads for natural minor keys: i iio III iv v VI VII
     */
    public static List<RomanNumeral> diatonicMinor() {
        return List.of(
            RomanNumeral.of(ScaleDegree.I, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.II, ChordQuality.DIMINISHED, false),
            RomanNumeral.of(ScaleDegree.III, ChordQuality.MAJOR, false),
            RomanNumeral.of(ScaleDegree.IV, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.V, ChordQuality.MINOR, false),
            RomanNumeral.of(ScaleDegree.VI, ChordQuality.MAJOR, false),
            RomanNumeral.of(ScaleDegree.VII, ChordQuality.MAJOR, false)
        );
    }

    static {
        Marshalling.register(RomanNumeral.class, MethodHandles.lookup());
    }

    @Override
    public String toString() {
        final ScaleDegree degree = degree();
        final ChordQuality quality = quality();

        String base = switch (degree) {
            case I -> "I";
            case II -> "II";
            case III -> "III";
            case IV -> "IV";
            case V -> "V";
            case VI -> "VI";
            case VII -> "VII";
        };

        final boolean isMinorQuality = quality == ChordQuality.MINOR || quality == ChordQuality.MINOR_7
            || quality == ChordQuality.DIMINISHED || quality == ChordQuality.DIMINISHED_7
            || quality == ChordQuality.HALF_DIMINISHED_7;

        if (isMinorQuality) {
            base = base.toLowerCase();
        }

        final String suffix = switch (quality) {
            case DOMINANT_7 -> "7";
            case MAJOR_7 -> "maj7";
            case MINOR_7 -> "7";
            case DIMINISHED -> "o";
            case DIMINISHED_7 -> "o7";
            case HALF_DIMINISHED_7 -> "m7b5";
            case AUGMENTED -> "aug";
            default -> "";
        };

        return base + suffix;
    }
}
