package build.music.rules;

import java.util.Objects;

/**
 * A rule violation: a message about something wrong or questionable in the music.
 * Rules never prevent composition — violations are advisory only.
 */
public record Violation(
    String ruleName,
    String severity,    // "error", "warning", "suggestion"
    String message,
    int noteIndex,      // which note triggered it (-1 if not note-specific)
    String voiceName
) {
    public Violation {
        Objects.requireNonNull(ruleName, "ruleName must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(voiceName, "voiceName must not be null");
    }

    public static Violation error(final String rule, final String message, final int index, final String voice) {
        return new Violation(rule, "error", message, index, voice);
    }

    public static Violation warning(final String rule, final String message, final int index, final String voice) {
        return new Violation(rule, "warning", message, index, voice);
    }

    public static Violation suggestion(final String rule, final String message, final int index, final String voice) {
        return new Violation(rule, "suggestion", message, index, voice);
    }

    /** Human-readable one-line summary. */
    @Override
    public String toString() {
        final String icon = switch (severity) {
            case "error"      -> "✗";
            case "warning"    -> "⚠";
            case "suggestion" -> "→";
            default           -> "?";
        };
        final String location = noteIndex >= 0 ? " (note " + noteIndex + ")" : "";
        return icon + " " + ruleName + ": " + message + location + " in '" + voiceName + "'";
    }
}
