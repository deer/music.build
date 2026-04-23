package build.music.core;

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
import build.music.time.Duration;
import build.music.time.RhythmicValue;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.stream.Stream;

public final class Rest
    extends AbstractTraitable
    implements NoteEvent {

    private Rest(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Rest(@Bound final MusicCodeModel codeModel,
                final Marshaller marshaller,
                final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Rest of(final Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        final Rest r = new Rest(MusicCodeModel.current());
        r.addTrait(duration);
        return r;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    @Override
    public Duration duration() {
        return traits(Duration.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Rest has no duration trait"));
    }

    // ── Object ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        final Duration dur = duration();
        final String durStr = (dur instanceof RhythmicValue rv) ? rv.symbol() : dur.toString();
        return "rest/" + durStr;
    }

    static {
        Marshalling.register(Rest.class, MethodHandles.lookup());
    }
}
