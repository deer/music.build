package build.music.form;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Trait;
import build.music.score.Voice;

import java.lang.invoke.MethodHandles;

/**
 * Adapter record serializing a single {@code Map.Entry<String, Voice>} from
 * {@link Section}'s voices map. The framework cannot marshal Java Maps directly.
 */
public record SectionVoicePair(String name, Voice voice) implements Trait {

    @Unmarshal
    public SectionVoicePair(final Marshaller marshaller,
                            final String name,
                            final Marshalled<Voice> voice) {
        this(name, marshaller.unmarshal(voice));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<String> name,
                           final Out<Marshalled<Voice>> voice) {
        name.set(this.name);
        voice.set(marshaller.marshal(this.voice));
    }

    static {
        Marshalling.register(SectionVoicePair.class, MethodHandles.lookup());
    }
}
