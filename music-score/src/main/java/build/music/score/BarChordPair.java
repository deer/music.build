package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.music.core.ChordSymbol;

import java.lang.invoke.MethodHandles;

/**
 * Adapter record serializing a single entry from {@link BarChordsTrait}'s
 * {@code Map<Integer, ChordSymbol>}. The framework cannot marshal Java Maps directly;
 * Score serializes bar-chord data as {@code Stream<Marshalled<BarChordPair>>}.
 */
public record BarChordPair(int bar, ChordSymbol chord) {

    @Unmarshal
    public BarChordPair(final Marshaller marshaller,
                        final int bar,
                        final Marshalled<ChordSymbol> chord) {
        this(bar, marshaller.unmarshal(chord));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Integer> bar,
                           final Out<Marshalled<ChordSymbol>> chord) {
        bar.set(this.bar);
        chord.set(marshaller.marshal(this.chord));
    }

    static {
        Marshalling.register(BarChordPair.class, MethodHandles.lookup());
    }
}
