package build.music.mcp.tools;

import build.music.instrument.Instrument;
import build.music.instrument.Instruments;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.rules.MeterRule;
import build.music.rules.RangeRule;
import build.music.rules.Rule;
import build.music.rules.RuleSet;
import build.music.rules.Violation;
import build.music.rules.VoiceLeadingRule;
import build.music.score.Score;
import build.music.score.Voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * MCP tools for composition validation.
 */
public final class RulesTools {

    private RulesTools() {
    }

    /**
     * Tool: rules.check — run all available rules and report violations.
     * Percussion voices (channel 9) are skipped — drum note numbers are not pitches.
     */
    public static ToolResult check(final CompositionContext ctx) {
        try {
            final Score score = ctx.buildScore();

            // Identify percussion voices to skip
            final Set<String> percussionVoices = new java.util.HashSet<>();
            for (final String name : ctx.voiceNames()) {
                if (ctx.isPercussionVoice(name)) {
                    percussionVoices.add(name);
                }
            }

            final List<Rule> rules = new ArrayList<>();
            rules.add(new VoiceLeadingRule());
            rules.add(new MeterRule(ctx.getTimeSignature()));
            final RuleSet ruleSet = new RuleSet(rules);

            // Check only non-percussion parts
            final List<Violation> violations = new ArrayList<>();
            for (final var part : score.scoreParts()) {
                if (percussionVoices.contains(part.name())) {
                    continue;
                }
                final Voice voice = part.voice();
                for (final Rule rule : rules) {
                    violations.addAll(rule.check(voice, part.name()));
                }
            }

            final List<String> checkedVoices = ctx.voiceNames().stream()
                .filter(n -> !percussionVoices.contains(n))
                .toList();

            final String skipNote = percussionVoices.isEmpty() ? "" :
                " (skipped " + percussionVoices.size() + " percussion voice(s): " + percussionVoices + ")";
            if (violations.isEmpty()) {
                return ToolResult.success("✓ No violations found" + skipNote + ".");
            }
            return formatViolationReport(violations, checkedVoices, skipNote);
        } catch (final Exception e) {
            return ToolResult.error("Error running rules: " + e.getMessage());
        }
    }

    /**
     * Tool: rules.check_range — check all voices against their assigned instrument ranges.
     */
    public static ToolResult checkRange(final CompositionContext ctx, final String voiceName,
                                        final String instrumentName) {
        try {
            Optional<Instrument> instrOpt = Instruments.byName(instrumentName);
            if (instrOpt.isEmpty()) {
                // Try case-insensitive partial match
                final String lower = instrumentName.toLowerCase();
                instrOpt = Instruments.all().stream()
                    .filter(i -> i.name().toLowerCase().contains(lower))
                    .findFirst();
            }
            if (instrOpt.isEmpty()) {
                return ToolResult.error("Unknown instrument: '" + instrumentName +
                    "'. Try: Piano, Flute, Violin, Cello, Trumpet, etc.");
            }
            final Instrument instrument = instrOpt.get();

            final Voice voice = Voice.of(voiceName, ctx.getVoice(voiceName));
            final RangeRule rule = new RangeRule(instrument);
            final List<Violation> violations = rule.check(voice, voiceName);

            if (violations.isEmpty()) {
                return ToolResult.success("✓ All notes in '" + voiceName + "' are within " +
                    instrument.name() + " range " + instrument.writtenRange() + ".");
            }
            return formatViolationReport(violations, List.of(voiceName));
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    private static ToolResult formatViolationReport(final List<Violation> violations, final List<String> voices,
                                                    final String skipNote) {
        final long errors = violations.stream().filter(v -> v.severity().equals("error")).count();
        final long warnings = violations.stream().filter(v -> v.severity().equals("warning")).count();
        final long suggestions = violations.stream().filter(v -> v.severity().equals("suggestion")).count();

        final StringBuilder sb = new StringBuilder();
        sb.append(violations.size()).append(" violation(s)").append(skipNote).append(": ")
            .append(errors).append(" error(s), ")
            .append(warnings).append(" warning(s), ")
            .append(suggestions).append(" suggestion(s)\n\n");
        for (final Violation v : violations) {
            sb.append(v).append("\n");
        }
        return ToolResult.success(sb.toString().stripTrailing());
    }

    /**
     * Overload for range-check (no skip note).
     */
    private static ToolResult formatViolationReport(final List<Violation> violations, final List<String> voices) {
        return formatViolationReport(violations, voices, "");
    }
}
