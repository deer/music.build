package build.music.form;

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
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.TimeSignature;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * A named, bounded unit of music with one or more voices.
 *
 * <p>A section may carry per-pass endings: on the Nth repetition of this section, the
 * final {@code endings.get(N).measures()} bars are replaced by the ending section's content.
 */
public final class Section
    extends AbstractTraitable {

    private Section(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Section(@Bound final MusicCodeModel codeModel,
                   final Marshaller marshaller,
                   final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static Section of(final String name, final String label, final Map<String, Voice> voices,
                             final int measures, final TimeSignature timeSignature,
                             final Map<Integer, Section> endings) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(voices, "voices must not be null");
        Objects.requireNonNull(timeSignature, "timeSignature must not be null");
        final Map<String, Voice> immutableVoices = Collections.unmodifiableMap(Map.copyOf(voices));
        final Map<Integer, Section> immutableEndings = endings != null
            ? Collections.unmodifiableMap(Map.copyOf(endings))
            : Map.of();

        final Section s = new Section(MusicCodeModel.current());
        s.addTrait(SectionNameTrait.of(name));
        s.addTrait(SectionLabelTrait.of(label));
        s.addTrait(MeasureCountTrait.of(measures));
        s.addTrait(timeSignature);
        for (final Map.Entry<String, Voice> e : immutableVoices.entrySet()) {
            s.addTrait(new SectionVoicePair(e.getKey(), e.getValue()));
        }
        for (final Map.Entry<Integer, Section> e : immutableEndings.entrySet()) {
            s.addTrait(new SectionEndingPair(e.getKey(), e.getValue()));
        }
        return s;
    }

    /**
     * Convenience: create a section without endings.
     */
    public static Section of(final String name, final String label, final Map<String, Voice> voices,
                             final int measures, final TimeSignature timeSignature) {
        return Section.of(name, label, voices, measures, timeSignature, Map.of());
    }

    /**
     * Convenience: create a simple single-voice section (no endings).
     */
    public static Section of(final String name, final String voiceName, final Voice voice,
                             final int measures, final TimeSignature ts) {
        return Section.of(name, name, Map.of(voiceName, voice), measures, ts, Map.of());
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String name() {
        return getTrait(SectionNameTrait.class).orElseThrow().name();
    }

    public String label() {
        return getTrait(SectionLabelTrait.class).orElseThrow().label();
    }

    public Map<String, Voice> voices() {
        return traits(SectionVoicePair.class)
            .collect(toUnmodifiableMap(SectionVoicePair::name, SectionVoicePair::voice));
    }

    public int measures() {
        return getTrait(MeasureCountTrait.class).orElseThrow().count();
    }

    public TimeSignature timeSignature() {
        return getTrait(TimeSignature.class).orElseThrow();
    }

    public Map<Integer, Section> endings() {
        return traits(SectionEndingPair.class)
            .collect(toUnmodifiableMap(SectionEndingPair::pass, SectionEndingPair::section));
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Total duration as a Fraction (measures × measureDuration).
     */
    public Fraction duration() {
        return timeSignature().measureDuration().multiply(measures());
    }

    /**
     * Get a specific voice from this section.
     */
    public Optional<Voice> voice(final String name) {
        return Optional.ofNullable(voices().get(name));
    }

    /**
     * Voice names present in this section.
     */
    public Set<String> voiceNames() {
        return voices().keySet();
    }

    static {
        Marshalling.register(Section.class, MethodHandles.lookup());
    }

    /**
     * Return a copy of this section with an additional ending for the given pass.
     */
    public Section withEnding(final int pass, final Section endingSection) {
        if (pass < 1) {
            throw new IllegalArgumentException("pass must be ≥ 1, got: " + pass);
        }
        Objects.requireNonNull(endingSection, "endingSection must not be null");
        if (endingSection.measures() > this.measures()) {
            throw new IllegalArgumentException(
                "Ending section '" + endingSection.name() + "' has " + endingSection.measures() +
                    " measures, which exceeds main section '" + name() + "' (" + measures() + " measures).");
        }
        final Map<Integer, Section> updated = new HashMap<>(endings());
        updated.put(pass, endingSection);
        return Section.of(name(), label(), voices(), measures(), timeSignature(), updated);
    }
}
