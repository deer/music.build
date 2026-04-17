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
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.StructuredVoice;
import build.music.score.StructuredVoice.Segment;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.RhythmicValue;
import build.music.time.Tempo;
import build.music.voice.VoiceOperations;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A complete formal plan: an ordered sequence of sections that can be turned into a Score.
 */
public final class FormalPlan
    extends AbstractTraitable {

    private FormalPlan(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public FormalPlan(@Bound final MusicCodeModel codeModel,
                      final Marshaller marshaller,
                      final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    public static FormalPlan of(final String title, final List<Section> sections, final Tempo tempo) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(sections, "sections must not be null");
        Objects.requireNonNull(tempo, "tempo must not be null");
        final List<Section> immutable = List.copyOf(sections);
        final FormalPlan fp = new FormalPlan(MusicCodeModel.current());
        fp.addTrait(FormTitleTrait.of(title));
        fp.addTrait(SectionSequenceTrait.of(immutable));
        fp.addTrait(tempo);
        return fp;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String title() {
        return getTrait(FormTitleTrait.class).orElseThrow().title();
    }

    public List<Section> sections() {
        return getTrait(SectionSequenceTrait.class).orElseThrow().sections();
    }

    public Tempo tempo() {
        return getTrait(Tempo.class).orElseThrow();
    }

    // ── Object ───────────────────────────────────────────────────────────────

    static {
        Marshalling.register(FormalPlan.class, MethodHandles.lookup());
    }

    // ── derived ───────────────────────────────────────────────────────────────

    /**
     * Build a Score from this formal plan.
     */
    public Score toScore(final Map<String, Integer> midiPrograms) {
        final List<Section> sections = sections();
        final Set<String> allVoiceNames = new LinkedHashSet<>();
        for (final Section s : sections) {
            allVoiceNames.addAll(s.voiceNames());
        }

        final var ts = sections.isEmpty()
            ? build.music.time.TimeSignature.COMMON_TIME
            : sections.get(0).timeSignature();

        final boolean hasAnyEndings = sections.stream().anyMatch(s -> !s.endings().isEmpty());

        final Map<String, Integer> passCounts = new HashMap<>();

        final Map<String, List<Segment>> structuredSegments = new HashMap<>();
        if (hasAnyEndings) {
            for (final String vn : allVoiceNames) {
                structuredSegments.put(vn, new ArrayList<>());
            }
        }

        final Score.Builder builder = Score.builder(title()).tempo(tempo()).timeSignature(ts);

        int channel = 0;
        for (final String voiceName : allVoiceNames) {
            passCounts.clear();
            final List<Voice> voiceParts = new ArrayList<>();

            for (final Section section : sections) {
                final int pass = passCounts.merge(section.name(), 1, Integer::sum);
                final Section ending = section.endings().get(pass);

                if (section.voices().containsKey(voiceName)) {
                    final Voice full = section.voices().get(voiceName);
                    if (ending != null && ending.voices().containsKey(voiceName)) {
                        final Voice tailVoice = ending.voices().get(voiceName);
                        final Voice bodyVoice = trimToMeasures(full, section.measures() - ending.measures(), ts);
                        voiceParts.add(VoiceOperations.concat(voiceName, List.of(bodyVoice, tailVoice)));
                    } else {
                        voiceParts.add(trimToMeasures(full, section.measures(), ts));
                    }
                } else {
                    voiceParts.add(buildRestVoice(voiceName, section.duration()));
                }
            }

            final Voice combined = VoiceOperations.concat(voiceName, voiceParts);
            final int program = midiPrograms.getOrDefault(voiceName, 0);
            builder.part(Part.of(voiceName, channel % 16, program, combined));
            channel++;
        }

        List<StructuredVoice> structuredVoices = List.of();
        if (hasAnyEndings) {
            structuredVoices = buildStructuredVoices(allVoiceNames, ts);
        }
        builder.structuredVoices(structuredVoices.isEmpty() ? null : structuredVoices);

        return builder.build();
    }

    /**
     * Build StructuredVoices for LilyPond volta rendering.
     */
    private List<StructuredVoice> buildStructuredVoices(
        final Set<String> voiceNames, final build.music.time.TimeSignature ts) {
        final List<Section> sections = sections();
        final List<StructuredVoice> result = new ArrayList<>();

        for (final String voiceName : voiceNames) {
            final List<Segment> segments = new ArrayList<>();

            int i = 0;
            while (i < sections.size()) {
                final Section sec = sections.get(i);
                int j = i;
                while (j < sections.size() && sections.get(j).name().equals(sec.name())) {
                    j++;
                }
                final List<Section> group = sections.subList(i, j);

                final boolean groupHasEndings = group.stream().anyMatch(s -> !s.endings().isEmpty());

                if (group.size() > 1 && groupHasEndings) {
                    final int maxEndingMeasures = group.stream()
                        .flatMap(s -> s.endings().values().stream())
                        .mapToInt(Section::measures)
                        .max().orElse(0);
                    final int bodyMeasures = sec.measures() - maxEndingMeasures;

                    final List<NoteEvent> body;
                    if (sec.voices().containsKey(voiceName)) {
                        body = trimToMeasures(sec.voices().get(voiceName), bodyMeasures, ts).events();
                    } else {
                        body = buildRestVoice(voiceName, ts.measureDuration().multiply(bodyMeasures)).events();
                    }

                    final List<List<NoteEvent>> endings = new ArrayList<>();
                    for (int pass = 1; pass <= group.size(); pass++) {
                        final Section passSection = group.get(pass - 1);
                        final Section endingSection = passSection.endings().get(pass);
                        if (endingSection != null && endingSection.voices().containsKey(voiceName)) {
                            endings.add(endingSection.voices().get(voiceName).events());
                        } else {
                            if (sec.voices().containsKey(voiceName)) {
                                final Voice full = sec.voices().get(voiceName);
                                endings.add(tailMeasures(full, maxEndingMeasures, ts).events());
                            } else {
                                endings.add(buildRestVoice(voiceName,
                                    ts.measureDuration().multiply(maxEndingMeasures)).events());
                            }
                        }
                    }
                    segments.add(new Segment.Volta(body, endings));
                } else {
                    for (final Section s : group) {
                        if (s.voices().containsKey(voiceName)) {
                            segments.add(new Segment.Plain(
                                trimToMeasures(s.voices().get(voiceName), s.measures(), ts).events()));
                        } else {
                            segments.add(new Segment.Plain(
                                buildRestVoice(voiceName, s.duration()).events()));
                        }
                    }
                }
                i = j;
            }
            result.add(StructuredVoice.of(voiceName, segments));
        }
        return result;
    }

    /**
     * Total number of measures across all sections.
     */
    public int totalMeasures() {
        return sections().stream().mapToInt(Section::measures).sum();
    }

    /**
     * Total duration.
     */
    public Fraction totalDuration() {
        return sections().stream()
            .map(Section::duration)
            .reduce(Fraction.ZERO, Fraction::add);
    }

    /**
     * Get a section by name (first match).
     */
    public Optional<Section> section(final String name) {
        return sections().stream().filter(s -> s.name().equals(name)).findFirst();
    }

    /**
     * Describe the form: "ABA: A(4 bars) → B(8 bars) → A(4 bars)"
     */
    public String describe() {
        final List<Section> sections = sections();
        final String form = sections.stream().map(Section::label).collect(Collectors.joining(""));
        final String parts = sections.stream()
            .map(s -> s.label() + "(" + s.measures() + (s.measures() == 1 ? " bar" : " bars") + ")")
            .collect(Collectors.joining(" → "));
        return form + ": " + parts;
    }

    private static Voice trimToMeasures(final Voice voice, final int measures, final build.music.time.TimeSignature ts) {
        if (measures <= 0) {
            return Voice.of(voice.name(), List.of());
        }
        final Fraction cutoff = ts.measureDuration().multiply(measures);
        final List<NoteEvent> result = new ArrayList<>();
        Fraction cursor = Fraction.ZERO;
        for (final NoteEvent event : voice.events()) {
            if (cursor.compareTo(cutoff) >= 0) {
                break;
            }
            result.add(event);
            cursor = cursor.add(event.duration().fraction());
        }
        return Voice.of(voice.name(), result);
    }

    private static Voice tailMeasures(final Voice voice, final int measures, final build.music.time.TimeSignature ts) {
        final Fraction total = voice.duration();
        final Fraction cutoff = total.subtract(ts.measureDuration().multiply(measures));
        if (cutoff.compareTo(Fraction.ZERO) <= 0) {
            return voice;
        }
        final List<NoteEvent> result = new ArrayList<>();
        Fraction cursor = Fraction.ZERO;
        for (final NoteEvent event : voice.events()) {
            if (cursor.compareTo(cutoff) >= 0) {
                result.add(event);
            }
            cursor = cursor.add(event.duration().fraction());
        }
        return Voice.of(voice.name(), result);
    }

    private static Voice buildRestVoice(final String name, final Fraction duration) {
        final List<NoteEvent> rests = new ArrayList<>();
        Fraction remaining = duration;
        final Fraction whole = Fraction.ONE;
        while (remaining.compareTo(whole) >= 0) {
            rests.add(Rest.of(RhythmicValue.WHOLE));
            remaining = remaining.subtract(whole);
        }
        for (final var rv : new RhythmicValue[]{RhythmicValue.HALF, RhythmicValue.QUARTER,
            RhythmicValue.EIGHTH, RhythmicValue.SIXTEENTH}) {
            if (remaining.compareTo(rv.fraction()) >= 0) {
                rests.add(Rest.of(rv));
                remaining = remaining.subtract(rv.fraction());
            }
        }
        return Voice.of(name, rests);
    }
}
