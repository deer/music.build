package build.music.core;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A chord symbol (root + quality) — both a {@link Trait} and a {@link AbstractTraitable}.
 */
public final class ChordSymbol
    extends AbstractTraitable
    implements Trait {

    private ChordSymbol(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public ChordSymbol(@Bound final MusicCodeModel codeModel,
                       final Marshaller marshaller,
                       final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static ChordSymbol of(final NoteName root, final Accidental rootAccidental, final ChordQuality quality) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(rootAccidental, "rootAccidental must not be null");
        Objects.requireNonNull(quality, "quality must not be null");
        final ChordSymbol cs = new ChordSymbol(MusicCodeModel.current());
        cs.addTrait(root);
        cs.addTrait(rootAccidental);
        cs.addTrait(quality);
        return cs;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public NoteName root() {
        return getTrait(NoteName.class).orElseThrow();
    }

    public Accidental rootAccidental() {
        return getTrait(Accidental.class).orElseThrow();
    }

    public ChordQuality quality() {
        return getTrait(ChordQuality.class).orElseThrow();
    }

    // ── operations ────────────────────────────────────────────────────────────

    /**
     * Generate pitches for this chord in the given octave.
     */
    public List<SpelledPitch> toPitches(final int octave) {
        final SpelledPitch rootPitch = SpelledPitch.of(root(), rootAccidental(), octave);
        final List<SpelledPitch> pitches = new ArrayList<>();
        for (final SpelledInterval interval : quality().intervals()) {
            pitches.add(rootPitch.transpose(interval));
        }
        return List.copyOf(pitches);
    }

    public ChordSymbol transpose(final SpelledInterval interval) {
        final SpelledPitch rootPitch = SpelledPitch.of(root(), rootAccidental(), 4);
        final SpelledPitch transposed = rootPitch.transpose(interval);
        return ChordSymbol.of(transposed.name(), transposed.accidental(), quality());
    }

    // ── Object ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return root().name() + rootAccidental().symbol() + quality().symbol();
    }

    // ── parse ─────────────────────────────────────────────────────────────────

    public static ChordSymbol parse(final String s) {
        Objects.requireNonNull(s, "input must not be null");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty chord symbol");
        }

        int i = 0;
        final NoteName root = switch (Character.toUpperCase(s.charAt(i++))) {
            case 'C' -> NoteName.C;
            case 'D' -> NoteName.D;
            case 'E' -> NoteName.E;
            case 'F' -> NoteName.F;
            case 'G' -> NoteName.G;
            case 'A' -> NoteName.A;
            case 'B' -> NoteName.B;
            default -> throw new IllegalArgumentException("Unknown note name: " + s.charAt(0));
        };

        Accidental acc = Accidental.NATURAL;
        if (i < s.length()) {
            final char c = s.charAt(i);
            if (c == 'b' || c == '\u266D') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == 'b' || s.charAt(i + 1) == '\u266D')) {
                    acc = Accidental.DOUBLE_FLAT;
                    i += 2;
                } else {
                    acc = Accidental.FLAT;
                    i += 1;
                }
            } else if (c == '#' || c == '\u266F') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == '#' || s.charAt(i + 1) == '\u266F')) {
                    acc = Accidental.DOUBLE_SHARP;
                    i += 2;
                } else {
                    acc = Accidental.SHARP;
                    i += 1;
                }
            }
        }

        final String qualityStr = s.substring(i);
        final ChordQuality quality = parseQuality(qualityStr);
        return ChordSymbol.of(root, acc, quality);
    }

    static {
        Marshalling.register(ChordSymbol.class, MethodHandles.lookup());
    }

    private static ChordQuality parseQuality(final String s) {
        return switch (s) {
            case "maj" -> ChordQuality.MAJOR;
            case "" -> ChordQuality.MAJOR;
            case "m" -> ChordQuality.MINOR;
            case "dim" -> ChordQuality.DIMINISHED;
            case "aug" -> ChordQuality.AUGMENTED;
            case "7" -> ChordQuality.DOMINANT_7;
            case "maj7" -> ChordQuality.MAJOR_7;
            case "m7" -> ChordQuality.MINOR_7;
            case "m7b5" -> ChordQuality.HALF_DIMINISHED_7;
            case "dim7" -> ChordQuality.DIMINISHED_7;
            case "mMaj7" -> ChordQuality.MINOR_MAJOR_7;
            case "augMaj7" -> ChordQuality.AUGMENTED_MAJOR_7;
            case "sus2" -> ChordQuality.SUSPENDED_2;
            case "sus4" -> ChordQuality.SUSPENDED_4;
            default -> throw new IllegalArgumentException("Unknown chord quality: '" + s + "'");
        };
    }
}
