package build.music.score;

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
 * Ordered list of {@link StructuredVoice.Segment}s carried by a {@link StructuredVoice}.
 * Singular: a structured voice has exactly one segment sequence.
 */
@Singular
public record SegmentSequenceTrait(List<StructuredVoice.Segment> segments) implements Trait {

    public SegmentSequenceTrait {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments must not be null"));
    }

    @Unmarshal
    public SegmentSequenceTrait(final Marshaller marshaller,
                                final Stream<Marshalled<StructuredVoice.Segment>> segments) {
        this(segments.map(marshaller::unmarshal).toList());
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<StructuredVoice.Segment>>> segments) {
        segments.set(this.segments.stream().map(marshaller::marshal));
    }

    public static SegmentSequenceTrait of(final List<StructuredVoice.Segment> segments) {
        return new SegmentSequenceTrait(segments);
    }

    static {
        Marshalling.register(SegmentSequenceTrait.class, MethodHandles.lookup());
    }
}
