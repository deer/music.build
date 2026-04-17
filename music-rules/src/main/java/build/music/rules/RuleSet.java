package build.music.rules;

import build.music.instrument.Instrument;
import build.music.score.Part;
import build.music.score.Score;
import build.music.score.Voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A composable collection of rules to run against voices or a full score.
 */
public record RuleSet(List<Rule> rules) {

    public RuleSet {
        Objects.requireNonNull(rules, "rules must not be null");
        rules = List.copyOf(rules);
    }

    /** Check a single voice against all rules. */
    public List<Violation> check(final Voice voice, final String voiceName) {
        final List<Violation> violations = new ArrayList<>();
        for (final Rule rule : rules) {
            violations.addAll(rule.check(voice, voiceName));
        }
        return List.copyOf(violations);
    }

    /** Check a full score: single-voice rules + pairwise checks between all voice pairs. */
    public List<Violation> checkScore(final Score score) {
        final List<Violation> violations = new ArrayList<>();
        final List<Part> parts = score.scoreParts();

        // Single-voice checks
        for (final var part : parts) {
            violations.addAll(check(part.voice(), part.name()));
        }

        // Pairwise checks (all unique pairs)
        for (int i = 0; i < parts.size(); i++) {
            for (int j = i + 1; j < parts.size(); j++) {
                final var a = parts.get(i);
                final var b = parts.get(j);
                for (final Rule rule : rules) {
                    violations.addAll(rule.checkPair(a.voice(), a.name(), b.voice(), b.name()));
                }
            }
        }

        return List.copyOf(violations);
    }

    /** Combine two rule sets into one. */
    public RuleSet and(final RuleSet other) {
        final List<Rule> combined = new ArrayList<>(rules);
        combined.addAll(other.rules());
        return new RuleSet(combined);
    }

    /** Rule set for orchestration: range checks with assigned instruments. */
    public static RuleSet orchestration(final Map<String, Instrument> assignments) {
        return new RuleSet(assignments.entrySet().stream()
            .map(e -> (Rule) new RangeRule(e.getValue()))
            .toList());
    }

    /** Rule set for basic counterpoint: parallel motion + voice leading. */
    public static RuleSet counterpoint() {
        return new RuleSet(List.of(new ParallelMotionRule(), new VoiceLeadingRule()));
    }

    /** Basic rule set: meter + voice leading. */
    public static RuleSet basic(final build.music.time.TimeSignature ts) {
        return new RuleSet(List.of(new MeterRule(ts), new VoiceLeadingRule()));
    }

    /** Format violations as a readable report. */
    public static String formatReport(final List<Violation> violations, final List<String> voiceNames) {
        if (violations.isEmpty()) {
            final String voices = String.join(", ", voiceNames);
            return "✓ No violations found" + (voices.isEmpty() ? "" : " in " + voices);
        }
        final StringBuilder sb = new StringBuilder();
        for (final Violation v : violations) {
            sb.append(v).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
