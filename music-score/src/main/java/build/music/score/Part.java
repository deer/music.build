package build.music.score;

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
 * A {@link Voice} assigned to a specific instrument for playback.
 *
 * <p>midiChannel: 0–15 (channel 9 is percussion by GM convention)
 * <p>midiProgram: 0–127 General MIDI program number
 */
public final class Part
    extends AbstractTraitable
    implements Trait {

    private Part(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Part(@Bound final MusicCodeModel codeModel,
                final Marshaller marshaller,
                final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Part of(final String name, final int midiChannel, final int midiProgram, final Voice voice) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(voice, "voice must not be null");
        if (midiChannel < 0 || midiChannel > 15) {
            throw new IllegalArgumentException("midiChannel must be 0-15, got: " + midiChannel);
        }
        if (midiProgram < 0 || midiProgram > 127) {
            throw new IllegalArgumentException("midiProgram must be 0-127, got: " + midiProgram);
        }
        final Part p = new Part(MusicCodeModel.current());
        p.addTrait(PartNameTrait.of(name));
        p.addTrait(ChannelTrait.of(midiChannel));
        p.addTrait(ProgramTrait.of(midiProgram));
        p.addTrait(voice);
        return p;
    }

    public static Part piano(final String name, final Voice voice) {
        return Part.of(name, 0, 0, voice);
    }

    public static Part strings(final String name, final Voice voice) {
        return Part.of(name, 0, 48, voice);
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(PartNameTrait.class).orElseThrow().name();
    }

    public int midiChannel() {
        return getTrait(ChannelTrait.class).orElseThrow().channel();
    }

    public int midiProgram() {
        return getTrait(ProgramTrait.class).orElseThrow().program();
    }

    public Voice voice() {
        return getTrait(Voice.class).orElseThrow();
    }

    static {
        Marshalling.register(Part.class, MethodHandles.lookup());
    }
}
