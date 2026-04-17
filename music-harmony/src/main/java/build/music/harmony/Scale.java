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
import build.music.pitch.Accidental;
import build.music.pitch.NoteName;
import build.music.pitch.PitchClass;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A specific scale: root note + scale type.
 * Scale pitches are spelled correctly using letter names, not enharmonic equivalents.
 */
public final class Scale
    extends AbstractTraitable {

    private Scale(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Scale(@Bound final MusicCodeModel codeModel,
                 final Marshaller marshaller,
                 final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Scale of(final NoteName root, final Accidental rootAccidental, final ScaleType type) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(rootAccidental, "rootAccidental must not be null");
        Objects.requireNonNull(type, "type must not be null");
        final Scale s = new Scale(MusicCodeModel.current());
        s.addTrait(root);
        s.addTrait(rootAccidental);
        s.addTrait(type);
        return s;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public NoteName root() {
        return getTrait(NoteName.class).orElseThrow();
    }

    public Accidental rootAccidental() {
        return getTrait(Accidental.class).orElseThrow();
    }

    public ScaleType type() {
        return getTrait(ScaleType.class).orElseThrow();
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Return all pitches of this scale in the given octave.
     */
    public List<SpelledPitch> pitches(final int octave) {
        final NoteName root = root();
        final Accidental rootAccidental = rootAccidental();
        final ScaleType type = type();
        final SpelledPitch rootPitch = SpelledPitch.of(root, rootAccidental, octave);
        final int rootMidi = rootPitch.midi();

        final List<SpelledPitch> result = new ArrayList<>();
        int cumSemi = 0;

        for (int i = 0; i < type.degreeCount(); i++) {
            final int targetMidi = rootMidi + cumSemi;
            final NoteName letter = root.steps(i);
            final int letterBase = letter.semitonesAboveC();

            final int q = targetMidi / 12;
            int offset = targetMidi - q * 12 - letterBase;
            int pitchOctave = q - 1;

            if (offset > 6) {
                offset -= 12;
                pitchOctave++;
            }
            if (offset < -6) {
                offset += 12;
                pitchOctave--;
            }

            final Accidental acc = Accidental.fromOffset(offset);
            result.add(SpelledPitch.of(letter, acc, pitchOctave));

            if (i < type.intervals().size()) {
                cumSemi += type.intervals().get(i);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Return the pitch at the given scale degree (1-based).
     */
    public SpelledPitch degree(final int degree, final int octave) {
        final ScaleType type = type();
        if (degree < 1 || degree > type.degreeCount()) {
            throw new IllegalArgumentException(
                "Degree " + degree + " out of range [1," + type.degreeCount() + "] for " + type);
        }
        return pitches(octave).get(degree - 1);
    }

    /**
     * Does this scale contain the given pitch class?
     */
    public boolean contains(final PitchClass pc) {
        return pitches(4).stream().anyMatch(p -> p.pitchClass() == pc);
    }

    /**
     * Does this scale contain the given spelled pitch (checks name+accidental, ignores octave)?
     */
    public boolean contains(final SpelledPitch pitch) {
        return pitches(4).stream().anyMatch(
            p -> p.name() == pitch.name() && p.accidental() == pitch.accidental());
    }

    /**
     * What scale degree (1-based) is this pitch? Returns empty if not in scale.
     */
    public Optional<Integer> degreeOf(final SpelledPitch pitch) {
        final List<SpelledPitch> p4 = pitches(4);
        for (int i = 0; i < p4.size(); i++) {
            if (p4.get(i).name() == pitch.name() && p4.get(i).accidental() == pitch.accidental()) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }

    public static Scale major(final NoteName root) {
        return Scale.of(root, Accidental.NATURAL, ScaleType.MAJOR);
    }

    public static Scale major(final NoteName root, final Accidental accidental) {
        return Scale.of(root, accidental, ScaleType.MAJOR);
    }

    public static Scale minor(final NoteName root) {
        return Scale.of(root, Accidental.NATURAL, ScaleType.NATURAL_MINOR);
    }

    public static Scale minor(final NoteName root, final Accidental accidental) {
        return Scale.of(root, accidental, ScaleType.NATURAL_MINOR);
    }

    /**
     * Parse "C major", "F# minor", "Bb dorian", "D harmonic minor", etc.
     */
    public static Scale parse(final String description) {
        Objects.requireNonNull(description, "description must not be null");
        final String[] parts = description.trim().split("\\s+", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Cannot parse scale: '" + description + "'. Expected 'Root Type'.");
        }

        final String rootStr = parts[0];
        final NoteName name = parseNoteName(rootStr.charAt(0));
        Accidental acc = Accidental.NATURAL;
        if (rootStr.length() > 1) {
            final String accStr = rootStr.substring(1);
            acc = switch (accStr) {
                case "#" -> Accidental.SHARP;
                case "b" -> Accidental.FLAT;
                case "##" -> Accidental.DOUBLE_SHARP;
                case "bb" -> Accidental.DOUBLE_FLAT;
                default -> throw new IllegalArgumentException("Unknown accidental: " + accStr);
            };
        }

        final ScaleType type = switch (parts[1].toLowerCase().replace(" ", "_")) {
            case "major" -> ScaleType.MAJOR;
            case "minor", "natural_minor" -> ScaleType.NATURAL_MINOR;
            case "harmonic_minor" -> ScaleType.HARMONIC_MINOR;
            case "melodic_minor" -> ScaleType.MELODIC_MINOR;
            case "dorian" -> ScaleType.DORIAN;
            case "phrygian" -> ScaleType.PHRYGIAN;
            case "lydian" -> ScaleType.LYDIAN;
            case "mixolydian" -> ScaleType.MIXOLYDIAN;
            case "aeolian" -> ScaleType.AEOLIAN;
            case "locrian" -> ScaleType.LOCRIAN;
            case "pentatonic_major", "major_pentatonic" -> ScaleType.PENTATONIC_MAJOR;
            case "pentatonic_minor", "minor_pentatonic" -> ScaleType.PENTATONIC_MINOR;
            case "blues" -> ScaleType.BLUES;
            case "chromatic" -> ScaleType.CHROMATIC;
            case "whole_tone" -> ScaleType.WHOLE_TONE;
            default -> throw new IllegalArgumentException("Unknown scale type: '" + parts[1] + "'");
        };

        return Scale.of(name, acc, type);
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
        return root().name() + rootAccidental().symbol() + " " + type().name().toLowerCase();
    }

    static {
        Marshalling.register(Scale.class, MethodHandles.lookup());
    }
}
