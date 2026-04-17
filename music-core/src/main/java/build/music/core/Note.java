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
import build.music.pitch.Pitch;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.time.Duration;
import build.music.time.RhythmicValue;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.stream.Stream;

public final class Note
    extends AbstractTraitable
    implements NoteEvent {

    private Note(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Note(@Bound final MusicCodeModel codeModel,
                final Marshaller marshaller,
                final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    /**
     * Convenience factory: MF velocity, NORMAL articulation, not tied.
     */
    public static Note of(final Pitch pitch, final Duration duration) {
        return Note.of(pitch, duration, Velocity.MF, Articulation.NORMAL, false);
    }

    /**
     * Full factory — reads the ambient {@link MusicCodeModel} from the ScopedValue.
     */
    public static Note of(final Pitch pitch, final Duration duration, final Velocity velocity,
                          final Articulation articulation, final boolean tied) {
        Objects.requireNonNull(pitch, "pitch must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(velocity, "velocity must not be null");
        Objects.requireNonNull(articulation, "articulation must not be null");
        final Note n = new Note(MusicCodeModel.current());
        if (pitch instanceof SpelledPitch sp) {
            n.addTrait(sp);
        }
        n.addTrait(duration);
        n.addTrait(velocity);
        n.addTrait(articulation);
        if (tied) {
            n.addTrait(TiedMarker.INSTANCE);
        }
        return n;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public Pitch pitch() {
        return getTrait(SpelledPitch.class)
            .map(sp -> (Pitch) sp)
            .orElseThrow(() -> new IllegalStateException("Note has no pitch trait"));
    }

    @Override
    public Duration duration() {
        return traits(Duration.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Note has no duration trait"));
    }

    public Velocity velocity() {
        return getTrait(Velocity.class).orElseThrow();
    }

    public Articulation articulation() {
        return getTrait(Articulation.class).orElseThrow();
    }

    public boolean tied() {
        return hasTrait(TiedMarker.class);
    }

    // ── derived ───────────────────────────────────────────────────────────────

    public int midi() {
        return pitch().midi();
    }

    public Note transpose(final SpelledInterval interval) {
        return Note.of(pitch().spelled().transpose(interval), duration(), velocity(), articulation(), tied());
    }

    public Note withVelocity(final Velocity velocity) {
        return Note.of(pitch(), duration(), velocity, articulation(), tied());
    }

    public Note withArticulation(final Articulation articulation) {
        return Note.of(pitch(), duration(), velocity(), articulation, tied());
    }

    public Note withTied(final boolean tied) {
        return Note.of(pitch(), duration(), velocity(), articulation(), tied);
    }

    // ── Object ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        final Duration dur = duration();
        final String durStr = (dur instanceof RhythmicValue rv) ? rv.symbol() : dur.toString();
        return pitch().toString() + "/" + durStr;
    }

    static {
        Marshalling.register(Note.class, MethodHandles.lookup());
    }
}
