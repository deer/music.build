package build.music.instrument;

import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.codemodel.foundation.descriptor.AbstractTraitable;
import build.codemodel.foundation.descriptor.Trait;
import build.music.core.Articulation;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.pitch.SpelledInterval;
import build.music.pitch.typesystem.MusicCodeModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An instrument definition with range and MIDI mapping.
 */
public final class Instrument
    extends AbstractTraitable {

    private Instrument(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Instrument(@Bound final MusicCodeModel codeModel,
                      final Marshaller marshaller,
                      final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Instrument of(final String name,
                                final InstrumentFamily family,
                                final PitchRange writtenRange,
                                final PitchRange comfortableRange,
                                final int midiProgram,
                                final List<Articulation> availableArticulations,
                                final boolean transposing,
                                final SpelledInterval transposition) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(family, "family must not be null");
        Objects.requireNonNull(writtenRange, "writtenRange must not be null");
        Objects.requireNonNull(comfortableRange, "comfortableRange must not be null");
        if (midiProgram < 0 || midiProgram > 127) {
            throw new IllegalArgumentException("midiProgram must be 0-127, got: " + midiProgram);
        }
        final List<Articulation> immutableArticulations = List.copyOf(availableArticulations);
        final Instrument inst = new Instrument(MusicCodeModel.current());
        inst.addTrait(InstrumentNameTrait.of(name));
        inst.addTrait(family);
        inst.addTrait(WrittenRangeTrait.of(writtenRange));
        inst.addTrait(ComfortableRangeTrait.of(comfortableRange));
        inst.addTrait(MidiProgramTrait.of(midiProgram));
        for (final Articulation a : immutableArticulations) {
            inst.addTrait(a);
        }
        if (transposing) {
            inst.addTrait(TransposingMarker.INSTANCE);
        }
        if (transposition != null) {
            inst.addTrait(transposition);
        }
        return inst;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(InstrumentNameTrait.class).orElseThrow().name();
    }

    public InstrumentFamily family() {
        return getTrait(InstrumentFamily.class).orElseThrow();
    }

    public PitchRange writtenRange() {
        return getTrait(WrittenRangeTrait.class).orElseThrow().range();
    }

    public PitchRange comfortableRange() {
        return getTrait(ComfortableRangeTrait.class).orElseThrow().range();
    }

    public int midiProgram() {
        return getTrait(MidiProgramTrait.class).orElseThrow().program();
    }

    public List<Articulation> availableArticulations() {
        return traits(Articulation.class).toList();
    }

    public boolean transposing() {
        return hasTrait(TransposingMarker.class);
    }

    public SpelledInterval transposition() {
        return getTrait(SpelledInterval.class).orElse(null);
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Check if all pitched notes in a voice are within this instrument's written range.
     */
    public boolean canPlay(final List<NoteEvent> events) {
        return events.stream()
            .filter(e -> e instanceof Note)
            .map(e -> (Note) e)
            .allMatch(n -> writtenRange().contains(n.pitch().spelled()));
    }

    /**
     * Find indices of notes that are out of written range.
     */
    public List<Integer> outOfRangeNotes(final List<NoteEvent> events) {
        final List<Integer> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) instanceof Note n) {
                if (!writtenRange().contains(n.pitch().spelled())) {
                    result.add(i);
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Suggested clef for this instrument.
     */
    public String clef() {
        final PitchRange wr = writtenRange();
        final int mid = (wr.low().midi() + wr.high().midi()) / 2;
        if (mid >= 55) {
            return "treble";
        }
        if (mid >= 45) {
            return "alto";
        }
        if (mid >= 36) {
            return "bass";
        }
        return "bass";
    }

    @Override
    public String toString() {
        return name() + " (" + family() + ", MIDI " + midiProgram() + ", " + writtenRange() + ")";
    }

    static {
        Marshalling.register(Instrument.class, MethodHandles.lookup());
    }
}
