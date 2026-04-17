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
import build.music.core.NoteEvent;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A voice that carries its volta structure for LilyPond rendering.
 *
 * <p>The structure is a sequence of segments. Plain segments render as-is.
 * Volta segments render as {@code \repeat volta N { body } \alternative { ... }}.
 *
 * <p>The flat {@link Voice} in the corresponding {@link Part} is authoritative for MIDI;
 * this structure is used only by the LilyPond renderer.
 */
public final class StructuredVoice
    extends AbstractTraitable {

    private StructuredVoice(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public StructuredVoice(@Bound final MusicCodeModel codeModel,
                           final Marshaller marshaller,
                           final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static StructuredVoice of(final String name, final List<Segment> segments) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(segments);
        final List<Segment> immutable = Collections.unmodifiableList(List.copyOf(segments));
        final StructuredVoice sv = new StructuredVoice(MusicCodeModel.current());
        sv.addTrait(VoiceNameTrait.of(name));
        sv.addTrait(SegmentSequenceTrait.of(immutable));
        return sv;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(VoiceNameTrait.class).orElseThrow().name();
    }

    public List<Segment> segments() {
        return getTrait(SegmentSequenceTrait.class).orElseThrow().segments();
    }

    static {
        Marshalling.register(StructuredVoice.class, MethodHandles.lookup());
    }

    // ── nested types ─────────────────────────────────────────────────────────

    public sealed interface Segment permits Segment.Plain, Segment.Volta {

        /**
         * A sequence of notes played exactly once.
         */
        record Plain(List<NoteEvent> events) implements Segment {

            @Unmarshal
            public Plain(final Marshaller marshaller, final Stream<Marshalled<NoteEvent>> events) {
                this(events.map(marshaller::unmarshal).toList());
            }

            public Plain {
                events = Collections.unmodifiableList(List.copyOf(events));
            }

            @Marshal
            public void destructor(final Marshaller marshaller,
                                   final Out<Stream<Marshalled<NoteEvent>>> events) {
                events.set(this.events.stream().map(marshaller::marshal));
            }

            static {
                Marshalling.register(Plain.class, MethodHandles.lookup());
            }
        }

        /**
         * A repeated section with per-pass endings.
         * {@code body} plays on every pass. {@code endings.get(i)} plays on pass {@code i+1}.
         * Endings are serialized as a stream of {@link NoteEventList} wrapper records because the
         * framework cannot marshal {@code List<List<NoteEvent>>} directly.
         */
        record Volta(List<NoteEvent> body, List<List<NoteEvent>> endings) implements Segment {

            @Unmarshal
            public Volta(final Marshaller marshaller,
                         final Stream<Marshalled<NoteEvent>> body,
                         final Stream<Marshalled<NoteEventList>> endings) {
                this(body.map(marshaller::unmarshal).toList(),
                    endings.map(marshaller::unmarshal)
                        .map(NoteEventList::events)
                        .toList());
            }

            public Volta {
                Objects.requireNonNull(body);
                Objects.requireNonNull(endings);
                body = Collections.unmodifiableList(List.copyOf(body));
                endings = endings.stream()
                    .map(e -> Collections.unmodifiableList(List.copyOf(e)))
                    .toList();
            }

            @Marshal
            public void destructor(final Marshaller marshaller,
                                   final Out<Stream<Marshalled<NoteEvent>>> body,
                                   final Out<Stream<Marshalled<NoteEventList>>> endings) {
                body.set(this.body.stream().map(marshaller::marshal));
                endings.set(this.endings.stream()
                    .map(NoteEventList::new)
                    .map(marshaller::marshal));
            }

            static {
                Marshalling.register(Volta.class, MethodHandles.lookup());
            }
        }
    }
}
