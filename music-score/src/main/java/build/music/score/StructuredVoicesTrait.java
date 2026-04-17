package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * List of {@link StructuredVoice}s carrying volta/repeat structure for LilyPond rendering.
 * Singular: a score has at most one set of structured voices.
 */
@Singular
public record StructuredVoicesTrait(List<StructuredVoice> voices) implements Trait {

    @Unmarshal
    public StructuredVoicesTrait {
        voices = List.copyOf(Objects.requireNonNull(voices, "voices must not be null"));
    }

    @Marshal
    public void destructor(final Out<List<StructuredVoice>> voices) {
        voices.set(this.voices);
    }

    public static StructuredVoicesTrait of(final List<StructuredVoice> voices) {
        return new StructuredVoicesTrait(voices);
    }

    static {
        Marshalling.register(StructuredVoicesTrait.class, MethodHandles.lookup());
    }
}
