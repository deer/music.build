package build.music.mcp;

import build.music.core.Articulation;
import build.music.core.ChordSymbol;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Velocity;
import build.music.form.FormBuilder;
import build.music.harmony.ChordProgression;
import build.music.harmony.Key;
import build.music.pitch.typesystem.MusicCodeModel;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.StructuredVoice;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.time.Tempo;
import build.music.time.TempoChange;
import build.music.time.TimeSignature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds mutable composition state for a single MCP session.
 * Not thread-safe — designed for single-user local server use.
 */
public final class CompositionContext {

    private final MusicCodeModel codeModel = new MusicCodeModel();

    public record PartAssignment(int channel, int program, String instrumentName) {
    }

    /**
     * An articulation applied to a contiguous range of bars in a voice.
     */
    public record ArticulationRange(int fromBar, int toBar, Articulation articulation) {
    }

    /**
     * Returns the {@link MusicCodeModel} for this session.
     */
    public MusicCodeModel codeModel() {
        return codeModel;
    }

    private final Map<String, List<NoteEvent>> voices = new LinkedHashMap<>();
    private final Map<String, List<NoteEvent>> motifs = new LinkedHashMap<>();
    private final Map<String, PartAssignment> partAssignments = new LinkedHashMap<>();
    private final Map<String, Velocity> voiceDynamics = new LinkedHashMap<>();
    private final Map<String, Articulation> voiceArticulations = new LinkedHashMap<>();
    private final Map<String, List<ArticulationRange>> voiceArticulationRanges = new LinkedHashMap<>();

    private String title = "Untitled";
    private TimeSignature timeSignature = TimeSignature.COMMON_TIME;
    private Tempo tempo = Tempo.of(120);
    private final List<TempoChange> tempoChanges = new ArrayList<>();
    private Key key = null;
    private ChordProgression progression = null;
    private FormBuilder formBuilder = null;
    private Fraction swingRatio = null;
    private final Map<Integer, ChordSymbol> barChords = new LinkedHashMap<>();
    private List<StructuredVoice> structuredVoices = new ArrayList<>();
    private final List<String> sessionLog = new ArrayList<>();
    private final List<String> sessionDisplayLog = new ArrayList<>();

    // --- Session log ---

    /**
     * Appends a pre-serialized JSON line to the in-memory session log.
     */
    public void addSessionLogLine(final String jsonLine) {
        sessionLog.add(jsonLine);
    }

    /**
     * Returns the accumulated session log lines (one JSON object per tool call).
     */
    public List<String> sessionLogLines() {
        return Collections.unmodifiableList(sessionLog);
    }

    /**
     * Appends a tab-prefixed display line ("ok\t..." or "err\t...") to the display log.
     */
    public void addSessionDisplayLine(final String line) {
        sessionDisplayLog.add(line);
    }

    /**
     * Returns the accumulated display log lines for console rendering.
     */
    public List<String> sessionDisplayLines() {
        return Collections.unmodifiableList(sessionDisplayLog);
    }

    // --- Voice operations ---

    public void createVoice(final String name, final List<NoteEvent> events) {
        voices.put(name, new ArrayList<>(events));
    }

    public void appendToVoice(final String name, final List<NoteEvent> events) {
        requireVoice(name);
        voices.get(name).addAll(events);
    }

    public List<NoteEvent> getVoice(final String name) {
        requireVoice(name);
        return Collections.unmodifiableList(voices.get(name));
    }

    public boolean hasVoice(final String name) {
        return voices.containsKey(name);
    }

    public Set<String> voiceNames() {
        return Collections.unmodifiableSet(voices.keySet());
    }

    public void deleteVoice(final String name) {
        requireVoice(name);
        voices.remove(name);
        partAssignments.remove(name);
        voiceDynamics.remove(name);
        voiceArticulations.remove(name);
        voiceArticulationRanges.remove(name);
    }

    // --- Motif operations ---

    public void saveMotif(final String name, final List<NoteEvent> events) {
        motifs.put(name, new ArrayList<>(events));
    }

    public List<NoteEvent> getMotif(final String name) {
        if (!motifs.containsKey(name)) {
            throw new IllegalArgumentException(
                "Motif '" + name + "' does not exist. Available motifs: " + motifs.keySet() +
                    ". Save one first with motif.save.");
        }
        return Collections.unmodifiableList(motifs.get(name));
    }

    public boolean hasMotif(final String name) {
        return motifs.containsKey(name);
    }

    public Set<String> motifNames() {
        return Collections.unmodifiableSet(motifs.keySet());
    }

    // --- Score metadata ---

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTimeSignature(final TimeSignature ts) {
        this.timeSignature = ts;
    }

    public TimeSignature getTimeSignature() {
        return timeSignature;
    }

    public void setTempo(final Tempo tempo) {
        this.tempo = tempo;
    }

    public Tempo getTempo() {
        return tempo;
    }

    /**
     * Add a gradual tempo change. Overlapping spans are rejected.
     * fromBpm is resolved here from the global tempo (or a prior change that ends before startBar).
     */
    public void addTempoChange(final int startBar, final int endBar, final int toBpm, final String curve) {
        // Check for overlaps
        for (final TempoChange existing : tempoChanges) {
            if (startBar <= existing.endBar() && endBar >= existing.startBar()) {
                throw new IllegalArgumentException(
                    "Tempo change [" + startBar + "-" + endBar + "] overlaps with existing change [" +
                        existing.startBar() + "-" + existing.endBar() + "].");
            }
        }
        // Resolve fromBpm: global tempo unless another change ends just before startBar
        int fromBpm = tempo.bpm();
        for (final TempoChange existing : tempoChanges) {
            if (existing.endBar() < startBar) {
                fromBpm = existing.toBpm();
            }
        }
        tempoChanges.add(new TempoChange(startBar, endBar, fromBpm, toBpm, curve));
    }

    public List<TempoChange> getTempoChanges() {
        return Collections.unmodifiableList(tempoChanges);
    }

    public boolean hasTempoChanges() {
        return !tempoChanges.isEmpty();
    }

    // --- Part assignments ---

    public void assignPart(final String voiceName, final int channel, final int program, final String instrumentName) {
        requireVoice(voiceName);
        partAssignments.put(voiceName, new PartAssignment(channel, program, instrumentName));
    }

    public Set<Integer> usedChannels() {
        return partAssignments.values().stream()
            .map(PartAssignment::channel)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Returns true if the voice is assigned to the GM percussion channel (9).
     */
    public boolean isPercussionVoice(final String voiceName) {
        final PartAssignment pa = partAssignments.get(voiceName);
        return pa != null && pa.channel() == 9;
    }

    // --- Build score ---

    public Score buildScore() {
        final var builder = Score.builder(title)
            .timeSignature(timeSignature)
            .tempo(tempo)
            .key(key)
            .swingRatio(swingRatio)
            .barChords(hasBarChords() ? barChords : null)
            .tempoChanges(tempoChanges.isEmpty() ? null : tempoChanges)
            .structuredVoices(structuredVoices.isEmpty() ? null : structuredVoices);

        int defaultChannel = 0;
        for (final Map.Entry<String, List<NoteEvent>> entry : voices.entrySet()) {
            final String name = entry.getKey();
            final List<NoteEvent> events = applyVoiceDefaults(name, entry.getValue());
            final Voice voice = Voice.of(name, events);

            final PartAssignment assignment = partAssignments.get(name);
            final Part part;
            if (assignment != null) {
                part = Part.of(name, assignment.channel(), assignment.program(), voice);
            } else {
                part = Part.of(name, defaultChannel, 0, voice);
                defaultChannel = (defaultChannel + 1) % 16;
            }
            builder.part(part);
        }

        return builder.build();
    }

    /**
     * Apply voice-level dynamics and articulation defaults to all notes in the voice.
     */
    private List<NoteEvent> applyVoiceDefaults(final String voiceName, final List<NoteEvent> events) {
        final Velocity dynamics = voiceDynamics.get(voiceName);
        final Articulation globalArticulation = voiceArticulations.get(voiceName);
        final List<ArticulationRange> ranges = voiceArticulationRanges.getOrDefault(voiceName, List.of());

        if (dynamics == null && globalArticulation == null && ranges.isEmpty()) {
            return events;
        }

        final Fraction measureDur = timeSignature.measureDuration();
        Fraction cursor = Fraction.ZERO;
        int bar = 1;
        final List<NoteEvent> result = new ArrayList<>(events.size());

        for (final NoteEvent event : events) {
            final int currentBar = bar;
            if (event instanceof Note n) {
                Note out = n;
                if (dynamics != null) {
                    out = out.withVelocity(dynamics);
                }

                // Range articulation overrides global; first matching range wins.
                Articulation effectiveArt = globalArticulation;
                for (final ArticulationRange r : ranges) {
                    if (currentBar >= r.fromBar() && currentBar <= r.toBar()) {
                        effectiveArt = r.articulation();
                        break;
                    }
                }
                if (effectiveArt != null && n.articulation() == Articulation.NORMAL) {
                    out = out.withArticulation(effectiveArt);
                }
                result.add(out);
            } else {
                result.add(event);
            }

            // Advance bar counter
            cursor = cursor.add(event.duration().fraction());
            while (cursor.compareTo(measureDur) >= 0) {
                cursor = cursor.subtract(measureDur);
                bar++;
            }
        }

        return result;
    }

    // --- State inspection ---

    public String describe() {
        final var sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        sb.append("Tempo: ").append(tempo.bpm()).append(" BPM");
        if (swingRatio != null) {
            sb.append(" (swing ").append(swingRatio).append(")");
        }
        sb.append("\n");
        sb.append("Time signature: ").append(timeSignature.beats()).append("/").append(timeSignature.beatUnit()).append("\n");
        if (key != null) {
            sb.append("Key: ").append(key).append("\n");
        }
        if (progression != null) {
            sb.append("Progression: ").append(progression).append("\n");
        }
        if (!barChords.isEmpty()) {
            sb.append("Bar chords: ").append(barChords.size()).append(" bar").append(barChords.size() == 1 ? "" : "s").append(" defined\n");
        }

        if (voices.isEmpty()) {
            sb.append("Voices: none\n");
        } else {
            sb.append("Voices (").append(voices.size()).append("):\n");
            for (final Map.Entry<String, List<NoteEvent>> entry : voices.entrySet()) {
                final String name = entry.getKey();
                final List<NoteEvent> events = entry.getValue();
                final Fraction total = events.stream()
                    .map(e -> e.duration().fraction())
                    .reduce(Fraction.ZERO, Fraction::add);
                final PartAssignment pa = partAssignments.get(name);
                final String instrument = pa != null ? " [" + pa.instrumentName() + "]" : "";
                final String dynamics = voiceDynamics.containsKey(name)
                    ? " dyn=" + dynamicsName(voiceDynamics.get(name)) : "";
                final String art = voiceArticulations.containsKey(name)
                    ? " art=" + voiceArticulations.get(name).name().toLowerCase() : "";
                sb.append("  ").append(name).append(": ")
                    .append(events.size()).append(" events, duration ")
                    .append(total).append(instrument).append(dynamics).append(art).append("\n");
            }
        }

        if (motifs.isEmpty()) {
            sb.append("Motifs: none\n");
        } else {
            sb.append("Motifs (").append(motifs.size()).append("):\n");
            for (final Map.Entry<String, List<NoteEvent>> entry : motifs.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().size()).append(" events\n");
            }
        }

        return sb.toString().stripTrailing();
    }

    // --- Voice dynamics ---

    public void setDynamics(final String voiceName, final Velocity velocity) {
        requireVoice(voiceName);
        voiceDynamics.put(voiceName, velocity);
    }

    public Velocity getDynamics(final String voiceName) {
        return voiceDynamics.getOrDefault(voiceName, Velocity.MF);
    }

    // --- Voice articulations ---

    public void setArticulation(final String voiceName, final Articulation articulation) {
        requireVoice(voiceName);
        voiceArticulations.put(voiceName, articulation);
    }

    public Articulation getArticulation(final String voiceName) {
        return voiceArticulations.getOrDefault(voiceName, Articulation.NORMAL);
    }

    /**
     * Add an articulation range for a specific bar span within a voice.
     * Overlapping ranges are rejected.
     */
    public void setArticulationRange(final String voiceName, final Articulation articulation, final int fromBar, final int toBar) {
        requireVoice(voiceName);
        if (fromBar < 1 || toBar < fromBar) {
            throw new IllegalArgumentException(
                "Invalid bar range: fromBar=" + fromBar + " toBar=" + toBar +
                    ". fromBar must be ≥ 1 and toBar must be ≥ fromBar.");
        }
        final List<ArticulationRange> ranges = voiceArticulationRanges.computeIfAbsent(voiceName, k -> new ArrayList<>());
        for (final ArticulationRange existing : ranges) {
            if (fromBar <= existing.toBar() && toBar >= existing.fromBar()) {
                throw new IllegalArgumentException(
                    "Articulation range [" + fromBar + "-" + toBar + "] overlaps with existing range [" +
                        existing.fromBar() + "-" + existing.toBar() + "] on voice '" + voiceName + "'.");
            }
        }
        ranges.add(new ArticulationRange(fromBar, toBar, articulation));
    }

    // --- Harmony / form state ---

    public void setKey(final Key key) {
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    public boolean hasKey() {
        return key != null;
    }

    public void setProgression(final ChordProgression prog) {
        this.progression = prog;
    }

    public ChordProgression getProgression() {
        return progression;
    }

    public void setFormBuilder(final FormBuilder fb) {
        this.formBuilder = fb;
    }

    public FormBuilder getFormBuilder() {
        return formBuilder;
    }

    public boolean hasFormBuilder() {
        return formBuilder != null;
    }

    public void setSwingRatio(final Fraction ratio) {
        this.swingRatio = ratio;
    }

    public Fraction getSwingRatio() {
        return swingRatio;
    }

    public boolean hasSwing() {
        return swingRatio != null;
    }

    public void setStructuredVoices(final List<StructuredVoice> sv) {
        this.structuredVoices = sv != null ? new ArrayList<>(sv) : new ArrayList<>();
    }

    public List<StructuredVoice> getStructuredVoices() {
        return Collections.unmodifiableList(structuredVoices);
    }

    public void setBarChords(final Map<Integer, ChordSymbol> chords) {
        barChords.clear();
        barChords.putAll(chords);
        // If a section was created before harmony.set_bars was called (i.e. it captured an
        // empty bar-chord map), retroactively populate it now so the order doesn't matter.
        if (formBuilder != null) {
            final String lastSection = formBuilder.lastSectionName();
            if (lastSection != null && formBuilder.getSectionBarChords(lastSection).isEmpty()) {
                formBuilder.setSectionBarChords(lastSection, Collections.unmodifiableMap(barChords));
            }
        }
    }

    public Map<Integer, ChordSymbol> getBarChords() {
        return Collections.unmodifiableMap(barChords);
    }

    public boolean hasBarChords() {
        return !barChords.isEmpty();
    }

    // --- Snapshot ---

    /**
     * Captures the current composition state as an immutable {@link CompositionSnapshot}.
     * Builds a {@link build.music.score.Score} from the current voices and metadata, then
     * packages it with the current motifs.
     */
    public CompositionSnapshot snapshot() {
        final Score score = buildScore();
        final List<MotifSnapshot> motifSnapshots = motifs.entrySet().stream()
            .map(e -> new MotifSnapshot(e.getKey(), List.copyOf(e.getValue())))
            .toList();
        return new CompositionSnapshot(CompositionSnapshot.SCHEMA_VERSION_STRING, score, motifSnapshots);
    }

    /**
     * Restores composition state from a {@link CompositionSnapshot}, replacing all current state.
     * Voice articulations and dynamics baked into the score's note events are preserved as-is;
     * they are not split back into separate dynamic/articulation maps.
     */
    public void restoreFrom(final CompositionSnapshot snap) {
        clear();
        final Score score = snap.score();
        title = score.title();
        tempo = score.tempo();
        timeSignature = score.timeSignature();
        key = score.key();
        swingRatio = score.swingRatio();

        for (final Part part : score.scoreParts()) {
            voices.put(part.name(), new ArrayList<>(part.voice().events()));
            partAssignments.put(part.name(),
                new PartAssignment(part.midiChannel(), part.midiProgram(), ""));
        }

        if (score.barChords() != null) {
            barChords.putAll(score.barChords());
        }
        tempoChanges.addAll(score.tempoChanges());

        for (final MotifSnapshot ms : snap.motifs()) {
            motifs.put(ms.name(), new ArrayList<>(ms.events()));
        }
    }

    // --- Reset ---

    public void clear() {
        voices.clear();
        motifs.clear();
        partAssignments.clear();
        voiceDynamics.clear();
        voiceArticulations.clear();
        voiceArticulationRanges.clear();
        tempoChanges.clear();
        barChords.clear();
        structuredVoices.clear();
        title = "Untitled";
        timeSignature = TimeSignature.COMMON_TIME;
        tempo = Tempo.of(120);
        key = null;
        progression = null;
        formBuilder = null;
        swingRatio = null;
    }

    // --- Internal helpers ---

    private void requireVoice(final String name) {
        if (!voices.containsKey(name)) {
            throw new IllegalArgumentException(
                "Voice '" + name + "' does not exist. Available voices: " + voices.keySet() +
                    ". Create it first with voice.create.");
        }
    }

    private static String dynamicsName(final Velocity v) {
        if (v.value() <= Velocity.PPP.value()) {
            return "ppp";
        }
        if (v.value() <= Velocity.PP.value()) {
            return "pp";
        }
        if (v.value() <= Velocity.P.value()) {
            return "p";
        }
        if (v.value() <= Velocity.MP.value()) {
            return "mp";
        }
        if (v.value() <= Velocity.MF.value()) {
            return "mf";
        }
        if (v.value() <= Velocity.F.value()) {
            return "f";
        }
        if (v.value() <= Velocity.FF.value()) {
            return "ff";
        }
        return "fff";
    }
}
