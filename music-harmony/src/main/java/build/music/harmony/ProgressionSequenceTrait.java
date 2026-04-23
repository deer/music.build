package build.music.harmony;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Carries the ordered list of {@link RomanNumeral}s that form a {@link ChordProgression}.
 * Singular: a progression has exactly one sequence of chords.
 */
@Singular
public record ProgressionSequenceTrait(List<RomanNumeral> numerals) implements Trait {

    @Unmarshal
    public ProgressionSequenceTrait(final Marshaller marshaller,
                                    final Stream<Marshalled<RomanNumeral>> numerals) {
        this(numerals.map(marshaller::unmarshal).toList());
    }

    public ProgressionSequenceTrait {
        numerals = List.copyOf(Objects.requireNonNull(numerals, "numerals must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<RomanNumeral>>> numerals) {
        numerals.set(this.numerals.stream().map(marshaller::marshal));
    }

    public static ProgressionSequenceTrait of(final List<RomanNumeral> numerals) {
        return new ProgressionSequenceTrait(numerals);
    }

    static {
        Marshalling.register(ProgressionSequenceTrait.class, MethodHandles.lookup());
    }
}
