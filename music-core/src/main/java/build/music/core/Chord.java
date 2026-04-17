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
import build.music.pitch.PitchClass;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.time.Duration;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Chord
    extends AbstractTraitable
    implements NoteEvent {

    private Chord(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Chord(@Bound final MusicCodeModel codeModel,
                 final Marshaller marshaller,
                 final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Chord of(final List<Pitch> pitches, final Duration duration, final Velocity velocity) {
        Objects.requireNonNull(pitches, "pitches must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(velocity, "velocity must not be null");
        if (pitches.size() < 2) {
            throw new IllegalArgumentException("Chord must have at least 2 pitches");
        }

        final List<Pitch> sorted = new ArrayList<>(pitches);
        sorted.sort(Comparator.comparingInt(Pitch::midi));
        final List<Pitch> immutable = List.copyOf(sorted);

        final Chord c = new Chord(MusicCodeModel.current());
        for (final Pitch p : immutable) {
            if (p instanceof SpelledPitch sp) {
                c.addTrait(sp);
            }
        }
        c.addTrait(OrderTrait.ascending(immutable.size()));
        c.addTrait(duration);
        c.addTrait(velocity);
        return c;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public List<Pitch> pitches() {
        return traits(SpelledPitch.class)
            .sorted(Comparator.comparingInt(SpelledPitch::midi))
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Duration duration() {
        return traits(Duration.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Chord has no duration trait"));
    }

    public Velocity velocity() {
        return getTrait(Velocity.class).orElseThrow();
    }

    // ── derived ───────────────────────────────────────────────────────────────

    public Pitch root() {
        return pitches().getFirst();
    }

    public Chord transpose(final SpelledInterval interval) {
        final List<Pitch> transposed = pitches().stream()
            .map(p -> (Pitch) p.spelled().transpose(interval))
            .toList();
        return Chord.of(transposed, duration(), velocity());
    }

    public boolean contains(final PitchClass pitchClass) {
        return pitches().stream().anyMatch(p -> p.pitchClass() == pitchClass);
    }

    /**
     * 0 = root position, 1 = first inversion, etc.
     */
    public int inversion() {
        return 0;
    }

    static {
        Marshalling.register(Chord.class, MethodHandles.lookup());
    }
}
