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
 * Spelled pitch — a {@link Trait} (non-singular: {@code Chord} carries multiple) and a {@link AbstractTraitable}.
 */
public final class SpelledPitch
    extends AbstractTraitable
    implements Pitch, Comparable<SpelledPitch>, Trait {

    private SpelledPitch(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public SpelledPitch(@Bound final MusicCodeModel codeModel,
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
     * Creates a {@link SpelledPitch} and registers {@link NoteName}, {@link Accidental}, and
     * {@link OctaveTrait} into the ambient {@link MusicCodeModel}.
     */
    public static SpelledPitch of(final NoteName name,
                                  final Accidental accidental,
                                  final int octave) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(accidental, "accidental must not be null");
        final int midi = computeMidi(name, accidental, octave);
        if (midi < 0 || midi > 127) {
            throw new IllegalArgumentException(
                "Resulting MIDI number " + midi + " is out of range [0,127] for "
                    + name + accidental.symbol() + octave);
        }
        final var p = new SpelledPitch(MusicCodeModel.current());
        p.addTrait(name);
        p.addTrait(accidental);
        p.addTrait(OctaveTrait.of(octave));
        return p;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public NoteName name() {
        return getTrait(NoteName.class).orElseThrow();
    }

    public Accidental accidental() {
        return getTrait(Accidental.class).orElseThrow();
    }

    public int octave() {
        return getTrait(OctaveTrait.class).orElseThrow().octave();
    }

    // ── Pitch ────────────────────────────────────────────────────────────────

    @Override
    public int midi() {
        return computeMidi(name(), accidental(), octave());
    }

    @Override
    public SpelledPitch spelled() {
        return this;
    }

    @Override
    public double frequency(final Tuning tuning) {
        return tuning.frequency(midi());
    }

    @Override
    public PitchClass pitchClass() {
        return PitchClass.of(midi());
    }

    // ── interval operations ───────────────────────────────────────────────────

    /**
     * Transpose this pitch up by the given interval, preserving spelling.
     * E.g., C4 up by diminished 4th = Fb4, NOT E4.
     */
    public SpelledPitch transpose(final SpelledInterval interval) {
        final var n = name();
        final int oct = octave();
        final var newName = n.steps(interval.size().steps());
        final int octaveIncrement = (n.ordinal() + interval.size().steps()) / 7;
        final int newOctave = oct + octaveIncrement;
        final int targetMidi = midi() + interval.semitones();
        final int newAccidentalOffset = targetMidi - (newOctave + 1) * 12 - newName.semitonesAboveC();
        final var newAccidental = Accidental.fromOffset(newAccidentalOffset);
        return SpelledPitch.of(newName, newAccidental, newOctave);
    }

    /**
     * Compute the spelled interval from this pitch up to another pitch.
     */
    public SpelledInterval intervalTo(final SpelledPitch other) {
        int nameDelta = ((other.name().ordinal() - name().ordinal()) + 7) % 7;
        int semitoneDelta = other.midi() - midi();
        if (semitoneDelta < 0) {
            semitoneDelta += 12;
        }
        if (nameDelta == 0 && semitoneDelta > 0) {
            nameDelta = 7;
        }

        final var size = switch (nameDelta) {
            case 0 -> IntervalSize.UNISON;
            case 1 -> IntervalSize.SECOND;
            case 2 -> IntervalSize.THIRD;
            case 3 -> IntervalSize.FOURTH;
            case 4 -> IntervalSize.FIFTH;
            case 5 -> IntervalSize.SIXTH;
            case 6 -> IntervalSize.SEVENTH;
            case 7 -> IntervalSize.OCTAVE;
            default -> throw new IllegalStateException("Unexpected name delta: " + nameDelta);
        };

        final int baseSemitones = size.baseSemitones();
        final int diff = semitoneDelta - baseSemitones;

        final IntervalQuality quality;
        if (size.isPerfectable()) {
            quality = switch (diff) {
                case -1 -> IntervalQuality.DIMINISHED;
                case 0 -> IntervalQuality.PERFECT;
                case 1 -> IntervalQuality.AUGMENTED;
                default -> throw new IllegalArgumentException(
                    "Cannot determine quality: semitone diff=" + diff + " for " + size);
            };
        } else {
            quality = switch (diff) {
                case -2 -> IntervalQuality.DIMINISHED;
                case -1 -> IntervalQuality.MINOR;
                case 0 -> IntervalQuality.MAJOR;
                case 1 -> IntervalQuality.AUGMENTED;
                default -> throw new IllegalArgumentException(
                    "Cannot determine quality: semitone diff=" + diff + " for " + size);
            };
        }

        return SpelledInterval.of(quality, size);
    }

    // ── Comparable ───────────────────────────────────────────────────────────

    @Override
    public int compareTo(final SpelledPitch other) {
        return Integer.compare(this.midi(), other.midi());
    }

    // ── Object ───────────────────────────────────────────────────────────────

    /**
     * Value equality: name + accidental + octave. Two {@code SpelledPitch} objects with the same
     * spelling are equal regardless of which {@link MusicCodeModel} instance created them.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpelledPitch other)) {
            return false;
        }
        return octave() == other.octave() && name() == other.name() && accidental() == other.accidental();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), accidental(), octave());
    }

    @Override
    public String toString() {
        return name().name() + accidental().symbol() + octave();
    }

    // ── parse ─────────────────────────────────────────────────────────────────

    public static SpelledPitch parse(final String s) {
        Objects.requireNonNull(s, "input must not be null");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty string as pitch");
        }

        int i = 0;
        final NoteName noteName = switch (Character.toUpperCase(s.charAt(i++))) {
            case 'C' -> NoteName.C;
            case 'D' -> NoteName.D;
            case 'E' -> NoteName.E;
            case 'F' -> NoteName.F;
            case 'G' -> NoteName.G;
            case 'A' -> NoteName.A;
            case 'B' -> NoteName.B;
            default -> throw new IllegalArgumentException("Unknown note name: " + s.charAt(0));
        };

        Accidental accidental = Accidental.NATURAL;
        if (i < s.length()) {
            final char next = s.charAt(i);
            if (next == 'b' || next == '\u266D') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == 'b' || s.charAt(i + 1) == '\u266D')) {
                    accidental = Accidental.DOUBLE_FLAT;
                    i += 2;
                } else {
                    accidental = Accidental.FLAT;
                    i += 1;
                }
            } else if (next == '#' || next == '\u266F') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == '#' || s.charAt(i + 1) == '\u266F')) {
                    accidental = Accidental.DOUBLE_SHARP;
                    i += 2;
                } else {
                    accidental = Accidental.SHARP;
                    i += 1;
                }
            }
        }

        final int octave;
        try {
            octave = Integer.parseInt(s.substring(i));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse octave from: " + s);
        }

        return SpelledPitch.of(noteName, accidental, octave);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static int computeMidi(final NoteName name, final Accidental accidental, final int octave) {
        return (octave + 1) * 12 + name.semitonesAboveC() + accidental.semitoneOffset();
    }

    static {
        Marshalling.register(SpelledPitch.class, MethodHandles.lookup());
    }
}
