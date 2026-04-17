package build.music.pitch;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A spelled interval between two pitches, carrying {@link IntervalQuality} and {@link IntervalSize} as traits.
 */
public final class SpelledInterval
    extends AbstractTraitable
    implements Interval, Trait {

    private SpelledInterval(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public SpelledInterval(@Bound final MusicCodeModel codeModel,
                           final Marshaller marshaller,
                           final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    /**
     * Creates a {@link SpelledInterval} and registers {@link IntervalQuality} and {@link IntervalSize}
     * into the ambient {@link MusicCodeModel}. Direction defaults to {@link IntervalDirection#ASCENDING}.
     */
    public static SpelledInterval of(final IntervalQuality quality, final IntervalSize size) {
        Objects.requireNonNull(quality, "quality must not be null");
        Objects.requireNonNull(size, "size must not be null");
        if (!quality.isValidFor(size)) {
            throw new IllegalArgumentException(
                "Quality " + quality + " is not valid for interval size " + size);
        }
        final SpelledInterval i = new SpelledInterval(MusicCodeModel.current());
        i.addTrait(quality);
        i.addTrait(size);
        i.addTrait(IntervalDirection.ASCENDING);
        return i;
    }

    /**
     * Creates a {@link SpelledInterval} with an explicit {@link IntervalDirection}.
     */
    public static SpelledInterval of(final IntervalQuality quality, final IntervalSize size, final IntervalDirection direction) {
        Objects.requireNonNull(quality, "quality must not be null");
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        if (!quality.isValidFor(size)) {
            throw new IllegalArgumentException(
                "Quality " + quality + " is not valid for interval size " + size);
        }
        final SpelledInterval i = new SpelledInterval(MusicCodeModel.current());
        i.addTrait(quality);
        i.addTrait(size);
        i.addTrait(direction);
        return i;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public IntervalQuality quality() {
        return getTrait(IntervalQuality.class).orElseThrow();
    }

    public IntervalSize size() {
        return getTrait(IntervalSize.class).orElseThrow();
    }

    // ── Interval ─────────────────────────────────────────────────────────────

    @Override
    public int semitones() {
        final int base = size().baseSemitones();
        final int adjustment = switch (quality()) {
            case PERFECT -> 0;
            case MAJOR -> 0;
            case MINOR -> -1;
            case AUGMENTED -> 1;
            case DIMINISHED -> size().isPerfectable() ? -1 : -2;
        };
        return base + adjustment;
    }

    @Override
    public SpelledInterval spelled() {
        return this;
    }

    public SpelledInterval invert() {
        return SpelledInterval.of(quality().invert(), size().invert());
    }

    public boolean isCompound() {
        return semitones() > 12;
    }

    public SpelledInterval simple() {
        if (!isCompound()) {
            return this;
        }
        return this;
    }

    // ── Object ───────────────────────────────────────────────────────────────

    /**
     * Value equality: quality + size. Two {@link SpelledInterval} objects with the same
     * quality and size are equal regardless of which {@link MusicCodeModel} instance created them.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpelledInterval other)) {
            return false;
        }
        return quality() == other.quality() && size() == other.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(quality(), size());
    }

    @Override
    public String toString() {
        return quality().symbol() + size().number();
    }

    // ── parse ─────────────────────────────────────────────────────────────────

    public static SpelledInterval parse(final String s) {
        Objects.requireNonNull(s, "input must not be null");
        if (s.length() < 2) {
            throw new IllegalArgumentException("Cannot parse interval: " + s);
        }

        final IntervalQuality quality = switch (s.charAt(0)) {
            case 'd' -> IntervalQuality.DIMINISHED;
            case 'm' -> IntervalQuality.MINOR;
            case 'P' -> IntervalQuality.PERFECT;
            case 'M' -> IntervalQuality.MAJOR;
            case 'A' -> IntervalQuality.AUGMENTED;
            default -> throw new IllegalArgumentException("Unknown quality symbol: " + s.charAt(0));
        };

        final int sizeNum;
        try {
            sizeNum = Integer.parseInt(s.substring(1));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse interval size from: " + s);
        }

        final IntervalSize size = switch (sizeNum) {
            case 1 -> IntervalSize.UNISON;
            case 2 -> IntervalSize.SECOND;
            case 3 -> IntervalSize.THIRD;
            case 4 -> IntervalSize.FOURTH;
            case 5 -> IntervalSize.FIFTH;
            case 6 -> IntervalSize.SIXTH;
            case 7 -> IntervalSize.SEVENTH;
            case 8 -> IntervalSize.OCTAVE;
            default -> throw new IllegalArgumentException("Unknown interval size number: " + sizeNum);
        };

        return SpelledInterval.of(quality, size);
    }

    static {
        Marshalling.register(SpelledInterval.class, MethodHandles.lookup());
    }
}
