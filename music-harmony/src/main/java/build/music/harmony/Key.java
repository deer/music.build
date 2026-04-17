package build.music.harmony;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A musical key: tonic + mode (major or minor).
 *
 * <p>Implements {@link Trait} so it can be used as a singular optional trait of {@code Score}.
 */
@Singular
public final class Key
    extends AbstractTraitable
    implements Trait {

    private static final List<NoteName> SHARP_ORDER = List.of(
        NoteName.F, NoteName.C, NoteName.G, NoteName.D, NoteName.A, NoteName.E, NoteName.B);
    private static final List<NoteName> FLAT_ORDER = List.of(
        NoteName.B, NoteName.E, NoteName.A, NoteName.D, NoteName.G, NoteName.C, NoteName.F);

    private Key(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Key(@Bound final MusicCodeModel codeModel,
               final Marshaller marshaller,
               final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Key of(final NoteName tonic, final Accidental accidental, final boolean minor) {
        Objects.requireNonNull(tonic, "tonic must not be null");
        Objects.requireNonNull(accidental, "accidental must not be null");
        final Key k = new Key(MusicCodeModel.current());
        k.addTrait(tonic);
        k.addTrait(accidental);
        k.addTrait(minor ? ModeTrait.MINOR : ModeTrait.MAJOR);
        return k;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public NoteName tonic() {
        return getTrait(NoteName.class).orElseThrow();
    }

    public Accidental accidental() {
        return getTrait(Accidental.class).orElseThrow();
    }

    public boolean minor() {
        return getTrait(ModeTrait.class).orElseThrow() == ModeTrait.MINOR;
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * The scale for this key.
     */
    public Scale scale() {
        final ScaleType type = minor() ? ScaleType.NATURAL_MINOR : ScaleType.MAJOR;
        return Scale.of(tonic(), accidental(), type);
    }

    /**
     * The relative major/minor key (same key signature, different tonic).
     */
    public Key relative() {
        if (!minor()) {
            final SpelledPitch sixth = scale().degree(6, 4);
            return Key.minor(sixth.name(), sixth.accidental());
        } else {
            final SpelledPitch third = scale().degree(3, 4);
            return Key.major(third.name(), third.accidental());
        }
    }

    /**
     * The parallel major/minor key (same tonic, different mode).
     */
    public Key parallel() {
        return Key.of(tonic(), accidental(), !minor());
    }

    /**
     * The dominant key (built on the 5th degree).
     */
    public Key dominant() {
        final SpelledPitch fifth = scale().degree(5, 4);
        return Key.major(fifth.name(), fifth.accidental());
    }

    /**
     * The subdominant key (built on the 4th degree).
     */
    public Key subdominant() {
        final SpelledPitch fourth = scale().degree(4, 4);
        return Key.major(fourth.name(), fourth.accidental());
    }

    /**
     * Number of sharps (positive) or flats (negative) in the key signature.
     */
    public int signatureAccidentals() {
        if (minor()) {
            return relative().signatureAccidentals();
        }
        final NoteName tonic = tonic();
        final Accidental accidental = accidental();
        final int semitone = (tonic.semitonesAboveC() + accidental.semitoneOffset() + 12) % 12;
        return switch (semitone) {
            case 0 -> 0;
            case 7 -> 1;
            case 2 -> 2;
            case 9 -> 3;
            case 4 -> 4;
            case 11 -> accidental == Accidental.FLAT ? -7 : 5;
            case 6 -> accidental == Accidental.SHARP ? 6 : -6;
            case 1 -> accidental == Accidental.SHARP ? 7 : -5;
            case 5 -> -1;
            case 10 -> -2;
            case 3 -> -3;
            case 8 -> -4;
            default -> throw new IllegalStateException(
                "Cannot determine key signature for: " + tonic + accidental.symbol());
        };
    }

    /**
     * Which note names have accidentals in this key signature.
     */
    public List<NoteName> accidentalNotes() {
        final int count = signatureAccidentals();
        if (count > 0) {
            return SHARP_ORDER.subList(0, count);
        }
        if (count < 0) {
            return FLAT_ORDER.subList(0, -count);
        }
        return List.of();
    }

    /**
     * Parse "C major", "A minor", "F# minor", "Bb major".
     */
    public static Key parse(final String description) {
        Objects.requireNonNull(description, "description must not be null");
        final String[] parts = description.trim().split("\\s+", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Cannot parse key: '" + description + "'. Expected 'Tonic Mode'.");
        }

        final String tonicStr = parts[0];
        final NoteName name = parseNoteName(tonicStr.charAt(0));
        Accidental acc = Accidental.NATURAL;
        if (tonicStr.length() > 1) {
            acc = switch (tonicStr.substring(1)) {
                case "#" -> Accidental.SHARP;
                case "b" -> Accidental.FLAT;
                case "##" -> Accidental.DOUBLE_SHARP;
                case "bb" -> Accidental.DOUBLE_FLAT;
                default -> throw new IllegalArgumentException("Unknown accidental: " + tonicStr.substring(1));
            };
        }

        final boolean isMinor = switch (parts[1].toLowerCase()) {
            case "minor", "min", "m" -> true;
            case "major", "maj", "M" -> false;
            default -> throw new IllegalArgumentException("Unknown mode: '" + parts[1] + "'");
        };

        return Key.of(name, acc, isMinor);
    }

    public static Key major(final NoteName tonic) {
        return Key.of(tonic, Accidental.NATURAL, false);
    }

    public static Key major(final NoteName tonic, final Accidental accidental) {
        return Key.of(tonic, accidental, false);
    }

    public static Key minor(final NoteName tonic) {
        return Key.of(tonic, Accidental.NATURAL, true);
    }

    public static Key minor(final NoteName tonic, final Accidental accidental) {
        return Key.of(tonic, accidental, true);
    }

    private static NoteName parseNoteName(final char c) {
        return switch (Character.toUpperCase(c)) {
            case 'C' -> NoteName.C;
            case 'D' -> NoteName.D;
            case 'E' -> NoteName.E;
            case 'F' -> NoteName.F;
            case 'G' -> NoteName.G;
            case 'A' -> NoteName.A;
            case 'B' -> NoteName.B;
            default -> throw new IllegalArgumentException("Unknown note name: " + c);
        };
    }

    @Override
    public String toString() {
        return tonic().name() + accidental().symbol() + " " + (minor() ? "minor" : "major");
    }

    static {
        Marshalling.register(Key.class, MethodHandles.lookup());
    }
}
