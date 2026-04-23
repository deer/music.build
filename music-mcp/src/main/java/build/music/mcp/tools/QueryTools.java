package build.music.mcp.tools;

import build.music.core.NoteEvent;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.time.Fraction;

import java.util.List;

/**
 * Tools for inspecting voices and motifs in the composition context.
 */
public final class QueryTools {

    private QueryTools() {
    }

    /**
     * Tool: query.voice — display the notes of a specific voice.
     * Groups notes into measures of 4 beats (quarter-note measure) separated by "|".
     */
    public static ToolResult queryVoice(final CompositionContext ctx, final String voiceName) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Fraction total = events.stream()
                .map(e -> e.duration().fraction())
                .reduce(Fraction.ZERO, Fraction::add);

            final var sb = new StringBuilder();
            sb.append("Voice '").append(voiceName).append("': ")
                .append(events.size()).append(" events, total duration ").append(total).append("\n");
            sb.append(formatEventsMeasured(events, ctx.getTimeSignature().measureDuration()));
            return ToolResult.success(sb.toString().stripTrailing());
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: query.motif — display the notes of a named motif.
     */
    public static ToolResult queryMotif(final CompositionContext ctx, final String motifName) {
        try {
            final List<NoteEvent> events = ctx.getMotif(motifName);
            final Fraction total = events.stream()
                .map(e -> e.duration().fraction())
                .reduce(Fraction.ZERO, Fraction::add);

            final var sb = new StringBuilder();
            sb.append("Motif '").append(motifName).append("': ")
                .append(events.size()).append(" events, total duration ").append(total).append("\n");
            sb.append(CreateNoteTools.formatSequence(events));
            return ToolResult.success(sb.toString().stripTrailing());
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    // --- Internal helpers ---

    private static String formatEventsMeasured(final List<NoteEvent> events, final Fraction measureDuration) {
        final var sb = new StringBuilder("  ");
        Fraction beatAccum = Fraction.ZERO;
        boolean first = true;

        for (final NoteEvent event : events) {
            if (!first) {
                sb.append(" ");
            }
            first = false;

            if (!beatAccum.equals(Fraction.ZERO) && beatAccum.compareTo(Fraction.ZERO) >= 0) {
                // check if we've passed a measure boundary
            }

            sb.append(CreateNoteTools.formatEvent(event));
            beatAccum = beatAccum.add(event.duration().fraction());

            // Insert bar line when we reach a measure boundary
            if (beatAccum.compareTo(measureDuration) >= 0) {
                final Fraction remainder = beatAccum.subtract(measureDuration);
                if (remainder.equals(Fraction.ZERO)) {
                    sb.append(" |");
                    beatAccum = Fraction.ZERO;
                }
            }
        }
        return sb.toString().stripTrailing();
    }
}
