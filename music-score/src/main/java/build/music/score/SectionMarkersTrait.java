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
 * Ordered list of section boundaries for MIDI marker meta messages. Singular: a score has at most
 * one section timeline.
 */
@Singular
public record SectionMarkersTrait(List<SectionMarker> markers) implements Trait {

    @Unmarshal
    public SectionMarkersTrait(final Marshaller marshaller, final Stream<Marshalled<SectionMarker>> markers) {
        this(markers.map(marshaller::unmarshal).toList());
    }

    public SectionMarkersTrait {
        markers = List.copyOf(Objects.requireNonNull(markers, "markers must not be null"));
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Stream<Marshalled<SectionMarker>>> markers) {
        markers.set(this.markers.stream().map(marshaller::marshal));
    }

    public static SectionMarkersTrait of(final List<SectionMarker> markers) {
        return new SectionMarkersTrait(markers);
    }

    static {
        Marshalling.register(SectionMarkersTrait.class, MethodHandles.lookup());
    }
}
