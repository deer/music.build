package build.music.form;

import build.music.core.ChordSymbol;
import build.music.score.Voice;
import build.music.time.Tempo;
import build.music.time.TimeSignature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for constructing formal plans.
 *
 * Usage:
 * <pre>
 * FormalPlan plan = FormBuilder.create("My Piece")
 *     .tempo(120)
 *     .timeSignature(TimeSignature.COMMON_TIME)
 *     .section("A", "melody", themeVoice, 4)
 *     .section("B", "melody", contrastVoice, 4)
 *     .repeatSection("A")
 *     .build();
 * </pre>
 */
public final class FormBuilder {

    private final String title;
    private Tempo tempo = Tempo.of(120);
    private TimeSignature timeSignature = TimeSignature.COMMON_TIME;

    private final List<Section> sections = new ArrayList<>();
    private final Map<String, Section> namedSections = new LinkedHashMap<>();
    // Chord map snapshot per section name, used to build a merged absolute bar-chord map.
    private final Map<String, Map<Integer, ChordSymbol>> sectionBarChords = new LinkedHashMap<>();

    private FormBuilder(final String title) {
        this.title = Objects.requireNonNull(title, "title must not be null");
    }

    public static FormBuilder create(final String title) {
        return new FormBuilder(title);
    }

    public FormBuilder tempo(final Tempo tempo) {
        this.tempo = Objects.requireNonNull(tempo);
        return this;
    }

    public FormBuilder tempo(final int bpm) {
        this.tempo = Tempo.of(bpm);
        return this;
    }

    public FormBuilder timeSignature(final TimeSignature ts) {
        this.timeSignature = Objects.requireNonNull(ts);
        return this;
    }

    /**
     * Add a pre-built section. The section is also registered by name for later repetition.
     */
    public FormBuilder section(final String name, final Section section) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(section, "section must not be null");
        sections.add(section);
        namedSections.put(name, section);
        return this;
    }

    /**
     * Add a section with a single voice (convenience method).
     */
    public FormBuilder section(final String name, final String voiceName, final Voice voice, final int measures) {
        final Section s = Section.of(name, name, Map.of(voiceName, voice), measures, timeSignature);
        return section(name, s);
    }

    /**
     * Repeat a previously defined section by name.
     */
    public FormBuilder repeatSection(final String existingSectionName) {
        return repeatSection(existingSectionName, existingSectionName);
    }

    /**
     * Repeat a previously defined section with a different display label.
     * The copy inherits the original's endings.
     */
    public FormBuilder repeatSection(final String existingSectionName, final String newLabel) {
        Objects.requireNonNull(existingSectionName, "existingSectionName must not be null");
        final Section original = namedSections.get(existingSectionName);
        if (original == null) {
            throw new IllegalArgumentException(
                "Section '" + existingSectionName + "' not found. " +
                "Available sections: " + namedSections.keySet());
        }
        // Create a copy with the new label, preserving endings
        final Section copy = Section.of(original.name(), newLabel, original.voices(),
            original.measures(), original.timeSignature(), original.endings());
        sections.add(copy);
        return this;
    }

    /**
     * Register an ending for a specific pass of a section.
     *
     * @param sectionName     the section to attach the ending to
     * @param pass            which pass this ending applies to (1-based)
     * @param endingSectionName  name of a previously defined section whose bars replace the tail
     */
    public FormBuilder setEnding(final String sectionName, final int pass, final String endingSectionName) {
        Objects.requireNonNull(sectionName, "sectionName must not be null");
        Objects.requireNonNull(endingSectionName, "endingSectionName must not be null");
        final Section main = namedSections.get(sectionName);
        if (main == null) {
            throw new IllegalArgumentException(
                "Section '" + sectionName + "' not found. Available: " + namedSections.keySet());
        }
        final Section ending = namedSections.get(endingSectionName);
        if (ending == null) {
            throw new IllegalArgumentException(
                "Ending section '" + endingSectionName + "' not found. Available: " + namedSections.keySet());
        }
        final Section updated = main.withEnding(pass, ending);
        namedSections.put(sectionName, updated);
        // Also update any already-queued occurrences of this section in the plan
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).name().equals(sectionName)) {
                final Section queued = sections.get(i);
                sections.set(i, Section.of(queued.name(), queued.label(), queued.voices(),
                    queued.measures(), queued.timeSignature(), updated.endings()));
            }
        }
        // Remove the ending section from the play sequence — it is a tail-replacement,
        // not a standalone section, so it must not produce extra bars.
        sections.removeIf(s -> s.name().equals(endingSectionName));
        return this;
    }

    /**
     * Record the bar-chord map that was active when a section was created.
     * Called by the MCP layer so form.build can assemble a merged absolute chord map.
     */
    public FormBuilder setSectionBarChords(final String sectionName, final Map<Integer, ChordSymbol> chords) {
        if (chords != null && !chords.isEmpty()) {
            sectionBarChords.put(sectionName, Map.copyOf(chords));
        }
        return this;
    }

    /** Return the bar-chord snapshot for the given section, or an empty map if none was recorded. */
    public Map<Integer, ChordSymbol> getSectionBarChords(final String sectionName) {
        return sectionBarChords.getOrDefault(sectionName, Map.of());
    }

    /** Return the name of the most recently added named section, or null if no sections exist. */
    public String lastSectionName() {
        if (namedSections.isEmpty()) {
            return null;
        }
        String last = null;
        for (final String name : namedSections.keySet()) {
            last = name;
        }
        return last;
    }

    public FormalPlan build() {
        if (sections.isEmpty()) {
            throw new IllegalStateException("FormalPlan must have at least one section");
        }
        return FormalPlan.of(title, sections, tempo);
    }
}
