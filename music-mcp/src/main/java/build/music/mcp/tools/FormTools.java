package build.music.mcp.tools;

import build.music.core.ChordSymbol;
import build.music.form.FormBuilder;
import build.music.form.FormalPlan;
import build.music.form.Section;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.score.Score;
import build.music.score.Voice;
import build.music.time.TimeSignature;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tools for formal structure: sections, plans, and score assembly.
 */
public final class FormTools {

    private FormTools() {
    }

    /**
     * Tool: form.create_section — create a named section.
     * If voiceNames is null, uses all existing voices.
     */
    public static ToolResult createSection(final CompositionContext ctx, final String sectionName,
                                           final String voiceNames, final int measures) {
        try {
            final TimeSignature ts = ctx.getTimeSignature();

            final Map<String, Voice> sectionVoices = new HashMap<>();
            if (voiceNames == null || voiceNames.isBlank()) {
                // Use all voices
                for (final String name : ctx.voiceNames()) {
                    sectionVoices.put(name, Voice.of(name, ctx.getVoice(name)));
                }
            } else {
                for (final String name : voiceNames.trim().split("[,\\s]+")) {
                    final String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        sectionVoices.put(trimmed, Voice.of(trimmed, ctx.getVoice(trimmed)));
                    }
                }
            }

            if (sectionVoices.isEmpty()) {
                return ToolResult.error("No voices found for section. Create voices first.");
            }

            // Init or get form builder
            if (!ctx.hasFormBuilder()) {
                ctx.setFormBuilder(FormBuilder.create(ctx.getTitle())
                    .tempo(ctx.getTempo())
                    .timeSignature(ts));
            }

            final Section section = Section.of(sectionName, sectionName, sectionVoices, measures, ts);
            ctx.getFormBuilder().section(sectionName, section);
            // Snapshot current bar-chord map for this section so form.build can assemble
            // an absolute merged chord map across all sections in order.
            ctx.getFormBuilder().setSectionBarChords(sectionName, ctx.getBarChords());

            return ToolResult.success("Created section '" + sectionName + "' with " +
                sectionVoices.size() + " voice(s) over " + measures + " measure(s).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: form.repeat_section — repeat a previously defined section.
     */
    public static ToolResult repeatSection(final CompositionContext ctx, final String sectionName,
                                           final String newLabel) {
        try {
            if (!ctx.hasFormBuilder()) {
                return ToolResult.error("No form started. Use form.create_section first.");
            }
            if (newLabel != null && !newLabel.isBlank()) {
                ctx.getFormBuilder().repeatSection(sectionName, newLabel);
            } else {
                ctx.getFormBuilder().repeatSection(sectionName);
            }
            return ToolResult.success("Added repeat of section '" + sectionName + "'" +
                (newLabel != null && !newLabel.isBlank() ? " as '" + newLabel + "'" : "") + ".");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: form.set_ending — register a per-pass ending for a section.
     *
     * @param sectionName       name of the section to attach the ending to
     * @param pass              which pass this ending applies to (1-based)
     * @param endingSectionName name of a previously defined section whose bars replace the tail
     */
    public static ToolResult setEnding(final CompositionContext ctx, final String sectionName,
                                       final int pass, final String endingSectionName) {
        try {
            if (!ctx.hasFormBuilder()) {
                return ToolResult.error("No form started. Use form.create_section first.");
            }
            ctx.getFormBuilder().setEnding(sectionName, pass, endingSectionName);
            return ToolResult.success(
                "Registered ending for section '" + sectionName + "' pass " + pass +
                    " → section '" + endingSectionName + "'.");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: form.build — build the score from the formal plan.
     * Replaces the voices in the context with the assembled score's voices.
     */
    public static ToolResult buildScore(final CompositionContext ctx) {
        try {
            if (!ctx.hasFormBuilder()) {
                return ToolResult.error("No form defined. Use form.create_section and form.repeat_section first.");
            }

            final FormalPlan plan = ctx.getFormBuilder().build();
            final FormBuilder fb = ctx.getFormBuilder();

            // Build the score to get assembled voices and structured voices (for volta notation)
            final Score score = plan.toScore(Map.of());

            // Replace all voices with the assembled versions.
            // This removes stale per-section originals — the assembled voice has the same
            // name as the original but spans the full form (all repeated sections combined).
            for (final var part : score.scoreParts()) {
                ctx.createVoice(part.name(), part.voice().events());
            }

            // Propagate structured voices so LilyPond export can emit \repeat volta notation.
            if (!score.structuredVoices().isEmpty()) {
                ctx.setStructuredVoices(score.structuredVoices());
            }

            // Assemble a merged bar-chord map by offsetting each section's snapshot to its
            // absolute bar position in the assembled score.
            final Map<Integer, ChordSymbol> mergedChords = new LinkedHashMap<>();
            int barOffset = 0;
            for (final Section section : plan.sections()) {
                final Map<Integer, ChordSymbol> sectionChords = fb.getSectionBarChords(section.name());
                for (final Map.Entry<Integer, ChordSymbol> e : sectionChords.entrySet()) {
                    mergedChords.put(e.getKey() + barOffset, e.getValue());
                }
                barOffset += section.measures();
            }
            if (!mergedChords.isEmpty()) {
                ctx.setBarChords(mergedChords);
            }

            final String description = plan.describe();
            return ToolResult.success("Built score from formal plan: " + description +
                "\nTotal measures: " + plan.totalMeasures() +
                "\nAssembled " + score.scoreParts().size() + " part(s). " +
                "Use score.describe or export.midi to see/export the result.");
        } catch (final Exception e) {
            return ToolResult.error("Error building score: " + e.getMessage());
        }
    }

    /**
     * Tool: form.describe — show the current formal plan structure.
     */
    public static ToolResult describeForm(final CompositionContext ctx) {
        if (!ctx.hasFormBuilder()) {
            return ToolResult.success("No formal plan defined yet. Use form.create_section to start.");
        }
        try {
            final FormalPlan plan = ctx.getFormBuilder().build();
            return ToolResult.success(plan.describe() + "\nTotal: " + plan.totalMeasures() + " measures.");
        } catch (final Exception e) {
            return ToolResult.error("Error describing form: " + e.getMessage());
        }
    }
}
