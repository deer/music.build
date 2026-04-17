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
import build.music.core.ChordSymbol;
import build.music.harmony.Key;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.time.Fraction;
import build.music.time.Tempo;
import build.music.time.TempoChange;
import build.music.time.TimeSignature;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The top-level composition container.
 */
public final class Score
    extends AbstractTraitable {

    private Score(final MusicCodeModel codeModel) {
        super(codeModel);
    }

    @Unmarshal
    public Score(@Bound final MusicCodeModel codeModel,
                 final Marshaller marshaller,
                 final Stream<Marshalled<Trait>> traits) {
        super(codeModel, marshaller, traits);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<Trait>>> traits) {
        super.destructor(marshaller, traits);
    }

    static Score of(final String title,
                    final TimeSignature timeSignature,
                    final Tempo tempo,
                    final List<Part> parts,
                    final Key key,
                    final Fraction swingRatio,
                    final Map<Integer, ChordSymbol> barChords,
                    final List<TempoChange> tempoChanges,
                    final List<StructuredVoice> structuredVoices) {

        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(timeSignature, "timeSignature must not be null");
        Objects.requireNonNull(tempo, "tempo must not be null");
        Objects.requireNonNull(parts, "parts must not be null");

        final List<Part> immutableParts = Collections.unmodifiableList(List.copyOf(parts));
        final Map<Integer, ChordSymbol> immutableBarChords =
            barChords != null ? Collections.unmodifiableMap(barChords) : null;
        final List<TempoChange> immutableTempoChanges =
            tempoChanges != null ? Collections.unmodifiableList(List.copyOf(tempoChanges)) : List.of();
        final List<StructuredVoice> immutableSV =
            structuredVoices != null ? Collections.unmodifiableList(List.copyOf(structuredVoices)) : List.of();

        final Score s = new Score(MusicCodeModel.current());

        s.addTrait(ScoreTitleTrait.of(title));
        s.addTrait(timeSignature);
        s.addTrait(tempo);
        if (key != null) {
            s.addTrait(key);
        }
        for (final Part part : immutableParts) {
            s.addTrait(part);
        }
        if (swingRatio != null) {
            s.addTrait(SwingRatioTrait.of(swingRatio));
        }
        if (immutableBarChords != null) {
            s.addTrait(BarChordsTrait.of(immutableBarChords));
        }
        if (!immutableTempoChanges.isEmpty()) {
            s.addTrait(TempoChangesTrait.of(immutableTempoChanges));
        }
        if (!immutableSV.isEmpty()) {
            s.addTrait(StructuredVoicesTrait.of(immutableSV));
        }

        return s;
    }

    // ── accessors ────────────────────────────────────────────────────────────

    public String title() {
        return getTrait(ScoreTitleTrait.class).orElseThrow().title();
    }

    public TimeSignature timeSignature() {
        return getTrait(TimeSignature.class).orElseThrow();
    }

    public Tempo tempo() {
        return getTrait(Tempo.class).orElseThrow();
    }

    public List<Part> scoreParts() {

        return traits(Part.class).toList();
    }

    public Key key() {
        return getTrait(Key.class).orElse(null);
    }

    public Fraction swingRatio() {
        return getTrait(SwingRatioTrait.class).map(SwingRatioTrait::ratio).orElse(null);
    }

    public Map<Integer, ChordSymbol> barChords() {
        return getTrait(BarChordsTrait.class).map(BarChordsTrait::chords).orElse(null);
    }

    public List<TempoChange> tempoChanges() {
        return getTrait(TempoChangesTrait.class).map(TempoChangesTrait::changes).orElse(List.of());
    }

    public List<StructuredVoice> structuredVoices() {
        return getTrait(StructuredVoicesTrait.class).map(StructuredVoicesTrait::voices).orElse(List.of());
    }

    // ── derived ───────────────────────────────────────────────────────────────

    public Optional<Part> part(final String name) {
        return scoreParts().stream().filter(p -> p.name().equals(name)).findFirst();
    }

    /**
     * Duration of the longest part.
     */
    public Fraction duration() {
        return scoreParts().stream()
            .map(p -> p.voice().duration())
            .max(Fraction::compareTo)
            .orElse(Fraction.ZERO);
    }

    static {
        Marshalling.register(Score.class, MethodHandles.lookup());
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder(final String title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final String title;
        private TimeSignature timeSignature = TimeSignature.COMMON_TIME;
        private Tempo tempo = Tempo.of(120);
        private final List<Part> parts = new ArrayList<>();
        private Key key = null;
        private Fraction swingRatio = null;
        private Map<Integer, ChordSymbol> barChords = null;
        private List<TempoChange> tempoChanges = null;
        private List<StructuredVoice> structuredVoices = null;

        private Builder(final String title) {
            this.title = Objects.requireNonNull(title, "title must not be null");
        }

        public Builder timeSignature(final TimeSignature ts) {
            this.timeSignature = Objects.requireNonNull(ts);
            return this;
        }

        public Builder tempo(final Tempo tempo) {
            this.tempo = Objects.requireNonNull(tempo);
            return this;
        }

        public Builder key(final Key k) {
            this.key = k;
            return this;
        }

        public Builder swingRatio(final Fraction f) {
            this.swingRatio = f;
            return this;
        }

        public Builder barChords(final Map<Integer, ChordSymbol> chords) {
            this.barChords = chords;
            return this;
        }

        public Builder tempoChanges(final List<TempoChange> changes) {
            this.tempoChanges = changes;
            return this;
        }

        public Builder structuredVoices(final List<StructuredVoice> sv) {
            this.structuredVoices = sv;
            return this;
        }

        public Builder part(final Part part) {
            this.parts.add(Objects.requireNonNull(part));
            return this;
        }

        public Score build() {
            return Score.of(title, timeSignature, tempo, parts, key, swingRatio,
                barChords, tempoChanges, structuredVoices);
        }
    }
}
