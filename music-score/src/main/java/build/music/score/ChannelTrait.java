package build.music.score;

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.Singular;
import build.codemodel.foundation.descriptor.Trait;

import java.lang.invoke.MethodHandles;

/**
 * MIDI channel assignment as a singular trait for {@link Voice} or {@link Part}.
 */
@Singular
public record ChannelTrait(int channel) implements Trait {

    @Unmarshal
    public ChannelTrait {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("MIDI channel must be 0-15, got: " + channel);
        }
    }

    @Marshal
    public void destructor(final Out<Integer> channel) {
        channel.set(this.channel);
    }

    public static ChannelTrait of(final int channel) {
        return new ChannelTrait(channel);
    }

    static {
        Marshalling.register(ChannelTrait.class, MethodHandles.lookup());
    }
}
