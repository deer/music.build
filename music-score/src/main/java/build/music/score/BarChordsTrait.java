package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;
import build.music.core.ChordSymbol;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Map of bar number → chord symbol for a {@link Score}. Singular: a score has at most one
 * bar-chord overlay. Keys are 1-based bar numbers.
 */
@Singular
public record BarChordsTrait(Map<Integer, ChordSymbol> chords) implements Trait {

    @Unmarshal
    public BarChordsTrait(final Marshaller marshaller, final Stream<Marshalled<BarChordPair>> chords) {
        this(chords.map(marshaller::unmarshal)
            .collect(Collectors.toUnmodifiableMap(BarChordPair::bar, BarChordPair::chord)));
    }

    public BarChordsTrait {
        chords = Map.copyOf(Objects.requireNonNull(chords, "chords must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Stream<Marshalled<BarChordPair>>> chords) {
        chords.set(this.chords.entrySet().stream()
            .map(e -> marshaller.marshal(new BarChordPair(e.getKey(), e.getValue()))));
    }

    public static BarChordsTrait of(final Map<Integer, ChordSymbol> chords) {
        return new BarChordsTrait(chords);
    }

    static {
        Marshalling.register(BarChordsTrait.class, MethodHandles.lookup());
    }
}
