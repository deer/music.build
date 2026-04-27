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
import build.music.core.Chord;
import build.music.core.ControlChange;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.ProgramChange;
import build.music.core.Rest;
import build.music.pitch.SpelledInterval;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.time.Fraction;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A named sequence of {@link NoteEvent}s in temporal order.
 * A raw melodic line — no bar lines, no phrase structure.
 */
public final class Voice
    extends AbstractTraitable
    implements Trait {

    private Voice(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Voice(@Bound final MusicCodeModel codeModel,
                 final Marshaller marshaller,
                 final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Voice of(final String name, final List<NoteEvent> events) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(events, "events must not be null");
        final List<NoteEvent> immutable = Collections.unmodifiableList(List.copyOf(events));
        final Voice v = new Voice(MusicCodeModel.current());
        v.addTrait(VoiceNameTrait.of(name));
        v.addTrait(EventSequenceTrait.of(immutable));
        return v;
    }

    // ── mereology ─────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterator<T> iterator(final Class<T> type) {
        if (type != null
                && (type.isAssignableFrom(NoteEvent.class) || NoteEvent.class.isAssignableFrom(type))) {
            return (Iterator<T>) events().stream().filter(type::isInstance).iterator();
        }
        return super.iterator(type);
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(VoiceNameTrait.class).orElseThrow().name();
    }

    public List<NoteEvent> events() {
        return getTrait(EventSequenceTrait.class).orElseThrow().events();
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Total duration as a fraction of a whole note.
     */
    public Fraction duration() {
        return events().stream()
            .map(e -> e.duration().fraction())
            .reduce(Fraction.ZERO, Fraction::add);
    }

    /**
     * Return a new Voice with all pitched events transposed by the given interval.
     */
    public Voice transpose(final SpelledInterval interval) {
        final List<NoteEvent> transposed = events().stream()
            .map(event -> switch (event) {
                case Note n -> (NoteEvent) n.transpose(interval);
                case Rest r -> r;
                case Chord c -> (NoteEvent) c.transpose(interval);
                case ControlChange cc -> cc;
                case ProgramChange pc -> pc;
            })
            .toList();
        return Voice.of(name(), transposed);
    }

    /**
     * Apply a transform to the event list, returning a new Voice. Pass {@code myTransform::apply}.
     */
    public Voice transform(final UnaryOperator<List<NoteEvent>> t) {
        return Voice.of(name(), t.apply(events()));
    }

    // ── Object ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Voice[" + name() + ", " + events().size() + " events]";
    }

    static {
        Marshalling.register(Voice.class, MethodHandles.lookup());
    }
}
