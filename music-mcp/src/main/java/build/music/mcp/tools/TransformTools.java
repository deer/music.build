package build.music.mcp.tools;

import build.music.core.Chord;
import build.music.core.Note;
import build.music.core.NoteEvent;
import build.music.core.Rest;
import build.music.mcp.CompositionContext;
import build.music.mcp.ToolResult;
import build.music.pitch.SpelledInterval;
import build.music.pitch.SpelledPitch;
import build.music.score.Voice;
import build.music.time.Fraction;
import build.music.transform.Augment;
import build.music.transform.Invert;
import build.music.transform.PitchTransform;
import build.music.transform.Retrograde;
import build.music.transform.Transpose;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Tools for transforming voices: transpose, invert, retrograde, augment, and saving motifs.
 */
public final class TransformTools {

    private TransformTools() {
    }

    /**
     * Tool: transform.transpose — transpose a voice by an interval.
     *
     * @param interval    interval string, e.g. "P5", "m3", "M2"
     * @param direction   "up" or "down"
     * @param targetVoice name for the result voice, or null to use default naming
     */
    public static ToolResult transpose(final CompositionContext ctx,
                                       final String voiceName,
                                       final String interval,
                                       final String direction,
                                       final String targetVoice) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Voice voice = Voice.of(voiceName, events);

            final SpelledInterval parsed = SpelledInterval.parse(interval);
            final Voice transposed;
            if ("down".equalsIgnoreCase(direction)) {
                transposed = voice.transform(pitchTransformToMelodic(Transpose.down(parsed)));
            } else {
                transposed = voice.transpose(parsed);
            }
            final String resultName = targetVoice != null && !targetVoice.isBlank()
                ? targetVoice
                : voiceName + "_transposed";

            ctx.createVoice(resultName, transposed.events());
            return ToolResult.success(
                "Transposed '" + voiceName + "' " + direction + " by " + interval +
                    " → '" + resultName + "' (" + transposed.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: transform.invert — invert a voice around an axis pitch.
     *
     * @param axis        axis pitch string, e.g. "C4"
     * @param targetVoice name for the result voice, or null to use default naming
     */
    public static ToolResult invert(final CompositionContext ctx,
                                    final String voiceName,
                                    final String axis,
                                    final String targetVoice) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Voice voice = Voice.of(voiceName, events);

            final SpelledPitch axisPitch = SpelledPitch.parse(axis);
            final Invert invert = new Invert(axisPitch);

            final Voice inverted = voice.transform(pitchTransformToMelodic(invert));
            final String resultName = targetVoice != null && !targetVoice.isBlank()
                ? targetVoice
                : voiceName + "_inverted";

            ctx.createVoice(resultName, inverted.events());
            return ToolResult.success(
                "Inverted '" + voiceName + "' around " + axis +
                    " → '" + resultName + "' (" + inverted.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: transform.retrograde — reverse the temporal order of a voice.
     *
     * @param targetVoice name for the result voice, or null to use default naming
     */
    public static ToolResult retrograde(final CompositionContext ctx,
                                        final String voiceName,
                                        final String targetVoice) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Voice voice = Voice.of(voiceName, events);

            final Voice reversed = voice.transform(new Retrograde()::apply);
            final String resultName = targetVoice != null && !targetVoice.isBlank()
                ? targetVoice
                : voiceName + "_retrograde";

            ctx.createVoice(resultName, reversed.events());
            return ToolResult.success(
                "Retrograded '" + voiceName + "' → '" + resultName +
                    "' (" + reversed.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: transform.augment — scale all durations in a voice by a factor.
     *
     * @param factor      fraction string, e.g. "2/1" (double), "1/2" (halve), "3/2" (dotted)
     * @param targetVoice name for the result voice, or null to use default naming
     */
    public static ToolResult augment(final CompositionContext ctx,
                                     final String voiceName,
                                     final String factor,
                                     final String targetVoice) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final Voice voice = Voice.of(voiceName, events);

            final Fraction f = parseFraction(factor);
            final Voice scaled = voice.transform(new Augment(f)::apply);
            final String resultName = targetVoice != null && !targetVoice.isBlank()
                ? targetVoice
                : voiceName + "_augmented";

            ctx.createVoice(resultName, scaled.events());
            return ToolResult.success(
                "Augmented '" + voiceName + "' by " + factor +
                    " → '" + resultName + "' (" + scaled.events().size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Tool: motif.save — save a slice of a voice as a named motif.
     *
     * @param startNote 0-based start index (inclusive), or null for beginning
     * @param endNote   0-based end index (exclusive), or null for end
     */
    public static ToolResult saveMotif(final CompositionContext ctx,
                                       final String voiceName,
                                       final String motifName,
                                       final Integer startNote,
                                       final Integer endNote) {
        try {
            final List<NoteEvent> events = ctx.getVoice(voiceName);
            final int start = startNote != null ? startNote : 0;
            final int end = endNote != null ? endNote : events.size();

            if (start < 0 || end > events.size() || start >= end) {
                return ToolResult.error(
                    "Invalid range [" + start + ", " + end + ") for voice '" + voiceName +
                        "' with " + events.size() + " events.");
            }

            final List<NoteEvent> slice = events.subList(start, end);
            ctx.saveMotif(motifName, slice);
            return ToolResult.success(
                "Saved events [" + start + ", " + end + ") of '" + voiceName +
                    "' as motif '" + motifName + "' (" + slice.size() + " events).");
        } catch (final IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }
    }

    // --- Package-private helpers used by VoiceTools ---

    /**
     * Wrap a PitchTransform (operates on Pitch) into a UnaryOperator over a list of NoteEvents,
     * mapping the transform over each Note while passing Rests through unchanged.
     */
    static UnaryOperator<List<NoteEvent>> pitchTransformToMelodic(final PitchTransform pt) {
        return events -> events.stream()
            .map(event -> switch (event) {
                case Note n ->
                    (NoteEvent) Note.of(pt.apply(n.pitch()), n.duration(), n.velocity(), n.articulation(), n.tied());
                case Rest r -> (NoteEvent) r;
                case Chord c -> (NoteEvent) Chord.of(
                    c.pitches().stream().map(pt::apply).toList(),
                    c.duration(), c.velocity());
            })
            .toList();
    }

    /**
     * Parse a fraction string like "2/1", "1/2", "3/2", or bare integer like "2".
     */
    static Fraction parseFraction(final String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Factor must not be empty.");
        }
        final String trimmed = s.trim();
        final int slash = trimmed.indexOf('/');
        if (slash < 0) {
            try {
                final int n = Integer.parseInt(trimmed);
                return Fraction.of(n, 1);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid factor '" + s + "'. Expected fraction like '2/1', '1/2', '3/2', or integer.");
            }
        }
        try {
            final int n = Integer.parseInt(trimmed.substring(0, slash).trim());
            final int d = Integer.parseInt(trimmed.substring(slash + 1).trim());
            return Fraction.of(n, d);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid factor '" + s + "'. Expected fraction like '2/1', '1/2', '3/2'.");
        }
    }
}
